package com.xlin.travel.db

/**
 * 会话历史仓库：界面层只依赖这个类，不直接接触 SQL。
 */
class ChatRepository(private val database: ChatDatabase) {

    /** 全部会话，按最后活跃时间倒序 */
    suspend fun conversations(): List<ConversationRecord> =
        database.withConnection { it.selectConversations() }

    /** 按标题 / 消息内容搜索会话 */
    suspend fun searchConversations(keyword: String): List<ConversationRecord> =
        database.withConnection { it.searchConversations(keyword) }

    /** 某个会话的消息；[untilSeq] 为空时返回全部消息 */
    suspend fun messages(conversationId: String, untilSeq: Long? = null): List<MessageRecord> =
        database.withConnection { it.selectMessages(conversationId, untilSeq) }

    /** 会话内当前最后一条消息的顺序号，用作历史上下文的分界线 */
    suspend fun lastMessageSeq(conversationId: String): Long =
        database.withConnection { it.lastMessageSeq(conversationId) }

    /** 新建会话，返回带 UUID 与外键时间戳的记录 */
    suspend fun createConversation(
        title: String,
        model: String? = null,
        systemPrompt: String? = null,
    ): ConversationRecord {
        val now = currentEpochMillis()
        val record = ConversationRecord(
            id = randomUuid(),
            title = title,
            model = model,
            systemPrompt = systemPrompt,
            createdAt = now,
            updatedAt = now,
        )
        database.withConnection { it.insertConversation(record) }
        return record
    }

    /** 追加一条消息，同时更新会话最后活跃时间 */
    suspend fun appendMessage(
        conversationId: String,
        role: String,
        content: String,
        reasoning: String = "",
        status: String = MessageStatuses.Done,
    ): MessageRecord = database.withTransaction { connection ->
        val now = currentEpochMillis()
        val record = MessageRecord(
            id = randomUuid(),
            conversationId = conversationId,
            role = role,
            content = content,
            reasoning = reasoning,
            seq = connection.nextMessageSeq(conversationId),
            status = status,
            createdAt = now,
            updatedAt = now,
        )
        connection.insertMessage(record)
        connection.updateConversationTimestamp(conversationId, now)
        record
    }

    /** 流式结束后更新 AI 消息内容与状态 */
    suspend fun updateMessage(
        messageId: String,
        content: String,
        reasoning: String,
        status: String,
    ) {
        val now = currentEpochMillis()
        database.withConnection {
            it.updateMessage(messageId, content, reasoning, status, now)
        }
    }

    /** 删除会话及其全部消息 */
    suspend fun deleteConversation(id: String) {
        database.withConnection { it.deleteConversation(id) }
    }
}
