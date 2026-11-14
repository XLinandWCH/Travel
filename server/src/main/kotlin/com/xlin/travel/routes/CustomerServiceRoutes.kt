package com.xlin.travel.routes

import com.xlin.travel.db.DatabaseFactory
import com.xlin.travel.models.ApiError
import com.xlin.travel.models.CustomerServiceAdminReplyRequest
import com.xlin.travel.models.CustomerServiceOkResponse
import com.xlin.travel.models.CustomerServiceSendMessageRequest
import com.xlin.travel.models.CustomerServiceSessionRequest
import com.xlin.travel.models.CustomerServiceTokenRequest
import com.xlin.travel.services.CustomerServiceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * 客服服务路由：创建会话、发送消息、获取消息、管理员回复。
 * 均需登录 token（服务端再校验管理员角色）；数据库未初始化返回 503。
 */
fun Route.customerServiceRoutes() {
    route("/api/customer-service") {

        post("/create") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<CustomerServiceTokenRequest>() ?: return@post badRequest(call)
            respondCustomerService(call, CustomerServiceService.createSession(request.token))
        }

        post("/send") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<CustomerServiceSendMessageRequest>() ?: return@post badRequest(call)
            respondCustomerService(call, CustomerServiceService.sendMessage(request.token, request.sessionId, request.content, request.imageData))
        }

        post("/admin/reply") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<CustomerServiceAdminReplyRequest>() ?: return@post badRequest(call)
            respondCustomerService(call, CustomerServiceService.adminReply(request.token, request.sessionId, request.content, request.imageData))
        }

        post("/messages") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<CustomerServiceSessionRequest>() ?: return@post badRequest(call)
            respondCustomerService(call, CustomerServiceService.getSessionMessages(request.token, request.sessionId))
        }

        post("/user/sessions") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<CustomerServiceTokenRequest>() ?: return@post badRequest(call)
            respondCustomerService(call, CustomerServiceService.userSessionList(request.token))
        }

        post("/admin/sessions") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<CustomerServiceTokenRequest>() ?: return@post badRequest(call)
            respondCustomerService(call, CustomerServiceService.adminSessionList(request.token))
        }
    }
}

/** 统一把业务结果映射为 HTTP 响应 */
private suspend fun respondCustomerService(call: ApplicationCall, result: CustomerServiceService.CustomerServiceResult) {
    when (result) {
        is CustomerServiceService.CustomerServiceResult.SessionList -> call.respond(result.data)
        is CustomerServiceService.CustomerServiceResult.SessionMessages -> call.respond(result.data)
        is CustomerServiceService.CustomerServiceResult.Created -> call.respond(result.data)
        is CustomerServiceService.CustomerServiceResult.Done ->
            call.respond(CustomerServiceOkResponse(result.message))
        is CustomerServiceService.CustomerServiceResult.Failed ->
            call.respond(HttpStatusCode.fromValue(result.status), ApiError(result.message))
    }
}

