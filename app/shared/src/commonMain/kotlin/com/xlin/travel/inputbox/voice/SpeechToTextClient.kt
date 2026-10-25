package com.xlin.travel.inputbox.voice

import com.xlin.travel.agent.RemoteModelConfig
import com.xlin.travel.login.createHttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 语音识别客户端：把录音（WAV 字节）以 Base64 Data URI 提交给 Fun-ASR-Flash 多模态生成接口，返回识别文本。
 *
 * 接口约定（见 UI设计/Fun-ASR-Flash 模型调用说明.md 与百炼 HTTP API）：
 * - POST 到服务端下发的 url（完整端点），携带 Authorization: Bearer <apiKey>；
 * - 音频短（< 1 分钟）时以非流式返回，读取 output.text 即可，故固定 X-DashScope-SSE: disable；
 * - parameters.format = wav、sample_rate = 16000，与 [AudioRecorder] 产出的 16kHz 单声道 PCM/WAV 对齐。
 */
object SpeechToTextClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val client by lazy { createHttpClient() }

    /**
     * @param wavBytes 16kHz / 单声道 / 16bit PCM 封装的 WAV 字节
     * @return 识别出的文本；空录音或服务端无文本时返回空串
     * @throws Exception 网络失败、鉴权失败或响应异常时抛出，消息可直接展示给用户
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun transcribe(config: RemoteModelConfig, wavBytes: ByteArray): String {
        require(wavBytes.isNotEmpty()) { "录音为空，请重新说话" }
        val dataUri = "data:audio/wav;base64," + Base64.encode(wavBytes)
        val body = buildAsrBody(config.modelId, dataUri)

        val response: HttpResponse = client.post(config.baseUrl) {
            contentType(ContentType.Application.Json)
            header("X-DashScope-SSE", "disable")
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            setBody(body)
        }
        val root = response.bodyAsJson()
        if (!response.status.isSuccess()) {
            val message = root["message"]?.jsonPrimitive?.contentOrNull
            error(message ?: "识别失败（${response.status.value}）")
        }
        return root["output"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty().trim()
    }

    /** 响应体读为文本再解析为 JsonObject，避免依赖协商好的序列化器 */
    private suspend fun HttpResponse.bodyAsJson(): JsonObject =
        json.parseToJsonElement(bodyAsText()).jsonObject

    /** 构造 Fun-ASR 请求体：input.messages[].content[] = {type:input_audio, input_audio:{data}} */
    private fun buildAsrBody(modelId: String, dataUri: String): String =
        json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("model", modelId)
                putJsonObject("input") {
                    putJsonArray("messages") {
                        add(
                            buildJsonObject {
                                put("role", "user")
                                putJsonArray("content") {
                                    add(
                                        buildJsonObject {
                                            put("type", "input_audio")
                                            putJsonObject("input_audio") { put("data", dataUri) }
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
                putJsonObject("parameters") {
                    put("format", "wav")
                    put("sample_rate", "16000")
                }
            },
        )
}
