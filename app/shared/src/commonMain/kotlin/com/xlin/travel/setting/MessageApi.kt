package com.xlin.travel.setting

import com.xlin.travel.login.SERVER_BASE_URL
import com.xlin.travel.login.createHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * 反馈 / 消息客户端：对接 server 模块 /api/feedback 接口（报文结构与 server FeedbackModels.kt 对应）。
 * 提交反馈（服务端生成 #7 位工单号）、普通用户收件箱、管理员列表 / 回复 / 公告。
 */

// ---------- 请求体 ----------

@Serializable
private data class SubmitFeedbackRequest(val token: String, val content: String)

@Serializable
private data class MessageTokenRequest(val token: String)

@Serializable
private data class ReplyFeedbackRequest(val token: String, val ticketId: String, val reply: String)

@Serializable
private data class BroadcastRequest(val token: String, val title: String, val content: String)

@Serializable
private data class BroadcastDeleteRequest(val token: String, val id: Long)

@Serializable
private data class TicketDetailRequest(val token: String, val ticketId: String)

@Serializable
private data class UserReplyRequest(val token: String, val ticketId: String, val content: String)

@Serializable
private data class TicketStatusRequest(val token: String, val ticketId: String, val status: String)

@Serializable
private data class ResolveRequest(val token: String, val ticketId: String, val resolved: Boolean, val rating: Int = 0)

// ---------- 响应体（与服务端 models 同名但客户端独立声明） ----------

/** 单条反馈：管理员视角带 username，普通用户拉到的只会是自己的 */
@Serializable
data class FeedbackDto(
    val ticketId: String,
    val username: String,
    val content: String,
    val createdAt: String,
    val reply: String? = null,
    val repliedAt: String? = null,
    val status: String = "submitted",
    val rating: Int? = null,
)

/** 工单内一条往来消息（邮件会话中的一封） */
@Serializable
data class FeedbackMessageDto(
    val sender: String,
    val senderRole: String,       // 'admin' / 'user'
    val content: String,
    val createdAt: String,
)

/** 工单详情：原始反馈 + 回复线程 + 状态 / 评分 */
@Serializable
data class FeedbackDetailResponse(
    val ticketId: String,
    val username: String,
    val content: String,
    val createdAt: String,
    val status: String,
    val rating: Int? = null,
    val mine: Boolean,
    val messages: List<FeedbackMessageDto> = emptyList(),
)

/** 未读徽标数量 */
@Serializable
data class UnreadResponse(val count: Int)

/** 单条管理员公告 */
@Serializable
data class BroadcastDto(
    val id: Long,
    val title: String,
    val content: String,
    val adminUsername: String,
    val createdAt: String,
)

/** 消息列表：反馈线程 + 公告 */
@Serializable
data class MessageListResponse(
    val feedbacks: List<FeedbackDto> = emptyList(),
    val broadcasts: List<BroadcastDto> = emptyList(),
)

@Serializable
private data class SubmitFeedbackResponse(val message: String, val ticketId: String)

@Serializable
private data class MessageOkResponse(val message: String)

@Serializable
private data class MsgApiError(val message: String)

/** 列表拉取结果 */
sealed interface MessageListResult {
    data class Success(val data: MessageListResponse) : MessageListResult
    data class Failed(val message: String) : MessageListResult
}

/** 工单详情拉取结果 */
sealed interface MessageDetailResult {
    data class Success(val data: FeedbackDetailResponse) : MessageDetailResult
    data class Failed(val message: String) : MessageDetailResult
}

/** 写操作（提交 / 回复 / 公告）结果：提交成功带回工单号，其余只有提示语 */
sealed interface MessageMutationResult {
    data class Submitted(val message: String, val ticketId: String) : MessageMutationResult
    data class Done(val message: String) : MessageMutationResult
    data class Failed(val message: String) : MessageMutationResult
}

object MessageApi {

    // 与 AuthApi 各自懒加载复用连接池（同一进程内 Ktor client 可多实例）
    private val client by lazy { createHttpClient() }

    /** 提交反馈：成功带回服务端生成的工单号（# + 7 位随机数） */
    suspend fun submit(token: String, content: String): MessageMutationResult {
        return try {
            val response = client.post("$SERVER_BASE_URL/api/feedback/submit") {
                contentType(ContentType.Application.Json)
                setBody(SubmitFeedbackRequest(token, content))
            }
            if (response.status == HttpStatusCode.OK) {
                val ok = response.body<SubmitFeedbackResponse>()
                MessageMutationResult.Submitted(ok.message, ok.ticketId)
            } else {
                MessageMutationResult.Failed(errorText(response))
            }
        } catch (e: Exception) {
            MessageMutationResult.Failed("网络连接失败，请检查服务器是否可用")
        }
    }

