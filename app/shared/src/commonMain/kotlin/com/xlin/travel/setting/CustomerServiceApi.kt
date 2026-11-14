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
 * 客服服务API：对接服务端 /api/customer 接口。
 * 客服（管理员）可以回复用户消息，用户可以发送消息给客服。
 */

// ---------- 请求体 ----------

@Serializable
private data class CustomerTokenRequest(val token: String)

@Serializable
private data class CustomerSendRequest(val token: String, val sessionId: String, val content: String)

@Serializable
private data class CustomerAdminReplyRequest(val token: String, val sessionId: String, val content: String)

@Serializable
private data class CustomerSessionRequest(val token: String, val sessionId: String)

// ---------- 响应体 ----------

/** 客服消息Dto */
@Serializable
data class CustomerServiceMessageDto(
    val sender: String,
    val senderRole: String,       // 'admin' / 'user'
    val content: String,
    val createdAt: String,
)

/** 客服会话Dto */
@Serializable
data class CustomerServiceSessionDto(
    val sessionId: String,
    val username: String,
    val lastMessage: String?,
    val lastMessageAt: String?,
    val createdAt: String,
)

/** 客服会话列表响应 */
@Serializable
data class CustomerServiceSessionListResponse(
    val sessions: List<CustomerServiceSessionDto> = emptyList(),
)

/** 客服消息列表响应 */
@Serializable
data class CustomerServiceSessionMessagesResponse(
    val sessionId: String,
    val messages: List<CustomerServiceMessageDto> = emptyList(),
)

/** 创建会话响应 */
@Serializable
private data class CustomerServiceCreateSessionResponse(
    val message: String,
    val sessionId: String,
)

/** 客服服务结果 */
sealed interface CustomerServiceResult {
    data class Success(val data: CustomerServiceSessionMessagesResponse) : CustomerServiceResult
    data class SessionList(val data: CustomerServiceSessionListResponse) : CustomerServiceResult
    data class Created(val sessionId: String) : CustomerServiceResult
    data class Failed(val message: String) : CustomerServiceResult
}

/** 客服服务写操作结果 */
sealed interface CustomerServiceMutationResult {
    data class Done(val message: String) : CustomerServiceMutationResult
    data class Failed(val message: String) : CustomerServiceMutationResult
}

@Serializable
private data class CustomerOkResponse(val message: String)

@Serializable
private data class CustomerApiError(val message: String)

object CustomerServiceApi {

    private val client by lazy { createHttpClient() }

    /** 创建会话 */
    suspend fun createSession(token: String): CustomerServiceResult {
        return try {
            val response = client.post("$SERVER_BASE_URL/api/customer-service/create") {
                contentType(ContentType.Application.Json)
                setBody(CustomerTokenRequest(token))
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.body<CustomerServiceCreateSessionResponse>()
                CustomerServiceResult.Created(body.sessionId)
            } else {
                CustomerServiceResult.Failed(errorText(response))
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed("网络连接失败，请检查服务器是否可用")
        }
    }

    /** 获取会话列表 */
    suspend fun getSessionList(token: String, isAdmin: Boolean): CustomerServiceResult {
        return try {
            val endpoint = if (isAdmin) "/api/customer-service/admin/sessions" else "/api/customer-service/user/sessions"
            val response = client.post("$SERVER_BASE_URL$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(CustomerTokenRequest(token))
            }
            if (response.status == HttpStatusCode.OK) {
                CustomerServiceResult.SessionList(response.body())
            } else {
                CustomerServiceResult.Failed(errorText(response))
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed("网络连接失败，请检查服务器是否可用")
        }
    }

    /** 获取会话消息列表 */
    suspend fun getMessages(token: String, sessionId: String): CustomerServiceResult {
        return try {
            val response = client.post("$SERVER_BASE_URL/api/customer-service/messages") {
                contentType(ContentType.Application.Json)
                setBody(CustomerSessionRequest(token, sessionId))
            }
            if (response.status == HttpStatusCode.OK) {
                CustomerServiceResult.Success(response.body())
            } else {
                CustomerServiceResult.Failed(errorText(response))
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed("网络连接失败，请检查服务器是否可用")
        }
    }

    /** 用户发送消息给客服 */
    suspend fun userSend(token: String, sessionId: String, content: String): CustomerServiceMutationResult {
        return try {
            val response = client.post("$SERVER_BASE_URL/api/customer-service/send") {
                contentType(ContentType.Application.Json)
                setBody(CustomerSendRequest(token, sessionId, content))
            }
            if (response.status == HttpStatusCode.OK) {
                CustomerServiceMutationResult.Done(response.body<CustomerOkResponse>().message)
            } else {
                CustomerServiceMutationResult.Failed(errorText(response))
            }
        } catch (e: Exception) {
            CustomerServiceMutationResult.Failed("网络连接失败，请检查服务器是否可用")
        }
    }

    /** 管理员（客服）回复用户消息 */
    suspend fun adminReply(token: String, sessionId: String, content: String): CustomerServiceMutationResult {
        return try {
            val response = client.post("$SERVER_BASE_URL/api/customer-service/admin/reply") {
                contentType(ContentType.Application.Json)
                setBody(CustomerAdminReplyRequest(token, sessionId, content))
            }
            if (response.status == HttpStatusCode.OK) {
                CustomerServiceMutationResult.Done(response.body<CustomerOkResponse>().message)
            } else {
                CustomerServiceMutationResult.Failed(errorText(response))
            }
        } catch (e: Exception) {
            CustomerServiceMutationResult.Failed("网络连接失败，请检查服务器是否可用")
        }
    }

    private suspend fun errorText(response: io.ktor.client.statement.HttpResponse): String =
        runCatching { response.body<CustomerApiError>().message }
            .getOrElse { "服务异常（${response.status.value}）" }
}
