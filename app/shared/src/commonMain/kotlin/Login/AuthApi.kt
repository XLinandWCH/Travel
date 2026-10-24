package com.xlin.travel.login

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * 账号服务客户端：对接 server 模块 /api/auth 接口（Ktor + kotlinx.serialization）。
 * 服务器为公网 IP + 开发端口，换环境时只需改 AUTH_BASE_URL。
 */
private const val AUTH_BASE_URL = "http://193.112.23.199:8080"

// ---------- 与 server/models/AuthModels.kt 对应的报文结构 ----------

@Serializable
private data class LoginRequest(val account: String, val password: String)

@Serializable
private data class RegisterRequest(
    val username: String,
    val phone: String,
    val email: String,
    val password: String,
)

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val phone: String,
    val email: String,
    val usageDays: Int,
    val registeredAt: String,
)

@Serializable
data class AuthResponse(val token: String, val user: UserDto)

@Serializable
data class ApiError(val message: String)

/** 接口调用结果：成功带回用户信息（token 暂存后续接入会话保持），失败带回服务端 / 网络错误提示 */
sealed interface AuthApiResult {
    data class Success(val user: UserDto, val token: String) : AuthApiResult
    data class Failed(val message: String) : AuthApiResult
}

object AuthApi {

    // 懒加载，进程内复用同一个连接池
    private val client by lazy { createHttpClient() }

    /** 登录：account 为邮箱或手机号（与后端 LoginRequest 规则一致） */
    suspend fun login(account: String, password: String): AuthApiResult =
        post("$AUTH_BASE_URL/api/auth/login", LoginRequest(account.trim(), password))

    suspend fun register(
        username: String,
        phone: String,
        email: String,
        password: String,
    ): AuthApiResult = post(
        "$AUTH_BASE_URL/api/auth/register",
        RegisterRequest(username.trim(), phone.trim(), email.trim(), password),
    )

    private suspend inline fun <reified B : Any> post(url: String, body: B): AuthApiResult {
        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                val auth = response.body<AuthResponse>()
                AuthApiResult.Success(auth.user, auth.token)
            } else {
                // 优先展示服务端给出的原因（如“该邮箱已被注册”），解析失败则回退状态码
                val message = runCatching { response.body<ApiError>().message }
                    .getOrElse { "服务异常（${response.status.value}）" }
                AuthApiResult.Failed(message)
            }
        } catch (e: Exception) {
            AuthApiResult.Failed("网络连接失败，请检查服务器是否可用")
        }
    }
}
