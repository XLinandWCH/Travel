package com.xlin.travel.db

/** 一条会话记录，对应 conversation 表 */
data class ConversationRecord(
    val id: String,
    val title: String,
    val model: String? = null,
    val systemPrompt: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 一条消息记录，对应 message 表 */
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
