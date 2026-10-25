package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 系统图片选择器（expect/actual）。
 * 返回一个 lambda，调用它即拉起系统相册 / 图片选择界面。
 * 选择结果以 [ImageBitmap] 与本地绝对路径回调；取消或失败时两者均为 null。
 *
 * 图片字节的解码在各平台 actual 中完成（Android 用 BitmapFactory，iOS 用 Skia），
 * commonMain 不依赖平台专属的解码 API。
 *
 * 回调第二个参数 [String] 为图片落盘后的本地绝对路径，供 media_analysis 工具读取；
 * 无法保存时返回 null（此时仅有预览位图，AI 无法识别图片内容）。
 *
 * Android: ActivityResultContracts.PickVisualMedia（系统照片选择器，无需权限），选中字节写入 cacheDir 后给出路径
 * iOS:     UIImagePickerController（相册，需要 Info.plist 权限声明），PNG 写入临时目录后给出路径
 */
@Composable
expect fun rememberImagePicker(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit
