package com.xlin.travel.agent

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.streaming.toMessageResponse
import kotlinx.coroutines.flow.toList

fun strategy(name: String, toolRegistry: ToolRegistry): AIAgentGraphStrategy<String, String> {

    return strategy(name) {
// 定义节点，
// 1. 定义节点
        val nodeCallLLM by nodeLLMRequestStreaming().transform { flow ->
            flow.toList().toMessageResponse()
        }

        val executeToolCall by nodeExecuteTools()

        val sendToolResult by nodeLLMSendToolResultsStreaming().transform { flow ->
            flow.toList().toMessageResponse()
        }

// 2. 边流转定义
        edge(nodeStart forwardTo nodeCallLLM)

        // 💡 关键修改：必须优先评估 onToolCalls！
// 如果大模型输出了工具调用指令，走向工具执行节点
        edge(nodeCallLLM forwardTo executeToolCall onToolCalls { true })
        // 只有当没有工具调用时，才作为普通文本输出走向 nodeFinish
        edge(nodeCallLLM forwardTo nodeFinish onTextMessage { true })

        // 工具执行完后把结果喂给 sendToolResult 节点
        edge(executeToolCall forwardTo sendToolResult)

        // 3. sendToolResult 后续的回路同理：优先判断是否还需要继续调工具，其次才是结束
        edge(sendToolResult forwardTo executeToolCall onToolCalls { true })
        edge(sendToolResult forwardTo nodeFinish onTextMessage { true })
    }
}