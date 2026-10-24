package com.xlin.travel.setting

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 设置页会从屏幕底部滑出的面板（帮助中心 / 意见反馈 / 两份法律文档） */
enum class SettingsSheet {
    Terms,
    Privacy,
    Help,
    Feedback,
}

/**
 * 设置页面板状态：同一时间只展开一个面板，意见反馈的草稿单独保存。
 * 由 SettingsScreen 持有，面板自身的窗口会消费返回键。
 */
@Stable
class SettingsSheetState {

    /** 当前展开的面板，null 表示全部收起 */
    var open by mutableStateOf<SettingsSheet?>(null)
        private set

    /** 意见反馈草稿 */
    var draft by mutableStateOf("")
        private set

    val visible: Boolean get() = open != null

    /** 意见反馈：写了内容才允许提交 */
    val canSubmitFeedback: Boolean get() = draft.isNotBlank()

    fun show(sheet: SettingsSheet) {
        open = sheet
    }

    fun dismiss() {
        open = null
    }

    fun onDraftChange(text: String) {
        draft = text
    }

    /**
     * 提交意见反馈：清空草稿并收起面板，返回提交内容。
     * 账号体系与上报接口接入前，内容先在这里汇总，后续可直接发给后端。
     */
    fun submitFeedback(): String {
        val content = draft.trim()
        draft = ""
        dismiss()
        return content
    }
}
