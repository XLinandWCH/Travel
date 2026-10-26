package com.xlin.travel.image_picker

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.xlin.travel.image_processor.AndroidImageProcessor
import java.io.File
import java.util.UUID

/**
 * Android 图片选择：使用系统 Photo Picker（PickVisualMedia）。
 * 它运行在独立的系统进程里，无需 READ_MEDIA_IMAGES 等权限，minSdk 26 可直接使用。
 *
 * 选中后先经 AndroidImageProcessor 规范化（png 保留、其余统一转 JPEG 并压缩尺寸），
 * 再写入 cacheDir/images 下一份临时文件，回调其绝对路径，
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
            // 规范化为 AI 可接收的 jpeg / png 后落盘，得到 media_analysis 工具可读的本地绝对路径
            val processed = AndroidImageProcessor.normalizeImage(
                bytes,
                context.contentResolver.getType(uri),
            )
            val path = processed?.let {
                runCatching {
                    val dir = File(context.cacheDir, "images").apply { mkdirs() }
                    val file = File(dir, "img_${UUID.randomUUID()}.${it.extension}")
                    file.writeBytes(it.bytes)
                    file.absolutePath
                }.getOrNull()
            }
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
