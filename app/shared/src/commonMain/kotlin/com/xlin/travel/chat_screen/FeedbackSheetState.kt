package com.xlin.travel.chat_screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 反馈面板的可选标签，分组与 UI设计/底部动作面板.html 保持一致 */
internal object FeedbackOptions {
    const val Title = "谢谢你的反馈，我们会继续优化进步"
    const val NoteTitle = "我要补充"
    const val NotePlaceholder = "请写下更多反馈内容"

    val groups: List<Pair<String, List<String>>> = listOf(
        "针对问题" to listOf("理解错误", "上下文错误", "未识别问题中的错误"),
        "针对回答" to listOf("事实错误", "推理错误", "内容不完整", "内容不专业", "计算错误", "违法有害"),
        "针对格式" to listOf("格式错误", "乱码错误", "内容重复", "需画图但生成文本"),
    )
}

/**
 * 底部反馈面板状态：标签多选 + 补充文字，两者都为空时不能提交。
 * 关闭或提交后自动清空，下次打开是全新的面板。
 */
@Stable
class FeedbackSheetState {

    /** 面板是否展开 */
    var visible: Boolean by mutableStateOf(false)
        private set

    /** 已选中的反馈标签 */
    var picked: Set<String> by mutableStateOf(emptySet<String>())
        private set

    /** 补充说明 */
    var note: String by mutableStateOf("")
        private set

    /** 至少选了一个标签或写了补充内容才允许提交 */
    val canSubmit: Boolean get() = picked.isNotEmpty() || note.isNotBlank()

    fun show() {
        visible = true
    }

    fun dismiss() {
        visible = false
        reset()
    }

    fun toggle(tag: String) {
        picked = if (tag in picked) picked - tag else picked + tag
    }

    fun onNoteChange(text: String) {
        note = text
    }

    /** 提交：返回拼好的反馈文本（以后可直接发给后端），同时关闭面板 */
    fun submit(): String {
        val payload = buildString {
            if (picked.isNotEmpty()) append(picked.joinToString("、"))
            if (note.isNotBlank()) {
                if (isNotEmpty()) append(" | ")
                append(note.trim())
            }
        }
        dismiss()
        return payload
    }

    private fun reset() {
        picked = emptySet()
        note = ""
    }
}
