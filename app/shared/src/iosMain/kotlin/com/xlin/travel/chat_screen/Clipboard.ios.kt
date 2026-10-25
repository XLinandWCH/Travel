package com.xlin.travel.chat_screen

import androidx.compose.runtime.Composable
import platform.UIKit.UIPasteboard

/**
 * iOS 实现：写入系统 UIPasteboard。
 * K/N 中类属性名是 generalPasteboard（`.general` 是 Swift 侧改名，互操作不可解析）。
 */
@Composable
internal actual fun rememberClipboardWriter(): (String) -> Unit =
    { text -> UIPasteboard.generalPasteboard.string = text }
