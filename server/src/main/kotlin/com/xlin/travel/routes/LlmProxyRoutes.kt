package com.xlin.travel.routes

import com.xlin.travel.db.DatabaseFactory
import com.xlin.travel.db.LlmModels
import com.xlin.travel.models.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * 大模型反向代理（Web 版专用）：
 * 浏览器直连 DashScope 等 OpenAI 兼容端点会被 CORS 拦截，网页端把请求发到本服务的
 * /api/llm/chat/completions，由这里查库拿到真实 baseUrl 与密钥后转发，并把 SSE 流原样透传回去。
 *
 * 防滥用：客户端必须携带与本模型下发过的密钥一致的 Authorization 头（Bearer apiKey），
 * 仅放行 chat/completions 路径，避免变成任意开放中继。
 */

/** 代理用的上游客户端：不设超时（SSE 长连接），不解析响应体（原样透传字节流） */
private val proxyClient = HttpClient(CIO) {
    engine {
        requestTimeout = 0
    }
}

private val proxyLog = LoggerFactory.getLogger("LlmProxy")

/**
 * 错误响应手工序列化为 JSON 文本：Koog 的流式请求携带 Accept: text/event-stream，
 * ContentNegotiation 匹配不到渲染器会把 respond(status, ApiError) 的 body 静默丢掉，
 * 客户端只能看到空 body 的裸状态码，无法诊断；改用 respondText 强制写 JSON 文本。
 */
private suspend fun ApplicationCall.respondProxyError(status: HttpStatusCode, message: String) {
    respondText(Json.encodeToString(ApiError.serializer(), ApiError(message)), ContentType.Application.Json, status)
}

fun Route.llmProxyRoutes() {
    route("/api/llm") {
        post("/chat/completions") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)

            val body = call.receiveText()
            val modelId = runCatching {
                Json.parseToJsonElement(body).jsonObject["model"]?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            if (modelId.isNullOrBlank()) {
                return@post call.respondProxyError(HttpStatusCode.BadRequest, "代理请求缺少 model 字段")
            }

            val row = try {
                transaction { LlmModels.selectAll().where { LlmModels.modelId eq modelId }.firstOrNull() }
            } catch (e: Exception) {
                proxyLog.warn("代理查库异常：${e.message}")
                null
            }
            if (row == null) {
                proxyLog.warn("代理 404：llm_models 中不存在模型 '$modelId'")
                return@post call.respondProxyError(HttpStatusCode.NotFound, "代理的模型不存在：$modelId")
            }
            val apiKey = row[LlmModels.apiKey]
            val baseUrl = row[LlmModels.url]

            // 鉴权：客户端回显的密钥必须与该模型库中存储的一致
            val auth = call.request.headers["Authorization"]
            if (auth.isNullOrBlank() || auth.removePrefix("Bearer ").trim() != apiKey) {
                proxyLog.warn("代理 401：Authorization 与库内密钥不匹配（model=$modelId）")
                return@post call.respondProxyError(HttpStatusCode.Unauthorized, "代理鉴权失败，请重新登录后再试")
            }

            val target = "${baseUrl.trimEnd('/')}/chat/completions"
            val upstream = try {
                proxyClient.post(target) {
                    header("Authorization", "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            } catch (e: Exception) {
                proxyLog.warn("代理 502：上游不可达 $target，${e.message}")
                return@post call.respondProxyError(HttpStatusCode.BadGateway, "上游模型服务不可达：${e.message}")
            }

            // 非 2xx 的上游错误用 JSON 文本回传（带可读 body），成功流（含 SSE）则原样透传
            if (!upstream.status.isSuccess()) {
                val upstreamBody = runCatching { upstream.bodyAsText() }.getOrNull().orEmpty()
                proxyLog.warn("代理透传上游错误：status=${upstream.status.value} model=$modelId target=$target body=${upstreamBody.take(300)}")
                return@post call.respondText(upstreamBody, ContentType.Application.Json, upstream.status)
            }

            // 状态码与内容类型（text/event-stream / application/json）原样透传
            val upstreamType = upstream.headers[io.ktor.http.HttpHeaders.ContentType]
                ?.let { runCatching { ContentType.parse(it) }.getOrNull() }
            call.respondStreaming(upstream.status, upstreamType, upstream.bodyAsChannel())
        }
    }
}

/** 以 ReadChannelContent 把上游字节流写成响应，支持 SSE 分块传输 */
private suspend fun ApplicationCall.respondStreaming(
    upstreamStatus: HttpStatusCode,
    upstreamContentType: ContentType?,
    channel: ByteReadChannel,
) {
    respond(object : OutgoingContent.ReadChannelContent() {
        override val status: HttpStatusCode = upstreamStatus
        override val contentType: ContentType? = upstreamContentType
        override fun readFrom(): ByteReadChannel = channel
    })
}
