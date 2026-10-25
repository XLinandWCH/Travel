package com.xlin.travel.agent

import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.streaming.toMessageResponse
import kotlinx.coroutines.flow.toList

private suspend fun AIAgentContext.historyIsTooLong(threshold: Int = 50): Boolean =
    llm.readSession { prompt.messages.size > threshold }

/**
 * 工具轮次是否已达上限：统计会话中「包含工具调用的 Assistant message」数量。
 * 用于切断弱模型反复发起同一工具调用导致的 sendToolResult ⇄ executeToolCall 死循环。
 */
private suspend fun AIAgentContext.toolRoundsExceeded(limit: Int): Boolean =
    llm.readSession {
        prompt.messages.count {
            it is Message.Assistant && it.parts.any { part -> part is MessagePart.Tool.Call }
        } >= limit
    }
fun strategy(name: String,
//             toolRegistry: ToolRegistry,
             compressionThreshold: Int = 50,
             maxToolRounds: Int = 3,
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
        // 回路加护栏：未超工具轮次上限且确有工具调用，才继续执行
        edge(sendToolResult forwardTo executeToolCall onToolCalls { true } onCondition {
            !toolRoundsExceeded(maxToolRounds)
        })
        // 正常文本答复 → 结束
        edge(sendToolResult forwardTo nodeFinish onTextMessage { true })
        // 兜底：已达轮次上限但模型仍在请求工具（无文本），强制收尾避免死循环 / 无匹配边死锁
        edge(sendToolResult forwardTo nodeFinish onCondition {
            toolRoundsExceeded(maxToolRounds)
        } transformed {
            "（已达到工具调用次数上限，请直接依据上述工具结果作答。）"
        })
    }
}