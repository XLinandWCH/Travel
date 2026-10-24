package com.xlin.travel.agent

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import com.xlin.travel.agent.tool.CalculatorTool
import kotlinx.io.files.Path

val TravelModel = LLModel(
    provider = LLMProvider.OpenAI,
    id = "Sage-A1.5",
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

// 1. commonMain 里用 expect 拿 executor
expect fun createChatExecutor(): PromptExecutor

/** 旅行助手的系统提示词，同时会随会话记录一起存入数据库 */
const val TravelSystemPrompt = """
    你是一个名为 Claude 的 AI 助手，由 Anthropic 制作。（如果有人问起你是什么模型你就如实回答就行，没人问AI模型就不要说这个
    
    你现在运行在一个旅行助手产品里，用户问旅行相关问题时：
    - 给出可执行的行程、地点、交通、预算建议
    - 需要工具调用时按工具结果回答
    - 保持 Claude 的克制、温和、专业语气
    
    旅游助手的产品名称：可达鸭。这个名称随时可能更改，不稳定。
"""

/**
 * 运行 Agent 并流式输出。
 *
 * @param message 用户输入
 * @param sessionId 会话 ID（即 conversation.id），ChatMemory 用它区分不同会话
 * @param historyProvider 历史记录提供者；传入数据库实现即可跨重启恢复上下文
 * @param onEvent 流式事件回调（思考 / 正文 / 结束），由界面层渲染
 * @return 本次对话的最终完整结果
 */
suspend fun chatStreaming(
    message: String,
    sessionId: String,
    historyProvider: ChatHistoryProvider? = null,
    onEvent: (ChatStreamEvent) -> Unit = {},
): String {
    val myTools = ToolRegistry {
        tool(CalculatorTool)
    }

    val agent = AIAgent(
        promptExecutor = createChatExecutor(),   // ← 不写 client，写 expect 函数
        llmModel = TravelModel,
        systemPrompt = TravelSystemPrompt,
        temperature = 0.8,
        toolRegistry = myTools,
        strategy = strategy("streaming-demo", myTools),

    ) {
        handleEvents {
            install(ChatMemory) {
                historyProvider?.let { chatHistoryProvider(it) }
                windowSize(20)
                filterMessages { it is Message.User || it is Message.Assistant }
            }

            onToolCallStarting { ctx ->
                println("\n[调用工具: ${ctx.toolName}]:[输入参数 ${ctx.toolArgs}]... ")
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