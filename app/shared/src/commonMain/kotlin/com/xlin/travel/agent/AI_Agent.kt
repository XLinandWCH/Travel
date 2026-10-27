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
import com.xlin.travel.agent.tool.DocumentReadTool
import com.xlin.travel.agent.tool.MediaAnalysisTool

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
    val myTools = ToolRegistry {
        tool(MediaAnalysisTool)
        tool(DocumentReadTool)
    }

    val agent = AIAgent(
        promptExecutor = createChatExecutor(config),   // ← 不写 client，写 expect 函数
        llmModel = travelModel(config.modelId),
        systemPrompt = config.systemPrompt?.takeIf { it.isNotBlank() } ?: FALLBACK_SYSTEM_PROMPT,
        temperature = config.temperature ?: ModelConfigStore.DEFAULT_TEMPERATURE,
        toolRegistry = myTools,
        strategy = strategy("streaming-demo"),

    ) {
        handleEvents {
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
                    else -> {}
                }
            }

            onLLMStreamingCompleted {
                onEvent(ChatStreamEvent.Completed)
            }
        }
    }

    return agent.run(message, sessionId)
}