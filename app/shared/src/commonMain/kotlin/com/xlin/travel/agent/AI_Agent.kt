package com.xlin.travel.agent

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import com.xlin.travel.agent.tool.A2uiRenderTool
import com.xlin.travel.agent.tool.AvoidPitTool
import com.xlin.travel.agent.tool.CheckInTool
import com.xlin.travel.agent.tool.DocumentReadTool
import com.xlin.travel.agent.tool.FlowAnalysisTool
import com.xlin.travel.agent.tool.FoodGuideTool
import com.xlin.travel.agent.tool.HelpConsultTool
import com.xlin.travel.agent.tool.MediaAnalysisTool
import com.xlin.travel.agent.tool.NearbySearchTool
import com.xlin.travel.agent.tool.RagKnowledgeTool
import com.xlin.travel.agent.tool.RoutePlanTool
import com.xlin.travel.agent.tool.ShowTimeTool
import com.xlin.travel.agent.tool.SpotGuideTool
import com.xlin.travel.agent.tool.SpotRecommendTool
import com.xlin.travel.agent.tool.TicketConsultTool
import com.xlin.travel.agent.tool.TrafficTool

/**
 * 旅行主模型：模型 id 由服务端 model_api_keys 下发，客户端只固定能力清单。
 */
fun travelModel(modelId: String): LLModel = LLModel(
    provider = LLMProvider.OpenAI,
    id = modelId,
    capabilities = listOf(
        LLMCapability.Completion,
        LLMCapability.Temperature,
        LLMCapability.OpenAIEndpoint.Completions,
        LLMCapability.ToolChoice,
        LLMCapability.Tools,
        LLMCapability.Thinking
    ),
    contextLength = 32_000
)

/** 服务端 system_prompt 未配置时的兜底提示词（正式提示词以云端表数据为准） */
private const val FALLBACK_SYSTEM_PROMPT =
    "你是一个旅行助手：给出可执行的行程、地点、交通与预算建议，需要工具调用时按工具结果回答，语气克制、温和、专业。"

/** 排版工具名：与 [A2uiRenderTool] 注册名一致，用于识别它的流式参数 */
private const val A2UI_RENDER_TOOL = "a2ui_render"

/**
 * 本地追加的知识库使用约束：云端 system_prompt 由服务端下发，改不了；
 * 这里补一句「先查目录再取段落」，否则模型容易凭记忆编年份、门牌与价格。
 */
private const val KNOWLEDGE_HINT =
    "\n\n[知识库] 遇到武汉里份相关的问题（历史、建筑、具体里份、路线、交通、美食、摄影、名人、保护改造），" +
        "先调用 rag_knowledge（不填参数）看章节目录，再按章节号带 query 取对应段落；只依据工具返回的资料回答，" +
        "资料里没写的就说没查到，不要凭记忆编造年份、门牌号与价格。"

// 1. commonMain 里用 expect 拿 executor；密钥与地址来自云端配置，不写死在各平台
expect fun createChatExecutor(config: RemoteModelConfig): PromptExecutor

/**
 * 运行 Agent 并流式输出。
 *
 * @param message 用户输入
 * @param sessionId 会话 ID（即 conversation.id），ChatMemory 用它区分不同会话
 * @param config 云端下发的模型配置（apiKey / baseUrl / 模型 id / 系统提示词 / 温度）
 * @param historyProvider 历史记录提供者；传入数据库实现即可跨重启恢复上下文
 * @param onEvent 流式事件回调（思考 / 正文 / 结束），由界面层渲染
 * @return 本次对话的最终完整结果
 */
