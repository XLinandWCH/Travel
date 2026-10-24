package com.xlin.travel.models

import kotlinx.serialization.Serializable

/**
 * /api/auth 接口的请求 / 响应体（kotlinx.serialization）。
 * 与客户端 LoginState 的字段一一对应。
 */

/** 注册请求：用户名 + 手机号 + 邮箱 + 明文密码（服务端 BCrypt 后落库） */
@Serializable
data class RegisterRequest(
    val username: String,
    val phone: String,
    val email: String,
    val password: String,
)

/** 登录请求：account 为邮箱或手机号，由服务端自动判别 */
@Serializable
data class LoginRequest(
    val account: String,
    val password: String,
)

/** 对外返回的用户信息（不含密码哈希） */
@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val phone: String,
    val email: String,
    val usageDays: Int,
    val registeredAt: String,
)

/** 登录 / 注册成功：返回会话 token 与用户信息 */
@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto,
)

/** 失败时统一返回的错误体 */
@Serializable
data class ApiError(
    val message: String,
)
