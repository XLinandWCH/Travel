package com.xlin.travel.routes

import com.xlin.travel.db.DatabaseFactory
import com.xlin.travel.models.ApiError
import com.xlin.travel.models.BroadcastRequest
import com.xlin.travel.models.MessageOkResponse
import com.xlin.travel.models.MessageTokenRequest
import com.xlin.travel.models.ReplyFeedbackRequest
import com.xlin.travel.models.ResolveRequest
import com.xlin.travel.models.SubmitFeedbackRequest
import com.xlin.travel.models.TicketDetailRequest
import com.xlin.travel.models.TicketStatusRequest
import com.xlin.travel.models.UserReplyRequest
import com.xlin.travel.services.FeedbackService
import com.xlin.travel.services.TicketThreadService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * 反馈 / 消息路由：提交反馈、用户收件箱、管理员列表 / 回复 / 公告。
 * 均需登录 token（服务端再校验管理员角色）；数据库未初始化返回 503。
 */
fun Route.feedbackRoutes() {
    route("/api/feedback") {

        post("/submit") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<SubmitFeedbackRequest>() ?: return@post badRequest(call)
            respondFeedback(call, FeedbackService.submit(request.token, request.content))
        }

        post("/inbox") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<MessageTokenRequest>() ?: return@post badRequest(call)
            respondFeedback(call, FeedbackService.inbox(request.token))
        }

        post("/admin/list") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<MessageTokenRequest>() ?: return@post badRequest(call)
            respondFeedback(call, FeedbackService.adminList(request.token))
        }

        post("/admin/reply") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<ReplyFeedbackRequest>() ?: return@post badRequest(call)
            respondFeedback(call, FeedbackService.reply(request.token, request.ticketId, request.reply))
        }

        post("/admin/broadcast") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<BroadcastRequest>() ?: return@post badRequest(call)
            respondFeedback(call, FeedbackService.broadcast(request.token, request))
        }

        // ---------- 工单线程（详情 / 回复 / 状态 / 解决 / 未读） ----------

        post("/detail") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<TicketDetailRequest>() ?: return@post badRequest(call)
            respondFeedback(call, TicketThreadService.detail(request.token, request.ticketId))
        }

        post("/reply") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<UserReplyRequest>() ?: return@post badRequest(call)
            respondFeedback(call, TicketThreadService.userReply(request.token, request.ticketId, request.content))
        }

        post("/admin/status") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<TicketStatusRequest>() ?: return@post badRequest(call)
            respondFeedback(call, TicketThreadService.adminStatus(request.token, request.ticketId, request.status))
        }

        post("/resolve") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<ResolveRequest>() ?: return@post badRequest(call)
            respondFeedback(call, TicketThreadService.resolve(request.token, request.ticketId, request.resolved, request.rating))
        }

        post("/unread") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<MessageTokenRequest>() ?: return@post badRequest(call)
            respondFeedback(call, TicketThreadService.unread(request.token))
        }

        post("/read") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<MessageTokenRequest>() ?: return@post badRequest(call)
            respondFeedback(call, TicketThreadService.markRead(request.token))
        }
    }
}

/** 统一把业务结果映射为 HTTP 响应 */
private suspend fun respondFeedback(call: ApplicationCall, result: FeedbackService.FeedbackResult) {
    when (result) {
        is FeedbackService.FeedbackResult.List -> call.respond(result.data)
        is FeedbackService.FeedbackResult.Submitted -> call.respond(result.data)
        is FeedbackService.FeedbackResult.Detail -> call.respond(result.data)
        is FeedbackService.FeedbackResult.Count -> call.respond(result.data)
        is FeedbackService.FeedbackResult.Done ->
            call.respond(MessageOkResponse(result.message))
        is FeedbackService.FeedbackResult.Failed ->
            call.respond(HttpStatusCode.fromValue(result.status), ApiError(result.message))
    }
}
