package com.xlin.travel.models

import kotlinx.serialization.Serializable

/**
 * /api/feedback 接口的请求 / 响应体（kotlinx.serialization）。
 * 用户提交反馈（生成 #7 位工单号）；管理员查看全部反馈并逐条回复、发布公告；
 * 普通用户拉取自己的反馈回复 + 管理员公告。
 */

/** 提交反馈：token 鉴权，content 为反馈正文 */
@Serializable
data class SubmitFeedbackRequest(val token: String, val content: String)

/** 仅需 token 的查询请求（拉取收件箱 / 管理员列表） */
@Serializable
data class MessageTokenRequest(val token: String)

/** 管理员回复某条反馈：ticketId 为工单号，reply 为回复正文 */
@Serializable
data class ReplyFeedbackRequest(val token: String, val ticketId: String, val reply: String)

/** 管理员发布公告：title 标题 + content 正文 */
@Serializable
data class BroadcastRequest(val token: String, val title: String, val content: String)

/** 提交成功响应：带回生成的工单号（含 # 前缀） */
@Serializable
data class SubmitFeedbackResponse(val message: String, val ticketId: String)

/** 单条反馈（对用户隐藏他人数据；管理员看全量时带 username） */
@Serializable
data class FeedbackDto(
    val ticketId: String,
    val username: String,
    val content: String,
    val createdAt: String,
    val reply: String? = null,
    val repliedAt: String? = null,
)

/** 单条管理员公告 */
@Serializable
data class BroadcastDto(
    val id: Long,
    val title: String,
    val content: String,
    val adminUsername: String,
    val createdAt: String,
)

/** 消息列表：反馈线程 + 公告，普通用户与管理员共用同一结构 */
@Serializable
data class MessageListResponse(
    val feedbacks: List<FeedbackDto>,
    val broadcasts: List<BroadcastDto>,
)

/** 通用操作成功响应 */
@Serializable
data class MessageOkResponse(val message: String)
