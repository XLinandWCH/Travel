package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * JVM（桌面）图片选择器桩实现。
 *
 * 桌面端仅用于本地测试，未接入系统相册选择器，故返回一个立即回调 (null, null)（视为取消）的 lambda，
 * 保证 commonMain 的 expect 在 jvmMain 有 actual、可正常编译，且调用方流程不会挂起。
 */
@Composable
actual fun rememberImagePicker(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit = {
    onResult(null, null)
}
