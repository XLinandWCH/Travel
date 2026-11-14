package com.xlin.travel.routeplan

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Android：与头像解码同路，BitmapFactory 解 PNG 字节后转 Compose 位图 */
actual fun decodeMapImageBytes(bytes: ByteArray): ImageBitmap? =
    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
