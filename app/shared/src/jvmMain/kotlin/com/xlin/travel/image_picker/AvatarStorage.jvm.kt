package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * JVM（桌面）头像编解码桩实现。
 *
 * 与 [rememberImagePicker] 同理，桌面端仅用于本地测试，未接入系统相册与原生 PNG 编解码：
 * - [rememberAvatarPicker] 返回一个立即回调 null（视为取消）的 lambda；
 * - [decodeAvatar] 直接返回 null（桌面不展示头像预览）。
 * 目的是让 commonMain 的 expect 在 jvmMain 有 actual，桌面目标可正常编译。
 */
@Composable
actual fun rememberAvatarPicker(onResult: (AvatarData?) -> Unit): () -> Unit = {
    onResult(null)
}

actual fun decodeAvatar(base64: String): ImageBitmap? = null

/** 桌面端仅用于本地测试，不展示历史附图缩略图，回到仅文字气泡 */
actual fun decodeImageFromPath(path: String): ImageBitmap? = null
