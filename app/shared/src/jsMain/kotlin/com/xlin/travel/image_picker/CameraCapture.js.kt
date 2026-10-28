package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Web 拍照桩实现：附图识别依赖设备本地文件路径（media_analysis 工具按路径读文件），
 * 浏览器没有可交给工具的本地文件，与 [rememberImagePicker] 的 Web 桩一致，回调 (null, null)。
 */
@Composable
actual fun rememberCameraCapture(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit = {
    onResult(null, null)
}
