package com.xlin.travel.routes

import com.xlin.travel.db.DatabaseFactory
import com.xlin.travel.models.ApiError
import com.xlin.travel.models.CustomerAdminReplyRequest
import com.xlin.travel.models.CustomerMessagesRequest
import com.xlin.travel.models.CustomerOkResponse
import com.xlin.travel.models.CustomerSendRequest
import com.xlin.travel.models.CustomerTokenRequest
import com.xlin.travel.services.CustomerServiceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * 客服聊天路由：拉取消息、用户发送、管理员回复、管理员会话列表。
 * 均需登录 token（服务端再校验管理员角色）;数据库未初始化返回 503。
 */
fun Route.customerServiceRoutes() {
    route("/api/customer-service") {

        post("/messages") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<CustomerMessagesRequest>() ?: return@post badRequest(call)
            respondCustomerService(call, CustomerServiceService.messages(request.token, request.username))
        }

        post("/send") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<CustomerSendRequest>() ?: return@post badRequest(call)
            respondCustomerService(call, CustomerServiceService.send(request.token, request.content, request.imageData))
        }

        post("/admin/reply") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<CustomerAdminReplyRequest>() ?: return@post badRequest(call)
            respondCustomerService(call, CustomerServiceService.adminReply(request.token, request.username, request.content, request.imageData))
        }

        post("/admin/conversations") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<CustomerTokenRequest>() ?: return@post badRequest(call)
            respondCustomerService(call, CustomerServiceService.conversations(request.token))
        }
    }
}

/** 统一把业务结果映射为 HTTP 响应 */
private suspend fun respondCustomerService(call: ApplicationCall, result: CustomerServiceService.CustomerServiceResult) {
    when (result) {
        is CustomerServiceService.CustomerServiceResult.Messages -> call.respond(result.data)
        is CustomerServiceService.CustomerServiceResult.Conversations -> call.respond(result.data)
        is CustomerServiceService.CustomerServiceResult.Done -> call.respond(CustomerOkResponse(result.message))
        is CustomerServiceService.CustomerServiceResult.Failed ->
            call.respond(HttpStatusCode.fromValue(result.status), ApiError(result.message))
    }
}