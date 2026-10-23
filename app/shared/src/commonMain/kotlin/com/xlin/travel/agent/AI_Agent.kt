package com.xlin.travel.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import com.xlin.travel.agent.tool.CalculatorTool
import kotlinx.coroutines.runBlocking

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

suspend fun chatStreaming(message: String): String {
    val myTools = ToolRegistry {
        tool(CalculatorTool)
    }

    val agent = AIAgent(
        promptExecutor = createChatExecutor(),   // ← 不写 client，写 expect 函数
        llmModel = TravelModel,
        systemPrompt = "你是一个旅行助手……",
        temperature = 0.1,
        toolRegistry = myTools
    ) {
        handleEvents {
            var isReasoningStarted = false

            onToolCallStarting { ctx ->
                println("\n[调用工具: ${ctx.toolName}]:[输入参数 ${ctx.toolArgs}]... ")
            }

            onLLMStreamingFrameReceived { ctx ->
                when (val frame = ctx.streamFrame) {
                    is StreamFrame.ReasoningDelta -> {
                        if (!isReasoningStarted) {
                            println("\n[Reasoning]")
                            isReasoningStarted = true
                        }
                        frame.text?.let { print(it) }
                    }
                    is StreamFrame.TextDelta -> {
                        if (isReasoningStarted) {
                            println("\n--------------------")
                            isReasoningStarted = false
                        }
                        print(frame.text)
                    }
                    else -> {}
                }
            }

            onLLMStreamingCompleted {
                println()
            }
        }
    }

    return agent.run(message)
}