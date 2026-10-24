package com.xlin.travel.routes

import com.xlin.travel.db.DatabaseFactory
import com.xlin.travel.models.ApiError
import com.xlin.travel.models.AuthResponse
import com.xlin.travel.models.DeleteAccountRequest
import com.xlin.travel.models.LoginRequest
import com.xlin.travel.models.OkResponse
import com.xlin.travel.models.RegisterRequest
import com.xlin.travel.models.UpdatePasswordRequest
import com.xlin.travel.models.UpdateUsernameRequest
import com.xlin.travel.models.UploadAvatarRequest
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

        // 修改用户名：需登录 token，成功回传最新资料
        post("/update-username") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<UpdateUsernameRequest>() ?: return@post badRequest(call)
            when (val result = UserService.updateUsername(request.token, request.username)) {
                is UserService.MutationResult.Success -> call.respond(
                    OkResponse(message = "用户名已更新", user = result.user),
                )
                is UserService.MutationResult.Failed ->
                    call.respond(HttpStatusCode.fromValue(result.status), ApiError(result.message))
            }
        }

        // 修改密码：需登录 token + 原密码，服务端强制 14 天冷却
        post("/update-password") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<UpdatePasswordRequest>() ?: return@post badRequest(call)
            when (val result = UserService.updatePassword(request.token, request.oldPassword, request.newPassword)) {
                is UserService.MutationResult.Success ->
                    call.respond(OkResponse(message = "密码已修改"))
                is UserService.MutationResult.Failed ->
                    call.respond(HttpStatusCode.fromValue(result.status), ApiError(result.message))
            }
        }

        // 注销账号：需登录 token，删除服务端该用户全部信息
        post("/delete-account") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<DeleteAccountRequest>() ?: return@post badRequest(call)
            when (val result = UserService.deleteAccount(request.token)) {
                is UserService.MutationResult.Success ->
                    call.respond(OkResponse(message = "账号已注销"))
                is UserService.MutationResult.Failed ->
                    call.respond(HttpStatusCode.fromValue(result.status), ApiError(result.message))
            }
        }

        // 上传头像：需登录 token + base64，服务端保存一份用于跨设备同步
        post("/upload-avatar") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<UploadAvatarRequest>() ?: return@post badRequest(call)
            when (val result = UserService.updateAvatar(request.token, request.avatar)) {
                is UserService.MutationResult.Success ->
                    call.respond(OkResponse(message = "头像已保存", user = result.user))
                is UserService.MutationResult.Failed ->
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
