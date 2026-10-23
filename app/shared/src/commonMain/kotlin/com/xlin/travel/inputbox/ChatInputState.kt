package com.xlin.travel.inputbox

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

/** 输入模式（对应 HTML 中的 Fast 下拉选择） */
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

    /** 是否有有效文本（对应 JS updateState 的 trim 判断） */
    var hasText: Boolean by mutableStateOf(false)
        internal set

    /** 是否已附带图片 */
    val hasImage: Boolean get() = pickedImage != null

    /** 右侧按钮是否显示发送图标：有文字或有图片时发送，否则显示麦克风 */
    val showSend: Boolean get() = hasText || hasImage

    fun onTextChange(value: String) {
        text = value
        hasText = value.isNotBlank()
    }

    /** 选择器返回图片；bitmap 为 null（取消/失败）时等同于移除 */
    fun onImagePicked(bitmap: ImageBitmap?) {
        pickedImage = bitmap
    }

    fun onImageRemoved() {
        pickedImage = null
    }

    /** 发送成功后重置输入内容 */
    fun onSent() {
        onTextChange("")
        pickedImage = null
    }
}
