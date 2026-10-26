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

/** 普通用户在工单内追问（邮件式会话） */
@Serializable
data class UserReplyRequest(val token: String, val ticketId: String, val content: String)

/** 管理员设置工单状态：status 为 submitted / in_progress / resolved */
@Serializable
data class TicketStatusRequest(val token: String, val ticketId: String, val status: String)

/** 用户确认解决并评分：resolved=true 标已解决、false 退回处理中；rating 1..5，0 表示不评分 */
@Serializable
data class ResolveRequest(val token: String, val ticketId: String, val resolved: Boolean, val rating: Int = 0)

/** 拉取指定工单详情：仅需 token + ticketId */
@Serializable
data class TicketDetailRequest(val token: String, val ticketId: String)

/** 未读徽标查询：返回相关新消息数量 */
@Serializable
data class UnreadResponse(val count: Int)

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
    val status: String = "submitted",
    val rating: Int? = null,
)

/** 工单内一条往来消息（邮件会话中的一封） */
@Serializable
data class FeedbackMessageDto(
    val sender: String,
    val senderRole: String,       // 'admin' / 'user'
    val content: String,
    val createdAt: String,
)

/** 工单详情：原始反馈 + 回复线程 + 状态 / 评分 */
@Serializable
data class FeedbackDetailResponse(
    val ticketId: String,
    val username: String,
    val content: String,
    val createdAt: String,
    val status: String,
    val rating: Int? = null,
    val mine: Boolean,            // 该工单是否属于当前查看者（普通用户自己的反馈）
    val messages: List<FeedbackMessageDto> = emptyList(),
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
