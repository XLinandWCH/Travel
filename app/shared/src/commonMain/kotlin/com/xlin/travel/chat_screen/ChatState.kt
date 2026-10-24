package com.xlin.travel.chat_screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xlin.travel.agent.TravelModel
import com.xlin.travel.agent.TravelSystemPrompt
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.MessageRecord
import com.xlin.travel.db.MessageRoles
import com.xlin.travel.db.MessageStatuses
import com.xlin.travel.db.randomUuid

/**
 * 聊天状态：维护消息列表，把消息写入 SQLite，并把 Agent 的流式事件写入对应的 AI 消息。
 *
 * 一轮回复的流式过程拆到 [AssistantRound]，发送新消息与重新生成共用。
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

    private val round = AssistantRound(repository)

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
            reply.recordId = round.run(content, activeConversation, cutoff, reply)
        } finally {
            isStreaming = false
        }
    }

    /**
     * 重新生成最后一条 AI 回答：先从数据库和界面删除旧回答，再用它对应的用户提示词重新请求一次。
     * 用户消息保留，因此新回答会接在同一句话之后。
     */
    suspend fun regenerate() {
        val index = _messages.indexOfLast { it.role == MessageRole.Assistant }
        val conversation = conversationId ?: return
        if (index < 0 || isStreaming) return
        val prompt = _messages.subList(0, index).lastOrNull { it.role == MessageRole.User }?.content
            ?: return

        isStreaming = true
        try {
            // 删除旧的 AI 记录，历史上下文才会停在用户提示词之前
            _messages[index].recordId?.let { oldId ->
                runCatching { repository.deleteMessage(oldId) }
            }
            val reply = ChatMessage(nextId++, MessageRole.Assistant)
            _messages[index] = reply

            // 此时数据库最后一条就是本轮的用户提示词，它作为新输入传入，
            // 需要再用 -1 把它从历史上下文中排除，避免重复拼接
            val lastSeq = runCatching { repository.lastMessageSeq(conversation) }.getOrDefault(0L)
            val cutoff = (lastSeq - 1).coerceAtLeast(0L)
            reply.recordId = round.run(prompt, conversation, cutoff, reply)
        } finally {
            isStreaming = false
        }
    }

    /** 记录用户对某条 AI 回答的反馈（赞同 / 不赞同），只在内存中生效 */
    fun markFeedback(message: ChatMessage, feedback: Feedback) {
        if (message.role != MessageRole.Assistant) return
        message.feedback = feedback
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
        val message = ChatMessage(
            id = nextId++,
            role = messageRole,
            content = content,
            reasoning = reasoning,
        )
        message.recordId = id
        return message
    }
}

/** 用用户的第一句话作为会话标题 */
private fun String.toConversationTitle(): String =
    lineSequence().firstOrNull()?.trim().orEmpty().take(30).ifBlank { "新对话" }
