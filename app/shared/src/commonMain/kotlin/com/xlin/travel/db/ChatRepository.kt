package com.xlin.travel.db

/**
 * 会话历史仓库：界面层只依赖这个接口，不直接接触 SQL / 存储细节。
 *
 * - Android / iOS / JVM：SqliteChatRepository（本地 SQLite，见 dbSqliteMain 源集）
 * - Web（Kotlin/JS）：WebChatRepository（内存 + localStorage 持久化，浏览器无 SQLite）
 */
interface ChatRepository {

    /** 全部会话，按最后活跃时间倒序 */
    suspend fun conversations(): List<ConversationRecord>

    /** 按标题 / 消息内容搜索会话 */
    suspend fun searchConversations(keyword: String): List<ConversationRecord>

    /**
     * 按消息内容搜索，返回命中的消息片段（而非会话标题），按消息时间倒序。
     * [limit] 为单页条数、[offset] 为跳过条数，供搜索页下滑分页加载。
     */
    suspend fun searchMessageHits(keyword: String, limit: Int, offset: Int): List<MessageSearchHit>

    /** 某个会话的消息；[untilSeq] 为空时返回全部消息 */
    suspend fun messages(conversationId: String, untilSeq: Long? = null): List<MessageRecord>

    /** 会话内当前最后一条消息的顺序号，用作历史上下文的分界线 */
    suspend fun lastMessageSeq(conversationId: String): Long

    /** 新建会话，返回带 UUID 与外键时间戳的记录 */
    suspend fun createConversation(
        title: String,
        model: String? = null,
        systemPrompt: String? = null,
    ): ConversationRecord

    /**
     * 追加一条消息，同时更新会话最后活跃时间；[imagePath] 为用户附图的本地绝对路径，
     * [documentPath] / [documentName] 为“本地文件”附件的本地路径与展示文件名（均可空）。
     */
    suspend fun appendMessage(
        conversationId: String,
        role: String,
        content: String,
        reasoning: String = "",
        status: String = MessageStatuses.Done,
        imagePath: String? = null,
        documentPath: String? = null,
        documentName: String? = null,
    ): MessageRecord

    /** 流式结束后更新 AI 消息内容与状态 */
    suspend fun updateMessage(
        messageId: String,
        content: String,
        reasoning: String,
        status: String,
    )

    /** 删除单条消息，例如“重新生成”时移除旧的 AI 回答 */
    suspend fun deleteMessage(id: String)

    /**
     * 「编辑重发」用：从 [fromMessageId] 这条消息开始（含它自身）删除本会话的全部后续消息，
     * 并刷新会话活跃时间。
     */
    suspend fun deleteMessagesFrom(conversationId: String, fromMessageId: String)

    /** 删除会话及其全部消息 */
    suspend fun deleteConversation(id: String)

    /**
     * 仅清空历史聊天：删除全部会话与消息，但保留 app_setting（主题 / 登录缓存等）。
     * 用于“历史管理 - 清空历史”，注销账号不会走到这里（它用 [clearAllLocalData]）。
     */
    suspend fun clearChatHistory()

    /** 重命名会话标题 */
    suspend fun renameConversation(id: String, title: String)

    /** 读取设置项（如主题偏好），未保存过时返回 null */
    suspend fun getSetting(key: String): String?

    /** 写入设置项，存在则覆盖 */
    suspend fun setSetting(key: String, value: String)

    /**
     * 清空本地全部数据：会话、消息与所有设置（含主题 / 登录缓存）。
     * 用于“注销账号”——先删本地，再请求服务端注销，回到最初的未登入界面。
     */
    suspend fun clearAllLocalData()
}
