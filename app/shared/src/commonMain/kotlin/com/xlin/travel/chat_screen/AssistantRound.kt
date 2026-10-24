package com.xlin.travel.chat_screen

import com.xlin.travel.agent.ChatStreamEvent
import com.xlin.travel.agent.chatStreaming
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.DatabaseChatHistoryProvider
import com.xlin.travel.db.MessageRoles
import com.xlin.travel.db.MessageStatuses

/**
 * 一轮 AI 回复的完整过程：占位落库 → 流式接收 → 回写内容与状态。
 *
 * 发送新消息与「重新生成」都复用这一套逻辑，区别只在于调用方传入的
 * [cutoffSeq]（上下文分界线）以及是否需要先删除旧的 AI 记录。
 */
internal class AssistantRound(
    private val repository: ChatRepository,
) {

    /**
     * @param reply 已经上屏的空白 AI 消息，流式增量会直接写进它
     * @param cutoffSeq 只加载 seq 小于等于该值的历史，用于排除本轮的用户提示词
     * @return 这条 AI 回复在 message 表中的主键，落库失败时为 null
     */
    suspend fun run(
        prompt: String,
        conversationId: String,
        cutoffSeq: Long,
        reply: ChatMessage,
    ): String? {
        val recordId = runCatching {
            repository.appendMessage(
                conversationId = conversationId,
                role = MessageRoles.Assistant,
                content = "",
                status = MessageStatuses.Sending,
            )
        }.getOrNull()?.id

        try {
            val result = chatStreaming(
                message = prompt,
                sessionId = conversationId,
                historyProvider = DatabaseChatHistoryProvider(repository, untilSeq = cutoffSeq),
            ) { event ->
                when (event) {
                    is ChatStreamEvent.Reasoning -> reply.reasoning += event.text
                    is ChatStreamEvent.Content -> reply.content += event.text
                    ChatStreamEvent.Completed -> Unit
                }
            }
            // 兜底：若没有收到任何流式内容，用最终结果直接展示，避免界面一直停留在“正在思考”
            if (reply.content.isEmpty() && result.isNotEmpty()) {
                reply.content = result
            }
            persist(recordId, reply, MessageStatuses.Done)
        } catch (t: Throwable) {
            reply.content = reply.content.ifEmpty { "请求失败：${t.message ?: "未知错误"}" }
            persist(recordId, reply, MessageStatuses.Error)
        }
        return recordId
    }

    /** 流式结束后把 AI 内容写回数据库；失败不影响界面展示 */
    private suspend fun persist(recordId: String?, reply: ChatMessage, status: String) {
        if (recordId == null) return
        runCatching {
            repository.updateMessage(
                messageId = recordId,
                content = reply.content,
                reasoning = reply.reasoning,
                status = status,
            )
        }
    }
}
