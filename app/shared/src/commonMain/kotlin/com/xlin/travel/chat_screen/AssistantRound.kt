package com.xlin.travel.chat_screen

import com.xlin.travel.agent.ChatStreamEvent
import com.xlin.travel.agent.ModelConfigStore
import com.xlin.travel.agent.chatStreaming
import com.xlin.travel.a2ui.A2uiPartialParser
import com.xlin.travel.a2ui.A2uiRelay
import com.xlin.travel.a2ui.A2uiSurface
import com.xlin.travel.a2ui.encodeA2uiSurface
import com.xlin.travel.a2ui.stripA2uiPayload
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.DatabaseChatHistoryProvider
import com.xlin.travel.db.MessageRoles
import com.xlin.travel.db.MessageStatuses
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    ): String? = coroutineScope {
        val recordId = runCatching {
            repository.appendMessage(
                conversationId = conversationId,
                role = MessageRoles.Assistant,
                content = "",
                status = MessageStatuses.Sending,
            )
        }.getOrNull()?.id

        // 本轮开始前清空界面票据：只让本轮 a2ui_render 的产物挂到这条回复上
        A2uiRelay.clear()
        // 校验只跑一次：模型万一把 a2ui_render 调两次，不能叠出两段校验
        var verifyJob: Job? = null

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
                        // 模型调完 a2ui_render 后常会再补一段「已为你排好…」的收尾正文。
                        // 正文已经写过、入口也已出现，这段再追加就变成「正文 → 入口 → 又一段正文」，
                        // 看上去像是重复输出，所以直接丢弃；正文为空时（先调工具）仍然正常接收。
                        val afterSurface = reply.surfaceStage != SurfaceStage.None && reply.content.isNotEmpty()
                        if (!afterSurface) reply.content += event.text
                    }
                    // a2ui_render 的参数是流式的：先拿已闭合的块上屏，
                    // 入口在正文之后立刻就能点开，不必等整段 JSON 写完
                    is ChatStreamEvent.SurfaceDelta -> {
                        reply.surfaceStage = SurfaceStage.Building
                        // 还没闭合任何对象时没必要重新扫描（标题首帧就已经取到）
                        val partial = if (event.json.contains('}')) A2uiPartialParser.parse(event.json) else null
                        val known = reply.surface?.blocks?.count { !it.isEmpty } ?: 0
                        val hasNewBlock = partial != null && partial.blocks.size > known
                        val hasNewTitle = partial != null &&
                            partial.title.isNotBlank() && partial.title != reply.surface?.title
                        if (partial != null && (hasNewBlock || hasNewTitle)) reply.surface = partial
                    }
                    // 工具调用不再挤进思考文本，而是作为独立时间线项呈现（仅显名称）；
                    // 工具进行中正文仍为空，isThinking 保持为真，整段思考区不中断
                    is ChatStreamEvent.ToolCallStarted -> {
                        if (thinkingStart == null) thinkingStart = TimeSource.Monotonic.markNow()
                        reply.appendTimelineToolCall(toolDisplayName(event.toolName))
                        // 参数还没开始流式时也先把入口挂上，避免「正文写完却什么都没出现」
                        if (event.toolName == A2UI_TOOL_NAME) {
                            reply.surfaceStage = SurfaceStage.Building
                            // 内容校验与界面编排同时开始（两条线并行），
                            // 但校验块排在界面入口之后、动作按钮之前
                            if (verifyJob == null) verifyJob = launch { FakeContentVerifier.run(reply, prompt) }
                        }
                    }
                    is ChatStreamEvent.ToolCallFinished -> {
                        reply.finishTimelineToolCall(failed = false)
                        // a2ui_render 的界面结构不在返回值里，而是经 A2uiRelay 单向传递
                        if (event.toolName == A2UI_TOOL_NAME) {
                            A2uiRelay.take()?.let { surface ->
                                reply.surface = surface
                                reply.surfaceStage = SurfaceStage.Ready
                                // 界面一就绪就先写库：历史页读的是数据库，
                                // 等整轮结束才写会出现「界面已经能看了、历史里还没有」的不一致
                                launch { storeSurface(recordId, surface) }
                            }
                        }
                    }
                    is ChatStreamEvent.ToolValidationFailed -> {
                        reply.finishTimelineToolCall(failed = true)
                        // 排版工具本身失败：收起入口，后续模型解释界面没生成的那段正文才能正常显示
                        if (event.toolName == A2UI_TOOL_NAME) reply.clearSurface()
                    }
                    is ChatStreamEvent.ToolCallFailed -> {
                        reply.finishTimelineToolCall(failed = true)
                        if (event.toolName == A2UI_TOOL_NAME) reply.clearSurface()
                    }
                    ChatStreamEvent.Completed -> finalizeThinking()
                }
            }
            finalizeThinking()
            // 兜底：工具执行完但未收到 Finished 事件时，界面票据仍在槽位里
            if (reply.surface == null) reply.surface = A2uiRelay.take()
            // 有块就算可用（停止生成时已排好的半屏也保留），空壳入口直接收起
            reply.surfaceStage =
                if (reply.surface?.isEmpty == false) SurfaceStage.Ready else SurfaceStage.None
            // 兜底：若没有收到任何流式内容，用最终结果直接展示，避免界面一直停留在“正在思考”
            if (reply.content.isEmpty() && result.isNotEmpty()) {
                reply.content = result
            }
            // 模型无视契约把界面 JSON 抄进正文时，在这里剪掉，聊天里只留人话
            val cleaned = reply.content.stripA2uiPayload()
            if (cleaned != reply.content && (cleaned.isNotEmpty() || reply.surface != null)) {
                reply.content = cleaned.ifEmpty {
                    "已为这次回答排好一屏界面，点下方「进入个性化界面」查看。"
                }
            }
            // 校验也要收尾：等它跑完再落库，「校验完成」一定在动作按钮之前出现
            verifyJob?.join()
            persist(recordId, reply, MessageStatuses.Done)
        } catch (t: Throwable) {
            // 用户主动“停止生成”：保留已输出的部分内容并正常落库，不把协程取消当作错误
            if (t is CancellationException) {
                if (reply.content.isEmpty() && reply.reasoning.isEmpty()) {
                    reply.content = "（已停止生成）"
                }
                verifyJob?.join()
                persist(recordId, reply, MessageStatuses.Done)
                throw t
            }
            reply.content = reply.content.ifEmpty { "请求失败：${t.message ?: "未知错误"}" }
            // 出错时不再等界面定稿，直接把校验收起，否则会卡在「校验中」
            verifyJob?.cancel()
            persist(recordId, reply, MessageStatuses.Error)
        }
        recordId
    }

    /** 流式结束后把 AI 内容写回数据库；失败不影响界面展示 */
    private suspend fun persist(recordId: String?, reply: ChatMessage, status: String) {
        // 停止 / 失败分支也会走到这里：界面阶段必须收尾，
        // 否则入口会一直停在「正在编排」而再也不会有新块
        if (reply.surfaceStage == SurfaceStage.Building) {
            reply.surfaceStage =
                if (reply.surface?.isEmpty == false) SurfaceStage.Ready else SurfaceStage.None
        }
        // 停止 / 失败分支也要给校验收尾，否则标题会一直停在「校验中…」
        if (reply.verifyRunning) reply.verifyRunning = false
        if (recordId == null) return
        // 思考时间线含工具调用时编码内联进 reasoning 落库（重开历史可还原工具卡片）；
        // 时间线为空（异常早退等）时退回纯 reasoning，避免丢失已输出的思考
        // 校验步骤也内联进 reasoning 尾部（历史重开能还原校验卡片）；
        // 没有校验步骤时不追加任何字符，普通消息的 reasoning 与以前一模一样
        val storedReasoning =
            (if (reply.timeline.isNotEmpty()) encodeThinkingTimeline(reply.timeline) else reply.reasoning) +
                encodeVerifySteps(reply.verifySteps)
        // 界面结构一并落库：历史会话重开时入口依然可用，不必重新问一遍 AI
        val storedSurface = reply.surface
            ?.takeIf { !it.isEmpty }
            ?.let { runCatching { encodeA2uiSurface(it) }.getOrNull() }
        runCatching {
            repository.updateMessage(
                messageId = recordId,
                content = reply.content,
                reasoning = storedReasoning,
                status = status,
                surfaceJson = storedSurface,
            )
        }
    }

    /** 界面已排版完成：只写 surface_json，不等整轮回答结束（正文/状态由 [persist] 后续补齐） */
    private suspend fun storeSurface(recordId: String?, surface: A2uiSurface) {
        if (recordId == null || surface.isEmpty) return
        val json = runCatching { encodeA2uiSurface(surface) }.getOrNull() ?: return
        runCatching { repository.updateMessageSurface(recordId, json) }
    }
}

/** a2ui_render 失败：入口与半成品都撤掉，避免一直停在「正在编排」 */
private fun ChatMessage.clearSurface() {
    surface = null
    surfaceStage = SurfaceStage.None
    // 界面没排成，校验也失去了对象：一并撤掉，避免留下孤零零的「校验完成」
    verifySteps = emptyList()
    verifyRunning = false
}

/** 编排个性化界面的工具名：只有它的完成事件会从 A2uiRelay 取界面、并触发内容校验 */
private const val A2UI_TOOL_NAME = "a2ui_render"
