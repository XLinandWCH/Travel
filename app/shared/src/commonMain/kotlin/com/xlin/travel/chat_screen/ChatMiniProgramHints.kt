package com.xlin.travel.chat_screen

import com.xlin.travel.a2ui.A2uiCatalog
import com.xlin.travel.miniprogram.MiniProgramCatalog

/**
 * 只在「发送给模型」时拼接：把输入框里的「@景点推荐」这类提及翻译成明确的工具调用要求，
 * 并附上 A2UI 的排版契约（让模型把这次内容再组织成一屏个性化界面）。
 *
 * 为什么不在用户消息里直接写工具名：用户看到的就是自己点的那个小程序名（@路线规划），
 * 上屏与落库保持原样；工具名只在这层提示里出现，模型据此调用对应工具，
 * 用户则在思考时间线里看到「调用景点推荐工具」的工具卡片，知道 AI 真的查了数据。
 *
 * 契约只在小程序命中时追加：普通闲聊 / 附件分析不需要一屏界面，避免每句话都生成卡片。
 *
 * 与 [withAttachmentHints] 同一个位置调用（[ChatState.send] / [ChatState.regenerate]），
 * 因此用户编辑重发、重新生成时依然生效。
 */
internal fun String.withMiniProgramHints(): String {
    val hit = MiniProgramCatalog.All.filter { this.contains("@${it.name}") }
    if (hit.isEmpty()) return this
    return buildString {
        append(this@withMiniProgramHints)
        append("\n\n[系统提示] 用户点了小程序：")
        hit.joinTo(this, "、") { "「${it.name}」" }
        append("。请先调用对应工具（")
        hit.joinTo(this, "、") { "${it.toolName}（提示：${it.toolHint}）" }
        append("），再依据工具返回的数据作答：不要编造工具结果里没有的价格、时间与点位；")
        append("正文尽量具体、分点；调用 a2ui_render 之后本轮直接结束，")
        append("不要再另写总结或邀请语（后续追问由界面底部按钮承载）。\n")
        append(A2uiCatalog.contract())
    }
}
