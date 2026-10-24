package com.xlin.travel.chat_screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/**
 * Android 实现：使用 [LocalClipboardManager]（Compose 1.12 中已废弃但仍可用，
 * 新的 `LocalClipboard` 需要构造平台相关的 ClipData，这里不必绕这一层）。
 */
@Suppress("DEPRECATION")
@Composable
internal actual fun rememberClipboardWriter(): (String) -> Unit {
    val manager = LocalClipboardManager.current
    return { text -> manager.setText(AnnotatedString(text)) }
}
