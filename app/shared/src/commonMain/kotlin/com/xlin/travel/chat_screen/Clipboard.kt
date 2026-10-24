package com.xlin.travel.chat_screen

import androidx.compose.runtime.Composable

/**
 * 返回一个「把文本写入系统剪贴板」的函数。
 *
 * Compose 1.12 在 commonMain 只暴露 `Clipboard.setClipEntry(ClipEntry)`，而 ClipEntry 的
 * 构造方式各平台不同，所以这里用 expect / actual 分别实现（Android / iOS / JVM）。
 */
@Composable
internal expect fun rememberClipboardWriter(): (String) -> Unit
