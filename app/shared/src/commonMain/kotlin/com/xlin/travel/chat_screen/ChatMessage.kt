package com.xlin.travel.chat_screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 消息角色 */
enum class MessageRole {
    User,
    Assistant,
}

/**
 * 单条聊天消息。
 * AI 消息把「思考内容」与「正文内容」分开保存，便于界面用不同颜色渲染。
 */
@Stable
class ChatMessage(
    val id: Long,
    val role: MessageRole,
    content: String = "",
    reasoning: String = "",
) {
    /** 正文内容 */
    var content: String by mutableStateOf(content)
        internal set

    /** 思考（Reasoning）内容 */
    var reasoning: String by mutableStateOf(reasoning)
        internal set

    /** 是否已有任何可展示的内容 */
    val hasAnyContent: Boolean get() = content.isNotEmpty() || reasoning.isNotEmpty()
}
