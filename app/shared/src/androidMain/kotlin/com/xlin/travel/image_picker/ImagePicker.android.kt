package com.xlin.travel.image_picker

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.util.UUID

/**
 * Android 图片选择：使用系统 Photo Picker（PickVisualMedia）。
 * 它运行在独立的系统进程里，无需 READ_MEDIA_IMAGES 等权限，minSdk 26 可直接使用。
 *
 * 选中后把原始字节写入 cacheDir/images 下一份临时文件，回调其绝对路径，
 * 供 media_analysis 工具通过本地路径读取图片内容。
 */
@Composable
actual fun rememberImagePicker(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: android.net.Uri? ->
        if (uri == null) {
            onResult(null, null)
            return@rememberLauncherForActivityResult
        }
        val result = runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@runCatching null to null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            // 落盘一份，得到 media_analysis 工具可读的本地绝对路径
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val ext = mimeToExtension(context.contentResolver.getType(uri))
            val file = File(dir, "img_${UUID.randomUUID()}.$ext")
            val path = runCatching { file.writeBytes(bytes); file.absolutePath }.getOrNull()
            bitmap to path
        }.getOrNull()
        onResult(result?.first, result?.second)
    }
    return {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}

/**
 * 由 MIME 推断文件后缀。
 * 关键：JPEG 必须用 "jpeg" 而非 "jpg"——koog 的 image(Path) 会按文件扩展名生成
 * mimeType = "image/" + 扩展名，而 "image/jpg" 不是合法的 MIME（标准是 image/jpeg），
 * OpenAI 兼容的视觉接口会因 "image/jpg" 直接拒收，表现为「不支持 jpg」。
 * 识别不了时默认按 jpeg 保存（图片选择器多为 JPEG/PNG）。
 */
private fun mimeToExtension(mime: String?): String = when (mime) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    "image/gif" -> "gif"
    "image/bmp", "image/x-ms-bmp" -> "bmp"
    else -> "jpeg"
}
