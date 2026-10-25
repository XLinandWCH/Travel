package com.xlin.travel.db

import androidx.sqlite.execSQL

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

    /** 追加一条消息，同时更新会话最后活跃时间；[imagePath] 为用户附图的本地绝对路径 */
    suspend fun appendMessage(
        conversationId: String,
        role: String,
        content: String,
        reasoning: String = "",
        status: String = MessageStatuses.Done,
        imagePath: String? = null,
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

    /** 删除单条消息，例如“重新生成”时移除旧的 AI 回答 */
    suspend fun deleteMessage(id: String) {
        database.withConnection { it.deleteMessage(id) }
    }

    /** 删除会话及其全部消息 */
    suspend fun deleteConversation(id: String) {
        database.withConnection { it.deleteConversation(id) }
    }

    /**
     * 仅清空历史聊天：删除全部会话与消息，但保留 app_setting（主题 / 登录缓存等）。
     * 用于“历史管理 - 清空历史”，注销账号不会走到这里（它用 [clearAllLocalData]）。
     */
    suspend fun clearChatHistory() {
        database.withTransaction { connection ->
            connection.execSQL("DELETE FROM message")
            connection.execSQL("DELETE FROM conversation")
        }
    }

    /** 重命名会话标题 */
    suspend fun renameConversation(id: String, title: String) {
        database.withConnection { it.updateConversationTitle(id, title) }
    }

    /** 读取设置项（如主题偏好），未保存过时返回 null */
    suspend fun getSetting(key: String): String? =
        database.withConnection { it.selectSetting(key) }

    /** 写入设置项，存在则覆盖 */
    suspend fun setSetting(key: String, value: String) {
        database.withConnection { it.upsertSetting(key, value) }
    }

    /**
     * 清空本地全部数据：会话、消息与所有设置（含主题 / 登录缓存）。
     * 用于“注销账号”——先删本地，再请求服务端注销，回到最初的未登入界面。
     */
    suspend fun clearAllLocalData() {
        database.withTransaction { connection ->
            connection.execSQL("DELETE FROM message")
            connection.execSQL("DELETE FROM conversation")
            connection.execSQL("DELETE FROM app_setting")
        }
    }
}
