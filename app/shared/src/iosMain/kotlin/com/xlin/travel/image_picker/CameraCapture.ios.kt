package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * iOS 拍照桩实现。
 *
 * iOS 26 / Kotlin 2.4 绑定下相机源枚举常量（UIImagePickerControllerSourceTypeCamera）不可解析，
 * 且设置 sourceType 需要额外的 interop 处理（详见 memory：iOS 26 符号未暴露系列），
 * 首版在 Windows 侧无法编译验证，故留桩：直接回调 (null, null)，用户改用「相册」通道
 * （UIImagePickerController 相册已在 ImagePicker.ios.kt 可用）。待 Mac/Xcode CI 侧补相机实现。
 */
@Composable
actual fun rememberCameraCapture(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit = {
    onResult(null, null)
}
