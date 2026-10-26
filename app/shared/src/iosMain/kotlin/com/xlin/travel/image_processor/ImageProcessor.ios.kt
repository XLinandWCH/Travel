package com.xlin.travel.image_processor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGImageGetAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * iOS 图片规范化：缩放走 UIGraphics 绘图上下文，JPEG 编码走 UIImageJPEGRepresentation，均为原生 API。
 *
 * iOS 26 / Kotlin 2.4 interop 约束（同 AvatarStorage.ios.kt）：Foundation 被 Swift 重写后，
 * ByteArray → NSData 的 dataWithBytes 系方法无法互操作，因此本模块直接以 UIImage 为输入、
 * 以编码后的 NSData 为输出交给调用方落盘（createFileAtPath 接受 NSData，无需再拆成字节）。
 *
 * 格式决策：UIImage 不携带「原始文件是 png 还是 jpeg」的信息，唯一可靠代理是透明通道——
 * 有透明（png 常见）保留 PNG，无透明（照片 / jpeg）统一转 JPEG 并压缩尺寸。
 * 注：本文件仅影响 iOS 编译目标，需在 Mac/Xcode 环境经 ios.yml CI 实测。
 */

/** 规范化结果：可直接落盘的 NSData + 匹配的文件扩展名（jpeg / png） */
data class NormalizedImage(val data: NSData, val extension: String)

object IosImageProcessor {

    /** 有透明通道 → 保留 PNG；否则等比缩放后转 JPEG；任一环节失败返回 null */
    @OptIn(ExperimentalForeignApi::class)
    fun normalizeImage(image: UIImage): NormalizedImage? = runCatching {
        if (hasAlpha(image)) {
            val png = UIImagePNGRepresentation(image) ?: return@runCatching null
            return@runCatching NormalizedImage(png, "png")
        }
        val scaled = image.scaledToFit(IMAGE_MAX_EDGE)
        val jpeg = UIImageJPEGRepresentation(scaled, IMAGE_JPEG_QUALITY / 100.0)
            ?: return@runCatching null
        NormalizedImage(jpeg, JPEG_EXTENSION)
    }.getOrNull()

    /** 是否含有效透明通道（None / NoneSkipLast / NoneSkipFirst 视为不透明，其余视为有透明） */
    @OptIn(ExperimentalForeignApi::class)
    private fun hasAlpha(image: UIImage): Boolean {
        val cg = image.CGImage ?: return false
        // iOS 26 SDK 绑定下 kCGImageAlphaNone 等常量名不再暴露；且 CGImageGetAlphaInfo 的返回类型
        // 不再是标准整型（CI 实测：toInt() 仅匹配到 String 重载，说明已被 String 化/非数值化）。
        // 因此不依赖具体类型：统一 toString 后解析枚举原始值（None=0, NoneSkipFirst=5,
        // NoneSkipLast=6 为不透明；PremultipliedLast=1, PremultipliedFirst=2, Last=3, First=4, Only=7）；
        // 解析失败按不透明处理（照片转 JPEG 是安全默认）。
        val alpha = CGImageGetAlphaInfo(cg).toString().trim().toIntOrNull()
        return alpha != null &&
            alpha != ALPHA_NONE && alpha != ALPHA_NONE_SKIP_FIRST && alpha != ALPHA_NONE_SKIP_LAST
    }
}

// CGImageAlphaInfo 原始值：None=0, PremultipliedLast=1, PremultipliedFirst=2, Last=3, First=4, NoneSkipFirst=5, NoneSkipLast=6
private const val ALPHA_NONE = 0
private const val ALPHA_NONE_SKIP_FIRST = 5
private const val ALPHA_NONE_SKIP_LAST = 6

/** 等比缩放到最长边不超过 maxDim，本身已够小则原样返回（同 AvatarStorage.ios 的绘图缩放模式） */
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.scaledToFit(maxDim: Int): UIImage {
    val width = size.useContents { width }
    val height = size.useContents { height }
    val longest = max(width, height)
    if (longest <= maxDim || longest <= 0.0) return this
    val ratio = maxDim / longest
    val newWidth = (width * ratio).roundToInt().coerceAtLeast(1)
    val newHeight = (height * ratio).roundToInt().coerceAtLeast(1)
    UIGraphicsBeginImageContextWithOptions(
        size = CGSizeMake(newWidth.toDouble(), newHeight.toDouble()),
        opaque = false,
        scale = 1.0,
    )
    drawInRect(CGRectMake(0.0, 0.0, newWidth.toDouble(), newHeight.toDouble()))
    val resized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return resized ?: this
}
