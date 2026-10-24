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

/** 用户对 AI 回答的反馈，点击正文下方的点赞 / 踩按钮后记录在内存中 */
enum class Feedback {
    Like,
    Dislike,
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

    /** 对应 message 表的主键；数据库写入失败时为 null，重新生成时据此删除旧记录 */
    var recordId: String? = null
        internal set

    /** 用户反馈（赞同 / 不赞同），不落库，仅用于按钮高亮 */
    var feedback: Feedback? by mutableStateOf(null)
        internal set

    /** 是否已有任何可展示的内容 */
    val hasAnyContent: Boolean get() = content.isNotEmpty() || reasoning.isNotEmpty()
}
