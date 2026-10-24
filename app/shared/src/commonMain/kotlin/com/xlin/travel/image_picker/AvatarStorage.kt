package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 头像持久化编解码（expect/actual）。
 *
 * 与 [rememberImagePicker] 同理：图片字节与各平台原生编解码 API 绑定（Android 用 Bitmap/BitmapFactory，
 * iOS 用 UIImage/Skia），commonMain 无法统一完成 ImageBitmap↔PNG 的编码，故放到各平台 actual。
 *
 * 约定：各 actual 在选图时把图片缩放到最长边 ≤ [AVATAR_MAX_DIM] 像素，再以 PNG 编成 base64，
 * 保证本地 SQLite 与服务器存储 / 传输的体积可控（通常几十 KB）。
 */

/** 头像最长边像素上限，压缩后据此缩放 */
const val AVATAR_MAX_DIM = 512

/** 选图结果：持久化用的 base64 与可直接上屏的预览位图 */
data class AvatarData(val base64: String, val preview: ImageBitmap)

/**
 * 头像选择器：拉起系统相册，选中后回调 [AvatarData]（缩放 + PNG + base64），取消回调 null。
 * 与聊天附图的 [rememberImagePicker] 分离，互不影响。
 */
@Composable
expect fun rememberAvatarPicker(onResult: (AvatarData?) -> Unit): () -> Unit

/** 把持久化的 base64 解码为预览位图；数据损坏 / 平台不支持时返回 null */
expect fun decodeAvatar(base64: String): ImageBitmap?
