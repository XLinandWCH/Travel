package com.xlin.travel.image_picker

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.xlin.travel.image_processor.AndroidImageProcessor
import java.io.File
import java.util.UUID

/**
 * Android 拍照：使用 ACTION_IMAGE_CAPTURE（[ActivityResultContracts.TakePicture]）。
 *
 * 该契约需要一个可写的 content:// URI 作为输出目标，这里用 FileProvider 把
 * cacheDir/camera 下的一份临时 jpg 暴露给系统相机进程；拍照无需 CAMERA 运行时权限
 * （由外部相机应用完成取景），minSdk 26 可直接使用。
 *
 * 拍摄成功后读回原始字节，统一经 [AndroidImageProcessor.normalizeImage] 规范化
 * （缩放至最长边以内并编码 JPEG，一步到位），写入 cacheDir/images 得到 media_analysis
 * 工具可读的本地绝对路径，同时解码出预览位图。取消或失败回调 (null, null)。
 */
@Composable
actual fun rememberCameraCapture(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    // 本次拍照的目标文件：launch 前写入，契约回调里按它读回内容
    var pendingFile by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success: Boolean ->
        val file = pendingFile
        pendingFile = null
        if (!success || file == null || !file.exists()) {
            onResult(null, null)
            return@rememberLauncherForActivityResult
        }
        val result = runCatching {
            val bytes = file.readBytes()
            val preview = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            // 相机产物按 JPEG 规范化（转码 + 压尺寸），落到 images 目录供工具按路径读取
            val processed = AndroidImageProcessor.normalizeImage(bytes, mime = "image/jpeg")
            val path = processed?.let {
                runCatching {
                    val dir = File(context.cacheDir, "images").apply { mkdirs() }
                    val out = File(dir, "img_${UUID.randomUUID()}.${it.extension}")
                    out.writeBytes(it.bytes)
                    out.absolutePath
                }.getOrNull()
            }
            preview to path
        }.getOrNull()
        onResult(result?.first, result?.second)
    }

    return {
        val shot = File(
            File(context.cacheDir, "camera").apply { mkdirs() },
            "cam_${UUID.randomUUID()}.jpg",
        )
        pendingFile = shot
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shot,
        )
        launcher.launch(uri)
    }
}