suspend fun chatStreaming(
    message: String,
    sessionId: String,
    config: RemoteModelConfig,
    historyProvider: ChatHistoryProvider? = null,
    onEvent: (ChatStreamEvent) -> Unit = {},
): String {
    // 12 个小程序 = 12 个工具：用户在输入框插入「@名称」后，模型据此调用对应工具作答。
    // 内容暂为写死的武汉·里份景区数据（agent.tool.lifen 包），后续换云端数据源时无需改注册。
    // 另加 a2ui_render：模型把本轮内容整理成一屏 A2UI 界面，客户端用 a2ui 包的渲染器落地。
    val myTools = ToolRegistry {
        tool(A2uiRenderTool)
        tool(RagKnowledgeTool)
        tool(MediaAnalysisTool)
        tool(DocumentReadTool)
        tool(RoutePlanTool)
        tool(SpotRecommendTool)
        tool(NearbySearchTool)
        tool(AvoidPitTool)
        tool(ShowTimeTool)
        tool(HelpConsultTool)
        tool(TicketConsultTool)
        tool(FoodGuideTool)
        tool(TrafficTool)
        tool(SpotGuideTool)
        tool(FlowAnalysisTool)
        tool(CheckInTool)
    }

    val agent = AIAgent(
        promptExecutor = createChatExecutor(config),   // ← 不写 client，写 expect 函数
        llmModel = travelModel(config.modelId),
        systemPrompt = (config.systemPrompt?.takeIf { it.isNotBlank() } ?: FALLBACK_SYSTEM_PROMPT) + KNOWLEDGE_HINT,
        temperature = config.temperature ?: ModelConfigStore.DEFAULT_TEMPERATURE,
        toolRegistry = myTools,
        strategy = strategy("streaming-demo"),

    ) {
        handleEvents {
            // a2ui_render 的参数是流式生成的：按 index 累积参数文本，
            // 每来一个片段就向外发一次「累计快照」，界面层才能边生成边排版
            val toolCallArgs = mutableMapOf<Int, Pair<String, StringBuilder>>()

            install(ChatMemory) {
                historyProvider?.let { chatHistoryProvider(it) }
                windowSize(20)
                filterMessages { it is Message.User || it is Message.Assistant }
            }

            onToolCallStarting { ctx ->
                onEvent(ChatStreamEvent.ToolCallStarted(ctx.toolName, ctx.toolArgs.toString()))
            }

            onToolCallCompleted { ctx ->
                onEvent(ChatStreamEvent.ToolCallFinished(ctx.toolName, ctx.toolResult.toString()))
            }

            onToolValidationFailed { ctx ->
                onEvent(ChatStreamEvent.ToolValidationFailed(ctx.toolName, ctx.message))
            }

            onToolCallFailed { ctx ->
                onEvent(ChatStreamEvent.ToolCallFailed(ctx.toolName, ctx.message))
            }

            onLLMStreamingFrameReceived { ctx ->
                when (val frame = ctx.streamFrame) {
                    is StreamFrame.ReasoningDelta -> {
                        frame.text?.let { onEvent(ChatStreamEvent.Reasoning(it)) }
                    }
                    is StreamFrame.TextDelta -> {
                        onEvent(ChatStreamEvent.Content(frame.text))
                    }
                    is StreamFrame.ToolCallDelta -> {
                        // 首帧才带 name，后续片段只有参数，所以按 index 记住工具名
                        val index = frame.index ?: 0
                        val previous = toolCallArgs[index]
                        val name = frame.name?.takeIf { it.isNotBlank() } ?: previous?.first.orEmpty()
                        val args = (previous?.second ?: StringBuilder()).append(frame.content.orEmpty())
                        toolCallArgs[index] = name to args
                        if (name == A2UI_RENDER_TOOL) {
                            onEvent(ChatStreamEvent.SurfaceDelta(name, args.toString()))
                        }
                    }
                    else -> {}
                }
            }

            onLLMStreamingCompleted {
                // 一次 LLM 调用结束后清空累积：工具结果会开启下一轮，index 从 0 重新计数
                toolCallArgs.clear()
                onEvent(ChatStreamEvent.Completed)
            }
        }
    }

    return agent.run(message, sessionId)
}