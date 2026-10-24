package com.xlin.travel.agent

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.streaming.toMessageResponse
import kotlinx.coroutines.flow.toList

private suspend fun AIAgentContext.historyIsTooLong(threshold: Int = 50): Boolean =
    llm.readSession { prompt.messages.size > threshold }
fun strategy(name: String,
             toolRegistry: ToolRegistry,
             compressionThreshold: Int = 50,
): AIAgentGraphStrategy<String, String> {

    return strategy(name) {
        // 1. 节点
        val nodeCallLLM by nodeLLMRequestStreaming().transform { flow ->
            flow.toList().toMessageResponse()
        }

        val executeToolCall by nodeExecuteTools()

        val sendToolResult by nodeLLMSendToolResultsStreaming().transform { flow ->
            flow.toList().toMessageResponse()
        }

        val compressHistory by nodeLLMCompressHistory<ReceivedToolResults>()

        // 2. 边
        edge(nodeStart forwardTo nodeCallLLM)

        // 必须先评估 onToolCalls：模型可能在一条 Assistant 消息里同时给出文本和工具调用，
        // koog 取第一条命中的边，若 onTextMessage 在前会直接走向 nodeFinish 导致工具不执行被截断
        edge(nodeCallLLM forwardTo executeToolCall onToolCalls { true })

        // 只有当没有工具调用时，才作为纯文本走向 nodeFinish
        edge(nodeCallLLM forwardTo nodeFinish onTextMessage { true })

        // 工具执行完 → 分流
        edge(executeToolCall forwardTo compressHistory onCondition {
            historyIsTooLong(compressionThreshold)
        })
        edge(executeToolCall forwardTo sendToolResult onCondition {
            !historyIsTooLong(compressionThreshold)
        })

        // 压缩完 → 继续
        edge(compressHistory forwardTo sendToolResult)

        // sendToolResult → 回路 or 结束
        edge(sendToolResult forwardTo executeToolCall onToolCalls { true })
        edge(sendToolResult forwardTo nodeFinish onTextMessage { true })
    }
}