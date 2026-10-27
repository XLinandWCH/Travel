// jsMain/kotlin/com/xlin/travel/db/WebChatRepository.kt
package com.xlin.travel.db

import kotlinx.browser.localStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.w3c.dom.Storage

/**
 * [ChatRepository] 的 Web 实现：内存列表 + localStorage 快照。
 *
 * 浏览器里 androidx.sqlite 只有异步 Worker 驱动（需要额外部署 SQLite WASM），
 * 个人使用场景数据量小，整仓 JSON 快照写入 localStorage 足够，且刷新 / 重开页可恢复。
 * 超出配额（约 5MB）时静默放弃持久化，当次会话仍在内存中可用。
 */
class WebChatRepository : ChatRepository {

    @Serializable
    private data class Snapshot(
        val conversations: List<ConversationRecord> = emptyList(),
        val messages: List<MessageRecord> = emptyList(),
        val settings: Map<String, String> = emptyMap(),
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val store: Storage = localStorage

    private var conversations: MutableList<ConversationRecord> = mutableListOf()
    private var messages: MutableList<MessageRecord> = mutableListOf()
    private var settings: MutableMap<String, String> = mutableMapOf()

    init {
        runCatching {
            store.getItem(KEY)?.let { raw ->
                val snapshot = json.decodeFromString(Snapshot.serializer(), raw)
                conversations = snapshot.conversations.toMutableList()
                messages = snapshot.messages.toMutableList()
                settings = snapshot.settings.toMutableMap()
            }
        }
    }

    private fun persist() {
        runCatching {
            store.setItem(
                KEY,
                json.encodeToString(
                    Snapshot.serializer(),
                    Snapshot(conversations.toList(), messages.toList(), settings.toMap()),
                ),
            )
        }
    }

    override suspend fun conversations(): List<ConversationRecord> =
        conversations.sortedByDescending { it.updatedAt }

    override suspend fun searchConversations(keyword: String): List<ConversationRecord> {
        val matchedConversations = conversations
            .filter { it.title.contains(keyword, ignoreCase = true) }
            .toMutableSet()
        messages
            .filter { it.content.contains(keyword, ignoreCase = true) }
            .forEach { msg ->
                conversations.firstOrNull { it.id == msg.conversationId }
                    ?.let { matchedConversations.add(it) }
            }
        return matchedConversations.sortedByDescending { it.updatedAt }
    }

    /** 与 SQLite 端一致：按消息正文命中，带会话标题，按时间倒序分页 */
    override suspend fun searchMessageHits(keyword: String, limit: Int, offset: Int): List<MessageSearchHit> =
        messages
            .filter { it.content.contains(keyword, ignoreCase = true) }
            .sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
            .map { msg ->
                MessageSearchHit(
                    messageId = msg.id,
                    conversationId = msg.conversationId,
                    conversationTitle = conversations.firstOrNull { it.id == msg.conversationId }?.title.orEmpty(),
                    role = msg.role,
                    content = msg.content,
                    createdAt = msg.createdAt,
                )
            }

    override suspend fun messages(conversationId: String, untilSeq: Long?): List<MessageRecord> =
        messages
            .filter {
                it.conversationId == conversationId && (untilSeq == null || it.seq <= untilSeq)
            }
            .sortedBy { it.seq }

    override suspend fun lastMessageSeq(conversationId: String): Long =
        messages.filter { it.conversationId == conversationId }
            .maxOfOrNull { it.seq } ?: 0L

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
        conversations.add(record)
        persist()
        return record
    }

    override suspend fun appendMessage(
        conversationId: String,
        role: String,
        content: String,
        reasoning: String,
        status: String,
        imagePath: String?,
        documentPath: String?,
        documentName: String?,
    ): MessageRecord {
        val now = currentEpochMillis()
        val record = MessageRecord(
            id = randomUuid(),
            conversationId = conversationId,
            role = role,
            content = content,
            reasoning = reasoning,
            imagePath = imagePath,
            documentPath = documentPath,
            documentName = documentName,
            seq = (lastMessageSeq(conversationId)) + 1,
            status = status,
            createdAt = now,
            updatedAt = now,
        )
        messages.add(record)
        touchConversation(conversationId, now)
        persist()
        return record
    }

    override suspend fun updateMessage(
        messageId: String,
        content: String,
        reasoning: String,
        status: String,
    ) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val old = messages[index]
        messages[index] = old.copy(
            content = content,
            reasoning = reasoning,
            status = status,
            updatedAt = currentEpochMillis(),
        )
        persist()
    }

    override suspend fun deleteMessage(id: String) {
        messages.removeAll { it.id == id }
        persist()
    }

    override suspend fun deleteMessagesFrom(conversationId: String, fromMessageId: String) {
        val seq = messages.firstOrNull { it.id == fromMessageId }?.seq ?: return
        messages.removeAll { it.conversationId == conversationId && it.seq >= seq }
        touchConversation(conversationId, currentEpochMillis())
        persist()
    }

    override suspend fun deleteConversation(id: String) {
        conversations.removeAll { it.id == id }
        messages.removeAll { it.conversationId == id }
        persist()
    }

    override suspend fun clearChatHistory() {
        conversations.clear()
        messages.clear()
        persist()
    }

    override suspend fun renameConversation(id: String, title: String) {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return
        // 与 SQLite 端一致：重命名不刷新活跃时间，避免抽屉重排
        conversations[index] = conversations[index].copy(title = title)
        persist()
    }

    override suspend fun getSetting(key: String): String? = settings[key]

    override suspend fun setSetting(key: String, value: String) {
        settings[key] = value
        persist()
    }

    override suspend fun clearAllLocalData() {
        conversations.clear()
        messages.clear()
        settings.clear()
        runCatching { store.removeItem(KEY) }
    }

    private fun touchConversation(id: String, updatedAt: Long) {
        val index = conversations.indexOfFirst { it.id == id }
        if (index >= 0) conversations[index] = conversations[index].copy(updatedAt = updatedAt)
    }

    private companion object {
        const val KEY = "travel_web_db"
    }
}
