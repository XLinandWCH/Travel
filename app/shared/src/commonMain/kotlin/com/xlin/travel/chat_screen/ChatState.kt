package com.xlin.travel.chat_screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xlin.travel.agent.ChatStreamEvent
import com.xlin.travel.agent.TravelModel
import com.xlin.travel.agent.TravelSystemPrompt
import com.xlin.travel.agent.chatStreaming
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.DatabaseChatHistoryProvider
import com.xlin.travel.db.MessageRecord
import com.xlin.travel.db.MessageRoles
import com.xlin.travel.db.MessageStatuses
import com.xlin.travel.db.randomUuid

/**
 * 聊天状态：维护消息列表，把消息写入 SQLite，并把 Agent 的流式事件写入对应的 AI 消息。
 *
 * @param repository 会话历史仓库
 * @param onConversationCreated 首次发送、会话记录创建完成后的回调（用于刷新抽屉并选中）
 */
@Stable
class ChatState(
    private val repository: ChatRepository,
    private val onConversationCreated: suspend (String) -> Unit = {},
) {

    private val _messages = mutableStateListOf<ChatMessage>()

    /** 按时间顺序排列的消息列表 */
    val messages: List<ChatMessage> get() = _messages

    private var nextId = 0L

    /** 是否正在等待 / 接收 AI 回复 */
    var isStreaming: Boolean by mutableStateOf(false)
        private set

    /** 当前会话 ID，同时作为 Koog ChatMemory 的 sessionId */
    var conversationId: String? by mutableStateOf(null)
        private set

    /** 开始一个全新对话：清空消息，会话记录在首次发送时写入数据库 */
    fun startNewChat() {
        _messages.clear()
        conversationId = null
        nextId = 0L
    }

    /** 打开历史对话，从数据库恢复消息 */
    suspend fun openConversation(id: String) {
        if (id == conversationId) return

        val records = runCatching { repository.messages(id) }.getOrDefault(emptyList())
        conversationId = id
        _messages.clear()
        nextId = 0L
        records.forEach { record -> record.toChatMessage()?.let { _messages.add(it) } }
    }

    /**
     * 发送用户消息：用户消息先立即上屏，再异步流式接收 AI 回复。
     * 用户消息与 AI 占位消息在请求前落库，流式结束后更新 AI 消息内容。
     */
    suspend fun send(text: String) {
        val content = text.trim()
        if (content.isEmpty() || isStreaming) return

        isStreaming = true
        try {
            val activeConversation = ensureConversation(content)

            // 本轮之前的历史分界线：ChatMemory 只加载 seq <= cutoff 的消息，
            // 避免把刚刚写入的本轮用户消息重复拼进上下文
            val cutoff = runCatching { repository.lastMessageSeq(activeConversation) }.getOrDefault(0L)

            // 用户消息先上屏，保证点击发送后立刻可见
            _messages.add(ChatMessage(nextId++, MessageRole.User, content = content))

            val reply = ChatMessage(nextId++, MessageRole.Assistant)
            _messages.add(reply)

            runCatching {
                repository.appendMessage(activeConversation, MessageRoles.User, content)
            }
            val replyRecord = runCatching {
                repository.appendMessage(
                    conversationId = activeConversation,
                    role = MessageRoles.Assistant,
                    content = "",
                    status = MessageStatuses.Sending,
                )
            }.getOrNull()

            try {
                val result = chatStreaming(
                    message = content,
                    sessionId = activeConversation,
                    historyProvider = DatabaseChatHistoryProvider(repository, untilSeq = cutoff),
                ) { event ->
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
                saveReply(replyRecord, reply, MessageStatuses.Done)
            } catch (t: Throwable) {
                reply.content = reply.content.ifEmpty { "请求失败：${t.message ?: "未知错误"}" }
                saveReply(replyRecord, reply, MessageStatuses.Error)
            }
        } finally {
            isStreaming = false
        }
    }

    /** 把 AI 回复写回数据库；失败不影响界面展示 */
    private suspend fun saveReply(record: MessageRecord?, reply: ChatMessage, status: String) {
        if (record == null) return
        runCatching {
            repository.updateMessage(
                messageId = record.id,
                content = reply.content,
                reasoning = reply.reasoning,
                status = status,
            )
        }
    }

    /** 首次发送时创建会话；创建失败也不阻塞聊天，只是本轮不落库 */
    private suspend fun ensureConversation(firstMessage: String): String {
        conversationId?.let { return it }

        val created = runCatching {
            repository.createConversation(
                title = firstMessage.toConversationTitle(),
                model = TravelModel.id,
                systemPrompt = TravelSystemPrompt,
            )
        }.getOrNull()

        val id = created?.id ?: randomUuid()
        conversationId = id
        if (created != null) onConversationCreated(id)
        return id
    }

    /** 空白的“正在发送”占位消息不展示（例如上次进程在流式过程中被杀死） */
    private fun MessageRecord.toChatMessage(): ChatMessage? {
        if (status == MessageStatuses.Sending && content.isBlank() && reasoning.isBlank()) return null
        val messageRole = when (role) {
            MessageRoles.User -> MessageRole.User
            MessageRoles.Assistant -> MessageRole.Assistant
            else -> return null
        }
        return ChatMessage(nextId++, messageRole, content = content, reasoning = reasoning)
    }
}

/** 用用户的第一句话作为会话标题 */
private fun String.toConversationTitle(): String =
    lineSequence().firstOrNull()?.trim().orEmpty().take(30).ifBlank { "新对话" }
