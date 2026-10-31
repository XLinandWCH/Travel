package com.xlin.travel.routeplan

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

/** JVM 桌面（本地测试用）：Skia 解码，静态地图能直接看到实图 */
actual fun decodeMapImageBytes(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
