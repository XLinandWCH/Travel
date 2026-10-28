package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 系统相机拍照（expect/actual）。
 * 返回一个 lambda，调用它即拉起系统相机拍摄一张照片。
 *
 * 与 [rememberImagePicker] 一致：结果以 [ImageBitmap] 预览 + 本地绝对路径回调，
 * 取消或失败时两者均为 null。拍出的照片在各平台 actual 中统一规范化为 JPEG 落盘
 * （拍照产物恒为照片、无透明通道，一步转 JPEG 最省），供 media_analysis 工具按路径读取。
 *
 * Android: ACTION_IMAGE_CAPTURE + FileProvider 写入 cacheDir/camera，取全分辨率后转 JPEG
 * iOS:     UIImagePickerController 相机源（KVC 设 sourceType，绑定枚举不可解析时的稳妥写法）
 */
@Composable
expect fun rememberCameraCapture(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit
