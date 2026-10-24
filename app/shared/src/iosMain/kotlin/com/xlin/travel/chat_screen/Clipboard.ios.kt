package com.xlin.travel.chat_screen

import androidx.compose.runtime.Composable
import platform.UIKit.UIPasteboard

/** iOS 实现：写入系统 UIPasteboard */
@Composable
internal actual fun rememberClipboardWriter(): (String) -> Unit =
    { text -> UIPasteboard.general.string = text }
