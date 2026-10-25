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

    /** 是否有有效文本（对应 JS updateState 的 trim 判断） */
    var hasText: Boolean by mutableStateOf(false)
        internal set

    /** 是否已附带图片 */
    val hasImage: Boolean get() = pickedImage != null

    /** 右侧按钮是否显示发送图标：有文字或有图片时发送，否则显示麦克风 */
    val showSend: Boolean get() = hasText || hasImage

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
        voiceMode = true
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

    /** 选择器返回图片；bitmap 为 null（取消/失败）时等同于移除。[path] 为本地绝对路径，供工具识别 */
    fun onImagePicked(bitmap: ImageBitmap?, path: String?) {
        pickedImage = bitmap
        pickedImagePath = path
    }

    fun onImageRemoved() {
        pickedImage = null
        pickedImagePath = null
    }

    /** 发送成功后重置输入内容 */
    fun onSent() {
        onTextChange("")
        pickedImage = null
        pickedImagePath = null
    }
}
