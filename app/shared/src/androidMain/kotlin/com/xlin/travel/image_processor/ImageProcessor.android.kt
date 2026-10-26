package com.xlin.travel.image_processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Android 图片规范化：BitmapFactory 可解码 jpeg / png / webp / gif / bmp 等系统支持的全部格式，
 * 缩放走 createScaledBitmap，JPEG 编码走 Bitmap.compress，均为原生 API，无需第三方库。
 */
object AndroidImageProcessor {

    /**
     * 把任意格式的图片字节规范化为 AI 可接收的 jpeg / png：
     * png 原样透传，其余缩放后转 JPEG；内容无法解码时返回 null，由调用方决定回退策略。
     */
    fun normalizeImage(bytes: ByteArray, mime: String? = null): ProcessedImage? {
        if (mime == "image/png" || isPngBytes(bytes)) return ProcessedImage(bytes, "png")
        return encodeJpeg(bytes)
    }

    /** 解码任意格式 → 等比缩放到最长边 ≤ [IMAGE_MAX_EDGE] → 编码 JPEG；JPEG 失败时降级 PNG 输出 */
    private fun encodeJpeg(bytes: ByteArray): ProcessedImage? {
        val raw = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
            ?: return null
        val scaled = scaleToFit(raw)
        val jpeg = scaled.compressTo(Bitmap.CompressFormat.JPEG, IMAGE_JPEG_QUALITY)
        val result = when {
            jpeg != null -> ProcessedImage(jpeg, JPEG_EXTENSION)
            // 极端情况兜底：JPEG 编码失败时转 PNG，格式仍在 AI 接收范围内
            else -> scaled.compressTo(Bitmap.CompressFormat.PNG, 100)?.let { ProcessedImage(it, "png") }
        }
        if (scaled !== raw) scaled.recycle()
        return result
    }

    /** 等比缩放到最长边不超过 [IMAGE_MAX_EDGE]，本身已够小则原样返回 */
    private fun scaleToFit(source: Bitmap): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= IMAGE_MAX_EDGE) return source
        val ratio = IMAGE_MAX_EDGE.toFloat() / longest
        val w = (source.width * ratio).toInt().coerceAtLeast(1)
        val h = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }

    private fun Bitmap.compressTo(format: Bitmap.CompressFormat, quality: Int): ByteArray? = runCatching {
        ByteArrayOutputStream().use { out ->
            if (compress(format, quality, out)) out.toByteArray() else null
        }
    }.getOrNull()
}
