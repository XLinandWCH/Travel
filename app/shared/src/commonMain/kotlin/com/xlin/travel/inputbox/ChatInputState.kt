package com.xlin.travel.inputbox

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

/** 输入模式 */
enum class InputMode(val label: String) {
    Fast("Fast")
}

/**
 * 输入框状态：对应 HTML 中 chatInput 的 has-text / has-image 类切换逻辑。
 * 图片使用真实的 [ImageBitmap]（由系统图片选择器提供），而非占位绘制。
 */
@Stable
class ChatInputState {

    /** 输入框文本 */
    var text: String by mutableStateOf("")
        internal set

    /** 已选择的图片（对应 .has-image），null 表示没有图片 */
    var pickedImage: ImageBitmap? by mutableStateOf(null)
        internal set

    /** 已选图片落盘后的本地绝对路径，供 media_analysis 工具识别；无图片时为 null */
    var pickedImagePath: String? by mutableStateOf(null)
        internal set

    /** 「+」工具栏展开附件动作面板（拍照 / 相册 / 本地文件）的开关 */
    var showAttachments: Boolean by mutableStateOf(false)
        internal set

    /** 「本地文件」选中的文档落盘后的本地绝对路径，供设备端抽取正文并落库展示；无则 null */
    var pickedDocumentPath: String? by mutableStateOf(null)
        internal set

    /** 已选文档的展示文件名（决定文件条幅/气泡的类型图标，并在发送时据此抽取正文） */
    var pickedDocumentName: String? by mutableStateOf(null)
        internal set

    /** 附件相关的瞬时提示（如「不支持的文件格式」），null 表示不显示 */
    var attachmentHint: String? by mutableStateOf(null)
        internal set

    /** 每次提示自增，作为界面重新计时 / 重播动画的 key */
    var attachmentHintTick: Long by mutableStateOf(0L)
        internal set

    /** 是否有有效文本（对应 JS updateState 的 trim 判断） */
    var hasText: Boolean by mutableStateOf(false)
        internal set

    /** 是否已附带图片 */
    val hasImage: Boolean get() = pickedImage != null

    /** 是否已附带文档 */
    val hasDocument: Boolean get() = pickedDocumentPath != null

    /** 是否已附带任意附件（图片或文档） */
    val hasAttachment: Boolean get() = hasImage || hasDocument

    /** 右侧按钮是否显示发送图标：有文字或有附件时发送，否则显示麦克风 */
    val showSend: Boolean get() = hasText || hasAttachment

    /**
     * 语音输入模式：为 true 时输入框复用同一“上中下”卡片，
     * 中部换成实时波形、底部左侧变关闭、右侧麦克风变完成发送。
     */
    var voiceMode: Boolean by mutableStateOf(false)
        internal set

    /** 进入语音模式（复用当前输入卡片，不再弹独立遮罩） */
    fun startVoice() {
        pickedImage = null
        pickedImagePath = null
        clearDocument()
        showAttachments = false
        voiceMode = true
    }

    /** 展开 / 收起附件动作面板 */
    fun toggleAttachments() {
        showAttachments = !showAttachments
    }

    /** 收起附件动作面板 */
    fun collapseAttachments() {
        showAttachments = false
    }

    /** 选择器返回图片；bitmap 为 null（取消/失败）时等同于移除。[path] 为本地绝对路径，供工具识别 */
    fun onImagePicked(bitmap: ImageBitmap?, path: String?) {
        pickedImage = bitmap
        pickedImagePath = path
    }

    fun onImageRemoved() {
        pickedImage = null
        pickedImagePath = null
    }

    /** 「本地文件」通过格式校验后写入文档附件（路径 + 文件名） */
    fun onDocumentPicked(path: String, name: String) {
        pickedDocumentPath = path
        pickedDocumentName = name
    }

    fun onDocumentRemoved() {
        clearDocument()
    }

    private fun clearDocument() {
        pickedDocumentPath = null
        pickedDocumentName = null
    }

    /** 弹出一条附件瞬时提示（如不支持的文件格式） */
    fun notifyAttachmentHint(message: String) {
        attachmentHint = message
        attachmentHintTick++
    }

    /** 清除附件提示（超时或用户再次操作后调用） */
    fun clearAttachmentHint() {
        attachmentHint = null
    }

    /** 发送成功后重置输入内容 */
    fun onSent() {
        onTextChange("")
        pickedImage = null
        pickedImagePath = null
        clearDocument()
        showAttachments = false
    }

    /** 退出语音模式 */
    fun stopVoice() {
        voiceMode = false
    }

    /** 把识别文本追加到输入框（非自动发送时用），已有文字以空格分隔 */
    fun appendRecognized(recognized: String) {
        if (recognized.isBlank()) return
        onTextChange(if (text.isBlank()) recognized else "$text $recognized")
    }

    fun onTextChange(value: String) {
        text = value
        hasText = value.isNotBlank()
    }
}
