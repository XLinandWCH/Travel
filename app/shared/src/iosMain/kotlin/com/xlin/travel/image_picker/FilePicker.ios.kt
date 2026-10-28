package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable

/**
 * iOS「本地文件」选择桩实现。
 *
 * UIDocumentPickerViewController 在 iOS 26 / Kotlin 2.4 绑定下依赖 UniformTypeIdentifiers 的 UTType，
 * 该互操作在当前绑定存在解析风险（详见 memory：iOS 26 Foundation/Swift 化系列），
 * Windows 侧无法编译验证，首版留桩：回调 null（视为取消）。待 Mac/Xcode CI 侧补齐。
 */
@Composable
actual fun rememberFilePicker(onResult: (PickedFile?) -> Unit): () -> Unit = {
    onResult(null)
}
