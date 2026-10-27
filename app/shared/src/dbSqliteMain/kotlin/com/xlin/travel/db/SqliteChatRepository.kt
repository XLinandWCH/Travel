package com.xlin.travel.db

import androidx.sqlite.execSQL

/**
 * [ChatRepository] 的 SQLite 实现，供 Android / iOS / JVM 使用。
 * 界面层通过 commonMain 的 expect fun rememberChatRepository() 拿到它，
 * 数据库文件位置与驱动由各平台 leaf 源集决定。
 */
class SqliteChatRepository(private val database: ChatDatabase) : ChatRepository {

    /** 全部会话，按最后活跃时间倒序 */
    override suspend fun conversations(): List<ConversationRecord> =
        database.withConnection { it.selectConversations() }

    /** 按标题 / 消息内容搜索会话 */
    override suspend fun searchConversations(keyword: String): List<ConversationRecord> =
        database.withConnection { it.searchConversations(keyword) }

    /** 按消息内容搜索命中片段，按时间倒序，[limit]/[offset] 分页 */
    override suspend fun searchMessageHits(keyword: String, limit: Int, offset: Int): List<MessageSearchHit> =
        database.withConnection { it.searchMessageHits(keyword, limit, offset) }

    /** 某个会话的消息；[untilSeq] 为空时返回全部消息 */
    override suspend fun messages(conversationId: String, untilSeq: Long?): List<MessageRecord> =
        database.withConnection { it.selectMessages(conversationId, untilSeq) }

    /** 会话内当前最后一条消息的顺序号，用作历史上下文的分界线 */
    override suspend fun lastMessageSeq(conversationId: String): Long =
        database.withConnection { it.lastMessageSeq(conversationId) }

    /** 新建会话，返回带 UUID 与外键时间戳的记录 */
    override suspend fun createConversation(
        title: String,
        model: String?,
        systemPrompt: String?,
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

    /** 追加一条消息，同时更新会话最后活跃时间；[imagePath] 为用户附图的本地绝对路径 */
    override suspend fun appendMessage(
        conversationId: String,
        role: String,
        content: String,
        reasoning: String,
        status: String,
        imagePath: String?,
    ): MessageRecord = database.withTransaction { connection ->
        val now = currentEpochMillis()
        val record = MessageRecord(
            id = randomUuid(),
            conversationId = conversationId,
            role = role,
            content = content,
            reasoning = reasoning,
            imagePath = imagePath,
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
    override suspend fun updateMessage(
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

    /** 删除单条消息，例如“重新生成”时移除旧的 AI 回答 */
    override suspend fun deleteMessage(id: String) {
        database.withConnection { it.deleteMessage(id) }
    }

    /**
     * 「编辑重发」用：从 [fromMessageId] 这条消息开始（含它自身）删除本会话的全部后续消息，
     * 并刷新会话活跃时间。在一个事务内先查 seq 再删除，避免其它写入插进来导致删错范围。
     */
    override suspend fun deleteMessagesFrom(conversationId: String, fromMessageId: String) {
        database.withTransaction { connection ->
            val seq = connection.messageSeq(fromMessageId) ?: return@withTransaction
            connection.deleteMessagesFrom(conversationId, seq)
            connection.updateConversationTimestamp(conversationId, currentEpochMillis())
        }
    }

    /** 删除会话及其全部消息 */
    override suspend fun deleteConversation(id: String) {
        database.withConnection { it.deleteConversation(id) }
    }

    /** 仅清空历史聊天：删除全部会话与消息，但保留 app_setting */
    override suspend fun clearChatHistory() {
        database.withTransaction { connection ->
            connection.execSQL("DELETE FROM message")
            connection.execSQL("DELETE FROM conversation")
        }
    }

    /** 重命名会话标题 */
    override suspend fun renameConversation(id: String, title: String) {
        database.withConnection { it.updateConversationTitle(id, title) }
    }

    /** 读取设置项（如主题偏好），未保存过时返回 null */
    override suspend fun getSetting(key: String): String? =
        database.withConnection { it.selectSetting(key) }

    /** 写入设置项，存在则覆盖 */
    override suspend fun setSetting(key: String, value: String) {
        database.withConnection { it.upsertSetting(key, value) }
    }

    /** 清空本地全部数据：会话、消息与所有设置（注销账号用） */
    override suspend fun clearAllLocalData() {
        database.withTransaction { connection ->
            connection.execSQL("DELETE FROM message")
            connection.execSQL("DELETE FROM conversation")
            connection.execSQL("DELETE FROM app_setting")
        }
    }
}
