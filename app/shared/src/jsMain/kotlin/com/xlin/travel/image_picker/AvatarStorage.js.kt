// jsMain/kotlin/com/xlin/travel/image_picker/AvatarStorage.js.kt
package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jetbrains.skia.Image

/**
 * Web 头像：
 * - [rememberAvatarPicker] 为桩实现（浏览器文件上传需 canvas 压缩 + 异步解码，
 *   首版网页不提供改头像功能，登录用户的既有头像仍可正常显示）；
 * - [decodeAvatar] 用 Skia（Compose Web 自带 skiko）把 base64 解成位图，
 *   保证能看到云端下发的头像；解码失败返回 null。
 * - [decodeImageFromPath] 返回 null：浏览器没有本地文件路径概念。
 */

@Composable
actual fun rememberAvatarPicker(onResult: (AvatarData?) -> Unit): () -> Unit = {
    onResult(null)
}

@OptIn(ExperimentalEncodingApi::class)
actual fun decodeAvatar(base64: String): ImageBitmap? = runCatching {
    val bytes = Base64.decode(base64)
    Image.makeFromEncoded(bytes).toComposeImageBitmap()
}.getOrNull()

actual fun decodeImageFromPath(path: String): ImageBitmap? = null
