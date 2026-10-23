package com.xlin.travel.agent

/**
 * LLM 流式输出事件，用于把 AI 的推理 / 正文增量交给界面层渲染。
 */
sealed interface ChatStreamEvent {

    /** 思考（Reasoning）增量 */
    data class Reasoning(val text: String) : ChatStreamEvent

    /** 正文增量 */
    data class Content(val text: String) : ChatStreamEvent

    /** 本轮流式输出结束 */
    data object Completed : ChatStreamEvent
}
