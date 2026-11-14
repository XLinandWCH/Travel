package com.xlin.travel.setting

import com.xlin.travel.login.SERVER_BASE_URL
import com.xlin.travel.login.createHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * 客服聊天 API：对接服务端 /api/customer-service。
 * 会话以客户用户名为归属键——普通用户收发自己的消息，管理员按 username 选择客户并回复。
 */

/** 全局通知会话键（与服务端 CustomerServiceService.GLOBAL_USERNAME 一致）：管理员向它发送的内容所有用户可见 */
const val CUSTOMER_GLOBAL_USERNAME = "__global__"

// ---------- 请求体 ----------

@Serializable
private data class CustomerTokenRequest(val token: String)

@Serializable
private data class CustomerMessagesRequest(val token: String, val username: String? = null)

@Serializable
private data class CustomerSendRequest(val token: String, val content: String, val imageData: String? = null)

@Serializable
private data class CustomerAdminReplyRequest(
    val token: String,
    val username: String,
    val content: String,
    val imageData: String? = null,
)

// ---------- 响应体 ----------

/** 单条客服消息 */
@Serializable
data class CustomerMessageDto(
    val senderRole: String,            // 'admin' / 'user'
    val content: String,
    val imageData: String? = null,     // 图片 base64，可选
    val createdAt: String,
)

/** 消息列表响应 */
@Serializable
data class CustomerMessagesResponse(val messages: List<CustomerMessageDto> = emptyList())

/** 单个会话（管理员视图） */
@Serializable
data class CustomerConversationDto(
    val username: String,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
)

/** 会话列表响应（管理员视图） */
@Serializable
data class CustomerConversationsResponse(val conversations: List<CustomerConversationDto> = emptyList())

@Serializable
private data class CustomerOkResponse(val message: String)

@Serializable
private data class CustomerApiError(val message: String)

/** 读操作结果 */
sealed interface CustomerServiceResult {
    data class Messages(val data: CustomerMessagesResponse) : CustomerServiceResult
    data class Conversations(val data: CustomerConversationsResponse) : CustomerServiceResult
    data class Failed(val message: String) : CustomerServiceResult
}

/** 写操作结果 */
sealed interface CustomerServiceMutationResult {
    data class Done(val message: String) : CustomerServiceMutationResult
    data class Failed(val message: String) : CustomerServiceMutationResult
}

object CustomerServiceApi {

    private val client by lazy { createHttpClient() }

    /** 拉取消息：普通用户传 username=null（恒为自己）;管理员传目标客户 username */
    suspend fun getMessages(token: String, username: String? = null): CustomerServiceResult = try {
        val response = client.post("$SERVER_BASE_URL/api/customer-service/messages") {
            contentType(ContentType.Application.Json)
            setBody(CustomerMessagesRequest(token, username))
        }
        if (response.status == HttpStatusCode.OK) {
            CustomerServiceResult.Messages(response.body())
        } else {
            CustomerServiceResult.Failed(errorText(response))
        }
    } catch (e: Exception) {
        CustomerServiceResult.Failed("网络连接失败，请检查服务器是否可用")
    }

    /** 用户发送消息（含可选图片） */
    suspend fun send(token: String, content: String, imageData: String? = null): CustomerServiceMutationResult = try {
        val response = client.post("$SERVER_BASE_URL/api/customer-service/send") {
            contentType(ContentType.Application.Json)
            setBody(CustomerSendRequest(token, content, imageData))
        }
        if (response.status == HttpStatusCode.OK) {
            CustomerServiceMutationResult.Done(response.body<CustomerOkResponse>().message)
        } else {
            CustomerServiceMutationResult.Failed(errorText(response))
        }
    } catch (e: Exception) {
        CustomerServiceMutationResult.Failed("网络连接失败，请检查服务器是否可用")
    }

    /** 管理员回复指定客户（含可选图片） */
    suspend fun adminReply(
        token: String,
        username: String,
        content: String,
        imageData: String? = null,
    ): CustomerServiceMutationResult = try {
        val response = client.post("$SERVER_BASE_URL/api/customer-service/admin/reply") {
            contentType(ContentType.Application.Json)
            setBody(CustomerAdminReplyRequest(token, username, content, imageData))
        }
        if (response.status == HttpStatusCode.OK) {
            CustomerServiceMutationResult.Done(response.body<CustomerOkResponse>().message)
        } else {
            CustomerServiceMutationResult.Failed(errorText(response))
        }
    } catch (e: Exception) {
        CustomerServiceMutationResult.Failed("网络连接失败，请检查服务器是否可用")
    }

    /** 管理员会话列表 */
    suspend fun getConversations(token: String): CustomerServiceResult = try {
        val response = client.post("$SERVER_BASE_URL/api/customer-service/admin/conversations") {
            contentType(ContentType.Application.Json)
            setBody(CustomerTokenRequest(token))
        }
        if (response.status == HttpStatusCode.OK) {
            CustomerServiceResult.Conversations(response.body())
        } else {
            CustomerServiceResult.Failed(errorText(response))
        }
    } catch (e: Exception) {
        CustomerServiceResult.Failed("网络连接失败，请检查服务器是否可用")
    }

    private suspend fun errorText(response: HttpResponse): String =
        runCatching { response.body<CustomerApiError>().message }
            .getOrElse { "服务异常（${response.status.value}）" }
}