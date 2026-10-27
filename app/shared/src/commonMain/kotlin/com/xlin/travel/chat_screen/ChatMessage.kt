package com.xlin.travel.chat_screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

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
 * 思考时间线里的一项，用于把「连续思考文本」与「工具调用」按发生顺序交错呈现。
 * 工具项只展示名称（如「调用识图工具」），不展示入参与返回结果。
 */
sealed class ThinkingItem {
    /** 一段思考文本（流式增量会并入同一项） */
    data class Text(val value: String) : ThinkingItem()

    /** 一次工具调用：[label] 为展示名，[running] 进行中，[failed] 校验 / 执行失败 */
    data class ToolCall(val label: String, val running: Boolean, val failed: Boolean) : ThinkingItem()
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
    /** 附带图片的本地绝对路径，仅用于「重新生成」时把路径重新交给识别工具；不落库 */
    val imagePath: String? = null,
    /** 附带图片的缩略图位图，仅在当前会话内存中随消息上屏展示；不落库 */
    val imageBitmap: ImageBitmap? = null,
    /**
     * 「本地文件」附带的文档本地绝对路径；发送时据此在本机抽取正文内联给模型，
     * 「重新生成」时重新抽取。与 imagePath 一样会落库，以便历史会话重现文件气泡。
     */
    val documentPath: String? = null,
    /** 附带文档的展示文件名（决定消息气泡的类型图标与名称）；落库 */
    val documentName: String? = null,
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

    /**
     * 本轮「深度思考」耗时（秒），仅在流式结束时于内存中写入，不落库。
     * 历史会话恢复时为 null，此时收起态标题只显示「已深度思考」不带用时。
     */
    var thinkingSeconds: Int? by mutableStateOf(null)
        internal set

    /** 是否已有任何可展示的内容 */
    val hasAnyContent: Boolean get() = content.isNotEmpty() || reasoning.isNotEmpty()

    /**
     * 本轮思考时间线（仅内存，不落库）：思考文本与工具调用按到达顺序交错，
     * 供 [ReasoningSection] 渲染「调用识图工具」这类工具卡片；历史恢复时为空、退化为纯文本思考。
     */
    var timeline: List<ThinkingItem> by mutableStateOf(emptyList())
        internal set

    private val timelineItems = mutableListOf<ThinkingItem>()

    /** 追加思考文本增量：并入最后一个文本项，否则新建一项 */
    internal fun appendTimelineReasoning(text: String) {
        val last = timelineItems.lastOrNull()
        if (last is ThinkingItem.Text) timelineItems[timelineItems.lastIndex] = last.copy(value = last.value + text)
        else timelineItems.add(ThinkingItem.Text(text))
        timeline = timelineItems.toList()
    }

    /** 记录一次工具调用开始：追加一个「进行中」的工具项 */
    internal fun appendTimelineToolCall(label: String) {
        timelineItems.add(ThinkingItem.ToolCall(label, running = true, failed = false))
        timeline = timelineItems.toList()
    }

    /** 结束最近一个进行中的工具调用：标记成功或失败 */
    internal fun finishTimelineToolCall(failed: Boolean) {
        val index = timelineItems.indexOfLast { it is ThinkingItem.ToolCall && it.running }
        if (index >= 0) {
            val call = timelineItems[index] as ThinkingItem.ToolCall
            timelineItems[index] = call.copy(running = false, failed = failed)
            timeline = timelineItems.toList()
        }
    }
}
