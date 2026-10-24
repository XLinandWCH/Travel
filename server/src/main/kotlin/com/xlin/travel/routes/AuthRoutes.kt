package com.xlin.travel.routes

import com.xlin.travel.db.DatabaseFactory
import com.xlin.travel.models.ApiError
import com.xlin.travel.models.AuthResponse
import com.xlin.travel.models.LoginRequest
import com.xlin.travel.models.RegisterRequest
import com.xlin.travel.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * 账号服务路由：注册 / 登录。
 * 数据库未初始化（本机没起 MySQL / 隧道没通）时统一返回 503，服务本身仍可运行，
 * 模型密钥、反馈工单等其它接口暂不开放。
 */
fun Route.authRoutes() {
    route("/api/auth") {

        post("/register") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<RegisterRequest>() ?: return@post badRequest(call)
            when (val result = UserService.register(request)) {
                is UserService.AuthResult.Success -> call.respond(
                    HttpStatusCode.Created,
                    AuthResponse(token = UserService.issueToken(result.user.id), user = result.user),
                )
                is UserService.AuthResult.Failed ->
                    call.respond(HttpStatusCode.fromValue(result.status), ApiError(result.message))
            }
        }

        post("/login") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<LoginRequest>() ?: return@post badRequest(call)
            when (val result = UserService.login(request)) {
                is UserService.AuthResult.Success -> call.respond(
                    AuthResponse(token = UserService.issueToken(result.user.id), user = result.user),
                )
                is UserService.AuthResult.Failed ->
                    call.respond(HttpStatusCode.fromValue(result.status), ApiError(result.message))
            }
        }
    }
}

private suspend inline fun <reified T> ApplicationCall.receiveOrNull(): T? =
    runCatching { receive<T>() }.getOrNull()

private suspend fun badRequest(call: ApplicationCall) =
    call.respond(HttpStatusCode.BadRequest, ApiError("请求体解析失败，请提交 JSON"))

private suspend fun databaseUnavailable(call: ApplicationCall) =
    call.respond(HttpStatusCode.ServiceUnavailable, ApiError("数据库服务未初始化"))
