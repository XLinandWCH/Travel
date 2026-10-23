package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 系统图片选择器（expect/actual）。
 * 返回一个 lambda，调用它即拉起系统相册 / 图片选择界面。
 * 选择结果以 [ImageBitmap] 回调；取消或失败时回调 null。
 *
 * 图片字节的解码在各平台 actual 中完成（Android 用 BitmapFactory，iOS 用 Skia），
 * commonMain 不依赖平台专属的解码 API。
 *
 * Android: ActivityResultContracts.PickVisualMedia（系统照片选择器，无需权限）
 * iOS:     UIImagePickerController（相册，需要 Info.plist 权限声明）
 */
@Composable
expect fun rememberImagePicker(onResult: (ImageBitmap?) -> Unit): () -> Unit
