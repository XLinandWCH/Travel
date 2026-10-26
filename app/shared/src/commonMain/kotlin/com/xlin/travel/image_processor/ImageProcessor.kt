package com.xlin.travel.image_processor

/**
 * 图片格式统一处理。
 *
 * 背景：云端视觉模型（MediaAnalysisTool 的 image(Path)）只接收 jpeg / png 两种格式——
 * koog 按文件扩展名生成 mimeType = "image/" + 扩展名，webp / gif / bmp / heic 等一律被拒收；
 * iPhone 拍照、安卓截图分享链路也常常产出 HEIC / WebP。
 *
 * 规则（与需求约定一致）：
 * - png 原样保留（AI 可直接处理，且保留无损/透明通道）；
 * - 其余一切格式统一转成 JPEG，并把最长边压到 [IMAGE_MAX_EDGE] 以内、按 [IMAGE_JPEG_QUALITY] 压缩
 *   （手机原图动辄 4000px+ 数 MB，转 JPEG 后通常几百 KB，兼顾传输体积与模型输入上限）。
 *
 * 形态说明：各平台入口保持同名语义但签名不同（Android 从字节解码、iOS 直接从 UIImage 处理，
 * iOS 的 ByteArray→NSData 互操作在 Kotlin 2.4 绑定下不可用），因此不用 expect/actual，
 * 由各平台的 AndroidImageProcessor / IosImageProcessor 提供 actual 侧逻辑，此处共享规则与常量。
 * 编解码全部走平台原生 API，不引入第三方图像库。
 */

/** 非 PNG 图片转换时保留的最长边像素，超出则等比缩小 */
const val IMAGE_MAX_EDGE = 2048

/** JPEG 编码质量（0~100） */
const val IMAGE_JPEG_QUALITY = 85

/** 规范化结果：落盘字节 + 与之匹配的文件扩展名（koog 按扩展名生成 mimeType，二者必须一致） */
data class ProcessedImage(val bytes: ByteArray, val extension: String) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is ProcessedImage && extension == other.extension && bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + extension.hashCode()
}

/**
 * PNG 魔数判断（部分平台 MIME 缺失或不准，签名最可靠）。
 * png 无需任何处理，直接以 "png" 扩展名落盘即可被 AI 接收。
 */
fun isPngBytes(bytes: ByteArray): Boolean =
    bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() &&
        bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()

/** JPEG 输出统一用 "jpeg" 扩展名：koog 按扩展名拼 MIME，"image/jpg" 不合法会被接口拒收 */
const val JPEG_EXTENSION = "jpeg"
