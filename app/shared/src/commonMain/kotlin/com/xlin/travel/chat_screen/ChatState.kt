package com.xlin.travel.chat_screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.xlin.travel.agent.ChatStreamEvent
import com.xlin.travel.agent.chatStreaming

/**
 * 聊天状态：维护消息列表，并把 Agent 的流式事件写入对应的 AI 消息。
 */
@Stable
class ChatState {

    private val _messages = mutableStateListOf<ChatMessage>()

    /** 按时间顺序排列的消息列表 */
    val messages: List<ChatMessage> get() = _messages

    private var nextId = 0L

    /** 是否正在等待 / 接收 AI 回复 */
    var isStreaming: Boolean = false
        private set

    /**
     * 发送用户消息：用户消息先立即上屏，再异步流式接收 AI 回复。
     */
    suspend fun send(text: String) {
        val content = text.trim()
        if (content.isEmpty()) return

        // 用户消息先上屏，保证点击发送后立刻可见
        _messages.add(ChatMessage(nextId++, MessageRole.User, content = content))

        val reply = ChatMessage(nextId++, MessageRole.Assistant)
        _messages.add(reply)

        isStreaming = true
        try {
            val result = chatStreaming(content) { event ->
                when (event) {
                    is ChatStreamEvent.Reasoning -> reply.reasoning += event.text
                    is ChatStreamEvent.Content -> reply.content += event.text
                    ChatStreamEvent.Completed -> Unit
                }
            }
            // 兑底：若没有收到任何流式内容，用最终结果直接展示，避免界面一直停留在“正在思考”
            if (reply.content.isEmpty() && result.isNotEmpty()) {
                reply.content = result
            }
        } catch (t: Throwable) {
            reply.content = reply.content.ifEmpty { "请求失败：${t.message ?: "未知错误"}" }
        } finally {
            isStreaming = false
        }
    }
}
