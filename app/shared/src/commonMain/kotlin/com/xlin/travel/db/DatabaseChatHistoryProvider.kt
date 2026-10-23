package com.xlin.travel.db

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo

/**
 * 把 SQLite 中保存的会话历史提供给 Koog 的 ChatMemory。
 *
 * @param repository 历史记录仓库
 * @param untilSeq 只加载这个顺序号之前的消息，用来排除本轮刚写入的用户消息，
 *                避免 Agent 把当前输入重复拼接进上下文
 */
class DatabaseChatHistoryProvider(
    private val repository: ChatRepository,
    private val untilSeq: Long? = null,
) : ChatHistoryProvider {

    override suspend fun load(conversationId: String): List<Message> {
        val records = runCatching { repository.messages(conversationId, untilSeq) }
            .getOrDefault(emptyList())
        return records.mapNotNull { it.toAgentMessage() }
    }

    override suspend fun store(conversationId: String, messages: List<Message>) {
        // 消息由 ChatState 在流式结束后落库，这里不再重复写入
    }

    private fun MessageRecord.toAgentMessage(): Message? {
        if (status == MessageStatuses.Sending || content.isBlank()) return null
        return when (role) {
            MessageRoles.User -> Message.User(content, RequestMetaInfo.Empty)
            MessageRoles.Assistant -> Message.Assistant(content, ResponseMetaInfo.Empty)
            else -> null
        }
    }
}
