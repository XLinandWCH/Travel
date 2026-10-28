package com.xlin.travel.chat_screen

import com.xlin.travel.agent.ChatStreamEvent
import com.xlin.travel.agent.ModelConfigStore
import com.xlin.travel.agent.chatStreaming
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.DatabaseChatHistoryProvider
import com.xlin.travel.db.MessageRoles
import com.xlin.travel.db.MessageStatuses
import kotlinx.coroutines.CancellationException
import kotlin.time.TimeMark
import kotlin.time.TimeSource

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
     * @param reply 已经上屏的空白 AI message，流式增量会直接写进它
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
            // 密钥 / 模型 id / 系统提示词都由服务端下发：先确保拿到配置，
            // 拉取失败走下方 catch，在 AI 气泡里展示原因
            val config = ModelConfigStore.ensure(repository)

            // 思考耗时计量：从第一条思考（reasoning / 工具）事件开始，
            // 到正文首字到达（或流结束）为止，只结算一次
            var thinkingStart: TimeMark? = null
            var thinkingFinalized = false
            fun finalizeThinking() {
                if (!thinkingFinalized) {
                    thinkingFinalized = true
                    thinkingStart?.let {
                        reply.thinkingSeconds = it.elapsedNow().inWholeSeconds.coerceAtLeast(1L).toInt()
                    }
                }
            }

            val result = chatStreaming(
                message = prompt,
                sessionId = conversationId,
                config = config,
                historyProvider = DatabaseChatHistoryProvider(repository, untilSeq = cutoffSeq),
            ) { event ->
                when (event) {
                    is ChatStreamEvent.Reasoning -> {
                        if (thinkingStart == null) thinkingStart = TimeSource.Monotonic.markNow()
                        reply.reasoning += event.text
                        reply.appendTimelineReasoning(event.text)
                    }
                    is ChatStreamEvent.Content -> {
                        finalizeThinking()
                        reply.content += event.text
                    }
                    // 工具调用不再挤进思考文本，而是作为独立时间线项呈现（仅显名称）；
                    // 工具进行中正文仍为空，isThinking 保持为真，整段思考区不中断
                    is ChatStreamEvent.ToolCallStarted -> {
                        if (thinkingStart == null) thinkingStart = TimeSource.Monotonic.markNow()
                        reply.appendTimelineToolCall(toolDisplayName(event.toolName))
                    }
                    is ChatStreamEvent.ToolCallFinished ->
                        reply.finishTimelineToolCall(failed = false)
                    is ChatStreamEvent.ToolValidationFailed ->
                        reply.finishTimelineToolCall(failed = true)
                    is ChatStreamEvent.ToolCallFailed ->
                        reply.finishTimelineToolCall(failed = true)
                    ChatStreamEvent.Completed -> finalizeThinking()
                }
            }
            finalizeThinking()
            // 兜底：若没有收到任何流式内容，用最终结果直接展示，避免界面一直停留在“正在思考”
            if (reply.content.isEmpty() && result.isNotEmpty()) {
                reply.content = result
            }
            persist(recordId, reply, MessageStatuses.Done)
        } catch (t: Throwable) {
            // 用户主动“停止生成”：保留已输出的部分内容并正常落库，不把协程取消当作错误
            if (t is CancellationException) {
                if (reply.content.isEmpty() && reply.reasoning.isEmpty()) {
                    reply.content = "（已停止生成）"
                }
                persist(recordId, reply, MessageStatuses.Done)
                throw t
            }
            reply.content = reply.content.ifEmpty { "请求失败：${t.message ?: "未知错误"}" }
            persist(recordId, reply, MessageStatuses.Error)
        }
        return recordId
    }

    /** 流式结束后把 AI 内容写回数据库；失败不影响界面展示 */
    private suspend fun persist(recordId: String?, reply: ChatMessage, status: String) {
        if (recordId == null) return
        // 思考时间线含工具调用时编码内联进 reasoning 落库（重开历史可还原工具卡片）；
        // 时间线为空（异常早退等）时退回纯 reasoning，避免丢失已输出的思考
        val storedReasoning =
            if (reply.timeline.isNotEmpty()) encodeThinkingTimeline(reply.timeline) else reply.reasoning
        runCatching {
            repository.updateMessage(
                messageId = recordId,
                content = reply.content,
                reasoning = storedReasoning,
                status = status,
            )
        }
    }
}

/** 工具名→用户可读的展示名（只展这几个字，不拼参数 / 结果）；未知工具回退为通用文案 */
private fun toolDisplayName(toolName: String): String = when (toolName) {
    "media_analysis" -> "调用识图工具"
    "document_read" -> "调用阅读工具"
    else -> "调用工具"
}