    /** 普通用户收件箱：自己的反馈（含管理员回复）+ 全部公告 */
    suspend fun inbox(token: String): MessageListResult =
        fetchList("$SERVER_BASE_URL/api/feedback/inbox", MessageTokenRequest(token))

    /** 管理员：全部反馈 + 全部公告 */
    suspend fun adminList(token: String): MessageListResult =
        fetchList("$SERVER_BASE_URL/api/feedback/admin/list", MessageTokenRequest(token))

    /** 管理员回复指定工单 */
    suspend fun reply(token: String, ticketId: String, reply: String): MessageMutationResult =
        mutate("$SERVER_BASE_URL/api/feedback/admin/reply", ReplyFeedbackRequest(token, ticketId, reply))

    /** 普通用户在工单内追问 */
    suspend fun userReply(token: String, ticketId: String, content: String): MessageMutationResult =
        mutate("$SERVER_BASE_URL/api/feedback/reply", UserReplyRequest(token, ticketId, content))

    /** 管理员设置工单状态 */
    suspend fun adminStatus(token: String, ticketId: String, status: String): MessageMutationResult =
        mutate("$SERVER_BASE_URL/api/feedback/admin/status", TicketStatusRequest(token, ticketId, status))

    /** 用户确认解决并评分 */
    suspend fun resolve(token: String, ticketId: String, resolved: Boolean, rating: Int): MessageMutationResult =
        mutate("$SERVER_BASE_URL/api/feedback/resolve", ResolveRequest(token, ticketId, resolved, rating))

    /** 拉取指定工单详情（含回复线程） */
    suspend fun detail(token: String, ticketId: String): MessageDetailResult {
        return try {
            val response = client.post("$SERVER_BASE_URL/api/feedback/detail") {
                contentType(ContentType.Application.Json)
                setBody(TicketDetailRequest(token, ticketId))
            }
            if (response.status == HttpStatusCode.OK) {
                MessageDetailResult.Success(response.body())
            } else {
                MessageDetailResult.Failed(errorText(response))
            }
        } catch (e: Exception) {
            MessageDetailResult.Failed("网络连接失败，请检查服务器是否可用")
        }
    }

    /** 未读徽标数量（失败时返回 0，不阻断主流程） */
    suspend fun unread(token: String): Int {
        return try {
            val response = client.post("$SERVER_BASE_URL/api/feedback/unread") {
                contentType(ContentType.Application.Json)
                setBody(MessageTokenRequest(token))
            }
            if (response.status == HttpStatusCode.OK) response.body<UnreadResponse>().count else 0
        } catch (e: Exception) {
            0
        }
    }

    /** 标记消息中心已读（失败静默） */
    suspend fun markRead(token: String) {
        try {
            client.post("$SERVER_BASE_URL/api/feedback/read") {
                contentType(ContentType.Application.Json)
                setBody(MessageTokenRequest(token))
            }
        } catch (_: Exception) {
        }
    }

    /** 管理员发布公告 */
    suspend fun broadcast(token: String, title: String, content: String): MessageMutationResult =
        mutate("$SERVER_BASE_URL/api/feedback/admin/broadcast", BroadcastRequest(token, title, content))

    /** 管理员删除指定公告（按 id） */
    suspend fun deleteBroadcast(token: String, id: Long): MessageMutationResult =
        mutate("$SERVER_BASE_URL/api/feedback/admin/broadcast/delete", BroadcastDeleteRequest(token, id))

    private suspend fun fetchList(url: String, body: Any): MessageListResult {
        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status == HttpStatusCode.OK) {
                MessageListResult.Success(response.body())
            } else {
                MessageListResult.Failed(errorText(response))
            }
        } catch (e: Exception) {
            MessageListResult.Failed("网络连接失败，请检查服务器是否可用")
        }
    }

    private suspend fun mutate(url: String, body: Any): MessageMutationResult {
        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status == HttpStatusCode.OK) {
                MessageMutationResult.Done(response.body<MessageOkResponse>().message)
            } else {
                MessageMutationResult.Failed(errorText(response))
            }
        } catch (e: Exception) {
            MessageMutationResult.Failed("网络连接失败，请检查服务器是否可用")
        }
    }

    /** 优先展示服务端给出的原因，解析失败则回退状态码 */
    private suspend fun errorText(response: io.ktor.client.statement.HttpResponse): String =
        runCatching { response.body<MsgApiError>().message }
            .getOrElse { "服务异常（${response.status.value}）" }
}
