package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable

/**
 * JVM（桌面）「本地文件」选择桩实现：桌面端仅用于本地测试，未接入系统文档选择器，
 * 回调 null 视为取消，保证 commonMain 的 expect 在 jvmMain 有 actual、可正常编译。
 */
@Composable
actual fun rememberFilePicker(onResult: (PickedFile?) -> Unit): () -> Unit = {
    onResult(null)
}
