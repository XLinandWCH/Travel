package com.xlin.travel.routeplan

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 图片字节 → [ImageBitmap] 的平台解码（expect/actual）。
 *
 * 与头像持久化（image_picker/AvatarStorage）同一套约定：ByteArray 解码依赖各平台
 * 图形 API（Android/JVM 用 BitmapFactory，iOS/Web 用 Skia），commonMain 无法统一，
 * 故声明为 expect 由各端 actual 提供。高德静态地图返回 PNG 字节，直接走本接口上屏。
 */
expect fun decodeMapImageBytes(bytes: ByteArray): ImageBitmap?
