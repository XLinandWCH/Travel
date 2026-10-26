// jsMain/kotlin/com/xlin/travel/chat_screen/Clipboard.js.kt
package com.xlin.travel.chat_screen

import androidx.compose.runtime.Composable

/** Web 实现：navigator.clipboard.writeText（非安全上下文时静默失败） */
@Composable
internal actual fun rememberClipboardWriter(): (String) -> Unit {
    val clipboard: dynamic = js("navigator.clipboard")
    return { text ->
        runCatching { clipboard.writeText(text) }
    }
}
