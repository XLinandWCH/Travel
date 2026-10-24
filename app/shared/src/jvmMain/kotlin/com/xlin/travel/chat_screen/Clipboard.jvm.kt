package com.xlin.travel.chat_screen

import androidx.compose.runtime.Composable
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/** JVM（桌面）实现：使用 AWT 系统剪贴板 */
@Composable
internal actual fun rememberClipboardWriter(): (String) -> Unit {
    val systemClipboard = Toolkit.getDefaultToolkit().systemClipboard
    return { text -> systemClipboard.setContents(StringSelection(text), null) }
}
