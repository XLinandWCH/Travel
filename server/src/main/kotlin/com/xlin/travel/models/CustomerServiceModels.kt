package com.xlin.travel.models

import kotlinx.serialization.Serializable

/**
 * /api/customer-service 接口的请求 / 响应体（kotlinx.serialization）。
 *
 * 会话以客户用户名（username）为归属键：普通用户收发自己的消息，
 * 管理员（users.type='admin'）按 username 选择客户并回复。
 */

/** 仅需 token 的请求（管理员拉取会话列表） */
@Serializable
data class CustomerTokenRequest(val token: String)

/** 用户发送消息：token + 内容 + 可选图片 */
@Serializable
data class CustomerSendRequest(
    val token: String,
    val content: String,
    val imageData: String? = null,
)

/** 管理员回复：token + 目标客户用户名 + 内容 + 可选图片 */
@Serializable
data class CustomerAdminReplyRequest(
    val token: String,
    val username: String,
    val content: String,
    val imageData: String? = null,
)

/** 拉取消息：token + 可选 username（管理员指定客户；普通用户忽略，恒为自己） */
@Serializable
data class CustomerMessagesRequest(
    val token: String,
    val username: String? = null,
)

/** 单条消息 */
@Serializable
data class CustomerMessageDto(
    val senderRole: String,            // 'admin' / 'user'
    val content: String,
    val imageData: String? = null,     // 图片 base64，可选
    val createdAt: String,
)

/** 消息列表响应 */
@Serializable
data class CustomerMessagesResponse(
    val messages: List<CustomerMessageDto> = emptyList(),
)

/** 单个会话（管理员视图）：客户用户名 + 最后一条消息摘要 */
@Serializable
data class CustomerConversationDto(
    val username: String,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
)

/** 会话列表响应（管理员视图） */
@Serializable
data class CustomerConversationsResponse(
    val conversations: List<CustomerConversationDto> = emptyList(),
)

/** 通用操作成功响应 */
@Serializable
data class CustomerOkResponse(val message: String)