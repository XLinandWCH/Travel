package com.xlin.travel.agent

/**
 * LLM 流式输出事件，用于把 AI 的推理 / 正文增量交给界面层渲染。
 */
sealed interface ChatStreamEvent {

    /** 思考（Reasoning）增量 */
    data class Reasoning(val text: String) : ChatStreamEvent

    /** 正文增量 */
    data class Content(val text: String) : ChatStreamEvent

    /** 工具开始调用 */
    data class ToolCallStarted(val toolName: String, val args: String) : ChatStreamEvent

    /** 工具调用完成，result 为执行结果 */
    data class ToolCallFinished(val toolName: String, val result: String) : ChatStreamEvent

    /** 工具参数校验失败，消息不会发送到工具实例 */
    data class ToolValidationFailed(val toolName: String, val message: String) : ChatStreamEvent

    /** 工具执行失败，结果不会发送给大模型 */
    data class ToolCallFailed(val toolName: String, val message: String) : ChatStreamEvent

    /** 本轮流式输出结束 */
    data object Completed : ChatStreamEvent
}
