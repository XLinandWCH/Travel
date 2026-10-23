package com.xlin.travel.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import com.xlin.travel.agent.tool.CalculatorTool

val TravelModel = LLModel(
    provider = LLMProvider.OpenAI,
    id = "Agents-A1",
    capabilities = listOf(
        LLMCapability.Completion,
        LLMCapability.Temperature,
        LLMCapability.OpenAIEndpoint.Completions,
        LLMCapability.ToolChoice,
        LLMCapability.Tools,
        LLMCapability.Thinking
    ),
    contextLength = 2028
)

// 1. commonMain 里用 expect 拿 executor
expect fun createChatExecutor(): PromptExecutor

/**
 * 运行 Agent 并流式输出。
 *
 * @param message 用户输入
 * @param onEvent 流式事件回调（思考 / 正文 / 结束），由界面层渲染
 * @return 本次对话的最终完整结果
 */
suspend fun chatStreaming(
    message: String,
    onEvent: (ChatStreamEvent) -> Unit = {},
): String {
    val myTools = ToolRegistry {
        tool(CalculatorTool)
    }

    val agent = AIAgent(
        promptExecutor = createChatExecutor(),   // ← 不写 client，写 expect 函数
        llmModel = TravelModel,
        systemPrompt = "你是一个旅行助手……",
        temperature = 0.1,
        toolRegistry = myTools,
        strategy = strategy("streaming-demo", ToolRegistry { listOf(CalculatorTool) })
    ) {
        handleEvents {
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

    return agent.run(message)
}