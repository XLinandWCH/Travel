package com.xlin.travel.image_picker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max

/**
 * Android 头像持久化：系统 Photo Picker 选图 → 缩放 → PNG 压缩 → base64。
 * 解码走 BitmapFactory，编码走 Bitmap.compress，均为 Android 原生 API。
 */
@Composable
actual fun rememberAvatarPicker(onResult: (AvatarData?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        val data = uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { raw ->
                        val scaled = scaleToFit(raw)
                        val bytes = scaled.toPngBytes()
                        if (bytes != null) {
                            AvatarData(
                                base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                                preview = scaled.asImageBitmap(),
                            )
                        } else null
                    }
                }
            }.getOrNull()
        }
        onResult(data)
    }
    return {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}

actual fun decodeAvatar(base64: String): ImageBitmap? =
    runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()

/** 等比缩放到最长边不超过 [AVATAR_MAX_DIM]，本身已够小则原样返回 */
private fun scaleToFit(source: Bitmap): Bitmap {
    val longest = max(source.width, source.height)
    if (longest <= AVATAR_MAX_DIM) return source
    val ratio = AVATAR_MAX_DIM.toFloat() / longest
    val w = (source.width * ratio).toInt().coerceAtLeast(1)
    val h = (source.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, w, h, true)
}

private fun Bitmap.toPngBytes(): ByteArray? = runCatching {
    java.io.ByteArrayOutputStream().use { out ->
        if (compress(Bitmap.CompressFormat.PNG, 100, out)) out.toByteArray() else null
    }
}.getOrNull()
