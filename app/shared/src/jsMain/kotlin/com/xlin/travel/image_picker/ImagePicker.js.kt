// jsMain/kotlin/com/xlin/travel/image_picker/ImagePicker.js.kt
package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Web 图片选择器桩实现：附图识别依赖设备本地文件路径（media_analysis 工具按路径读文件），
 * 浏览器没有文件系统，首版网页不支持发图，与 JVM 相同处理：立即回调 (null, null) 视为取消。
 */
@Composable
actual fun rememberImagePicker(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit = {
    onResult(null, null)
}
