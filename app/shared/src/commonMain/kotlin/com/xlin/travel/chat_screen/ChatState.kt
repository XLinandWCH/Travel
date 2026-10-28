package com.xlin.travel.chat_screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.xlin.travel.agent.ModelConfigStore
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.MessageRecord
import com.xlin.travel.db.MessageRoles
import com.xlin.travel.db.MessageStatuses
import com.xlin.travel.db.randomUuid
import com.xlin.travel.image_picker.decodeImageFromPath
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.coroutineContext

/**
 * 聊天状态：维护消息列表，把消息写入 SQLite，并把 Agent 的流式事件写入对应的 AI message。
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

    /** 当前正在进行的流式回复所对应的协程 Job，用于「停止生成」时取消它；空闲时为 null */
    private var activeJob: Job? = null

    /**
     * 「编辑重发」待发送的消息：点「编辑」只预填输入框并记住目标，原消息与后续历史原地不动；
     * 直到用户再次点「发送」才删掉原文及其后续、把新内容当全新轮次发出。
     * 清空输入框 / 切换会话 / 开新对话都会取消，原对话毫发无损。
     */
    var editingMessage: ChatMessage? by mutableStateOf(null)
        private set

    /** 开始一个全新对话：清空消息，会话记录在首次发送时写入数据库 */
    fun startNewChat() {
        _messages.clear()
        conversationId = null
        nextId = 0L
        editingMessage = null
    }

    /** 停止当前 AI 输出：取消正在流式的协程，保留已生成的部分内容 */
    fun stopStreaming() {
        activeJob?.cancel()
    }

    /**
     * 进入「编辑重发」：只允许最新一条用户消息（列表里最后一条 User），流式输出中不允许。
     * 本方法只记录待编辑目标，不删任何内容；预填输入框由界面层完成。
     */
    fun startEdit(message: ChatMessage) {
        if (isStreaming || message.role != MessageRole.User) return
        if (message != _messages.lastOrNull { it.role == MessageRole.User }) return
        editingMessage = message
    }

    /** 取消「编辑重发」：原消息与后续历史保持不动 */
    fun cancelEdit() {
        editingMessage = null
    }

    /** 打开历史对话，从数据库恢复消息 */
    suspend fun openConversation(id: String) {
        if (id == conversationId) return

        val records = runCatching { repository.messages(id) }.getOrDefault(emptyList())
        conversationId = id
        editingMessage = null
        _messages.clear()
        nextId = 0L
        records.forEach { record -> record.toChatMessage()?.let { _messages.add(it) } }
    }

    /**
     * 发送用户消息：用户消息先立即上屏，再异步流式接收 AI 回复。
     * 用户消息与 AI 占位消息在请求前落库，流式结束后更新 AI 消息内容。
     *
     * [imagePath] / [image] 非空时，只把真实图片路径拼进「发送给模型的提示词」，
     * 供 media_analysis 工具调用；[documentPath] / [documentName] 非空时，在本机抽取文档正文
     * 并以纯文本内联进提示词（OpenAI 兼容端点不接受文档二进制，只吃 text/image_url）。
     * 上屏与落库的用户消息都只保留原始文本，绝不会把系统提示语或抽取正文混进用户自己发送的内容里。
     */
    suspend fun send(
        text: String,
        imagePath: String? = null,
        image: ImageBitmap? = null,
        documentPath: String? = null,
        documentName: String? = null,
    ) {
        val content = text.trim()
        if ((content.isEmpty() && imagePath.isNullOrBlank() && documentPath.isNullOrBlank()) || isStreaming) return

        isStreaming = true
        activeJob = coroutineContext[Job]
        try {
            // 「编辑重发」在发送这一刻才回删：先删掉原消息及其后的全部历史（界面 + 数据库），
            // 再按全新轮次发出本轮内容；分界线 cutoff 在回删后才计算，因此上下文不会残留被删内容
            editingMessage?.let { edit ->
                editingMessage = null
                truncateFrom(edit)
            }

            // 仅作为本轮发送给模型的输入：图片路径交给工具，文档在本机抽取正文内联，用户看不到这段话
            val llmPrompt = content.withAttachmentHints(imagePath, documentPath, documentName)
            // 预热云端模型配置（密钥不再硬编码），供会话记录落库使用；
            // 失败不在这里抛出，由 AssistantRound 统一在 AI 气泡里展示错误
            runCatching { ModelConfigStore.ensure(repository) }
            val activeConversation = ensureConversation(content)

            // 本轮之前的历史分界线：ChatMemory 只加载 seq <= cutoff 的消息，
            // 避免把刚刚写入的本轮用户消息重复拼进上下文
            val cutoff = runCatching { repository.lastMessageSeq(activeConversation) }.getOrDefault(0L)

            // 上屏与落库都用用户原始文本；图片缩略图仅在内存中随消息展示
            _messages.add(
                ChatMessage(
                    id = nextId++,
                    role = MessageRole.User,
                    content = content,
                    imagePath = imagePath,
                    imageBitmap = image,
                    documentPath = documentPath,
                    documentName = documentName,
                )
            )
            val reply = ChatMessage(nextId++, MessageRole.Assistant)
            _messages.add(reply)

            runCatching {
                repository.appendMessage(
                    activeConversation, MessageRoles.User, content,
                    imagePath = imagePath, documentPath = documentPath, documentName = documentName,
                )
            }
            reply.recordId = round.run(llmPrompt, activeConversation, cutoff, reply)
        } finally {
            isStreaming = false
            activeJob = null
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
        val userMessage = _messages.subList(0, index).lastOrNull { it.role == MessageRole.User }
            ?: return
        // 重新发送时同样只把路径拼进给模型的提示词，用户消息本身仍是原始文本
        val prompt = userMessage.content.withAttachmentHints(
            userMessage.imagePath, userMessage.documentPath, userMessage.documentName,
        )

        isStreaming = true
        activeJob = coroutineContext[Job]
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
            activeJob = null
        }
    }

    /** 记录用户对某条 AI 回答的反馈（赞同 / 不赞同），只在内存中生效 */
    fun markFeedback(message: ChatMessage, feedback: Feedback) {
        if (message.role != MessageRole.Assistant) return
        message.feedback = feedback
    }

    /**
     * 删除一条用户消息，并连带删除它对应的那条 AI 回答 —— 一问一答算一组。
     * 组内成员：这条用户消息 + 紧随其后的所有 AI 消息（遇到下一条用户消息为止），
     * 本地 SQLite 逐条删除，界面同步移除。流式输出中禁止操作。
     */
    suspend fun deleteMessage(message: ChatMessage) {
        if (isStreaming || message.role != MessageRole.User) return
        val from = _messages.indexOf(message)
        if (from < 0) return
        // 待发送的「编辑重发」目标属于这一组时，一并取消
        val editIndex = editingMessage?.let { _messages.indexOf(it) } ?: -1
        // 往后收集属于这一组的 AI 回答（工具调用等异常历史可能有多条）
        var to = from + 1
        while (to < _messages.size && _messages[to].role != MessageRole.User) to++
        (from until to).forEach { i ->
            _messages[i].recordId?.let { runCatching { repository.deleteMessage(it) } }
        }
        // 从后往前删，避免删除过程中的索引漂移
        for (i in to - 1 downTo from) _messages.removeAt(i)
        if (editIndex in from until to) editingMessage = null
    }

    /**
     * 「编辑重发」的历史回删：从这条用户消息开始（含它自身）删掉界面与数据库里的后续全部内容。
     * 只在用户真正点「发送」时调用；记录未落库时只删界面内容，不走数据库操作。
     */
    private suspend fun truncateFrom(message: ChatMessage) {
        val index = _messages.indexOf(message)
        if (index < 0) return
        val conversation = conversationId
        val recordId = message.recordId
        if (conversation != null && recordId != null) {
            runCatching { repository.deleteMessagesFrom(conversation, recordId) }
        }
        while (_messages.size > index) _messages.removeAt(_messages.size - 1)
    }

    /** 首次发送时创建会话；创建失败也不阻塞聊天，只是本轮不落库 */
    private suspend fun ensureConversation(firstMessage: String): String {
        conversationId?.let { return it }

        // 模型 id 与系统提示词都来自云端配置（send 开头已预热）；尚未加载时留空
        val config = ModelConfigStore.current()
        val created = runCatching {
            repository.createConversation(
                title = firstMessage.toConversationTitle(),
                model = config?.modelId,
                systemPrompt = config?.systemPrompt,
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
            // 落库的 reasoning 可能内联了工具标记：展示用去掉标记的纯文本
            reasoning = stripThinkingMarkers(reasoning),
            imagePath = imagePath,
            imageBitmap = imagePath?.let { decodeImageFromPath(it) },
            documentPath = documentPath,
            documentName = documentName,
        )
        message.recordId = id
        // 还原思考时间线（含历史工具卡片）；无标记时为空/纯文本，与实时流式一致
        message.timeline = decodeThinkingTimeline(reasoning)
        return message
    }
}
