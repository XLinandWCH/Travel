package com.xlin.travel.db

import kotlinx.serialization.Serializable

/** 一条会话记录，对应 conversation 表；@Serializable 供 Web 端 localStorage 持久化 */
@Serializable
data class ConversationRecord(
    val id: String,
    val title: String,
    val model: String? = null,
    val systemPrompt: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 一条消息记录，对应 message 表；@Serializable 供 Web 端 localStorage 持久化 */
@Serializable
data class MessageRecord(
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val reasoning: String = "",
    val imagePath: String? = null,
    val seq: Long,
    val status: String = MessageStatuses.Done,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** message.role 的取值 */
object MessageRoles {
    const val User = "user"
    const val Assistant = "assistant"
    const val System = "system"
}

/** message.status 的取值 */
object MessageStatuses {
    const val Sending = "sending"
    const val Done = "done"
    const val Error = "error"
}

/**
 * 搜索命中的一条消息片段：直接展示消息正文而非会话标题。
 * 携带所属会话信息，点击后可跳转并定位到该会话。
 */
@Serializable
data class MessageSearchHit(
    val messageId: String,
    val conversationId: String,
    val conversationTitle: String,
    val role: String,
    val content: String,
    val createdAt: Long,
)
