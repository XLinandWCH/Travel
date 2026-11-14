package com.xlin.travel.routeplan

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

/** iOS：Skia 解码（与 AvatarStorage.ios 的 decodeAvatar 同一套路） */
actual fun decodeMapImageBytes(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
