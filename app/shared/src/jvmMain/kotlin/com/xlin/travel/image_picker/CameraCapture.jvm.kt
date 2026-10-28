package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * JVM（桌面）拍照桩实现：桌面端仅用于本地测试，未接入系统相机，
 * 与 [rememberImagePicker] 的 JVM 桩一致，回调 (null, null) 视为取消。
 */
@Composable
actual fun rememberCameraCapture(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit = {
    onResult(null, null)
}
