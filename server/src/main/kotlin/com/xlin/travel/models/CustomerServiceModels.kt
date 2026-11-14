package com.xlin.travel.models

import kotlinx.serialization.Serializable

/**
 * /api/customer-service 接口的请求 / 响应体（kotlinx.serialization）。
 * 用户发起客服会话，管理员（客服）回复消息。
 */

/** 仅需 token 的查询请求（拉取会话列表） */
@Serializable
data class CustomerServiceTokenRequest(val token: String)

/** 用户发送消息：token + 会话ID + 消息内容 + 可选图片 */
@Serializable
data class CustomerServiceSendMessageRequest(
    val token: String,
    val sessionId: String,
    val content: String,
    val imageData: String? = null    // 图片base64数据，可选
)

/** 管理员回复消息：token + 会话ID + 消息内容 + 可选图片 */
@Serializable
data class CustomerServiceAdminReplyRequest(
    val token: String,
    val sessionId: String,
    val content: String,
    val imageData: String? = null    // 图片base64数据，可选
)

/** 获取会话消息：token + 会话ID */
@Serializable
data class CustomerServiceSessionRequest(val token: String, val sessionId: String)

/** 会话列表响应 */
@Serializable
data class CustomerServiceSessionListResponse(
    val sessions: List<CustomerServiceSessionDto>,
)

/** 单条会话 */
@Serializable
data class CustomerServiceSessionDto(
    val sessionId: String,
    val username: String,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
    val createdAt: String,
)

/** 会话消息列表响应 */
@Serializable
data class CustomerServiceSessionMessagesResponse(
    val sessionId: String,
    val messages: List<CustomerServiceMessageDto>,
)

/** 单条消息 */
@Serializable
data class CustomerServiceMessageDto(
    val sender: String,
    val senderRole: String,       // 'admin' / 'user'
    val content: String,
    val imageData: String? = null,  // 图片base64数据，可选
    val createdAt: String,
)

/** 创建会话响应 */
@Serializable
data class CustomerServiceCreateSessionResponse(val message: String, val sessionId: String)

/** 通用操作成功响应 */
@Serializable
data class CustomerServiceOkResponse(val message: String)
