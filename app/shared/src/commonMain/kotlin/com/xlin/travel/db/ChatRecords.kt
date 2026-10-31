package com.xlin.travel.db

import kotlinx.serialization.Serializable

/** 一条会话记录，对应 conversation 表；@Serializable 供 Web 端 localStorage 持久化 */
@Serializable
data class ConversationRecord(
    val id: String,
    val title: String,
    val model: String? = null,
    val systemPrompt: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 一条消息记录，对应 message 表；@Serializable 供 Web 端 localStorage 持久化 */
@Serializable
data class MessageRecord(
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val reasoning: String = "",
    val imagePath: String? = null,
    /** 「本地文件」附带的文档本地绝对路径（发送时据此重新抽取正文），与 imagePath 一样可空 */
    val documentPath: String? = null,
    /** 附带文档的展示文件名（决定消息气泡的类型图标与名称） */
    val documentName: String? = null,
    /** AI 编排的个性化界面（a2ui_render 产物）序列化结果；无界面时为 null */
    val surfaceJson: String? = null,
    val seq: Long,
    val status: String = MessageStatuses.Done,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * 一条个性化界面历史记录：跨会话列出近期由 AI 编排过的 A2UI 界面，
 * 供顶栏「个性化窗口展示」页按时间由新到旧浏览。
 */
@Serializable
data class SurfaceRecord(
    val messageId: String,
    val conversationId: String,
    /** 所属会话标题，列表里用来说明「这屏是哪次聊出来的」 */
    val conversationTitle: String,
    /** A2UI 界面序列化结果，展示前解回 [com.xlin.travel.a2ui.A2uiSurface] */
    val surfaceJson: String,
    val createdAt: Long,
)

/** message.role 的取值 */
object MessageRoles {
    const val User = "user"
    const val Assistant = "assistant"
    const val System = "system"
}

/** message.status 的取值 */
object MessageStatuses {
    const val Sending = "sending"
    const val Done = "done"
    const val Error = "error"
}

/**
 * 搜索命中的一条消息片段：直接展示消息正文而非会话标题。
 * 携带所属会话信息，点击后可跳转并定位到该会话。
 */
@Serializable
data class MessageSearchHit(
    val messageId: String,
    val conversationId: String,
    val conversationTitle: String,
    val role: String,
    val content: String,
    val createdAt: Long,
)
