package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.LocalUIViewController
import com.xlin.travel.io.readFileBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.darwin.NSObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * iOS 头像持久化：UIImagePickerController 从相册选图 → 缩放 → PNG → base64。
 *
 * iOS 26 / Kotlin 2.4 interop 注意（同 ImagePicker.ios.kt）：Foundation 已被
 * Swift 重写，NSData 的 base64 / getBytes / create 系方法无法互操作——
 * 写文件走 NSFileManager.createFileAtPath，读字节走 POSIX（readFileBytes），
 * base64 编解码走 stdlib kotlin.io.encoding.Base64。
 */
@Composable
actual fun rememberAvatarPicker(onResult: (AvatarData?) -> Unit): () -> Unit {
    val host = LocalUIViewController.current
    val delegate = remember(onResult) { AvatarPickerDelegate(onResult) }
    return {
        val picker = UIImagePickerController()
        picker.delegate = delegate
        host.presentViewController(picker, animated = true, completion = null)
    }
}

@OptIn(ExperimentalEncodingApi::class)
actual fun decodeAvatar(base64: String): ImageBitmap? =
    runCatching {
        Image.makeFromEncoded(Base64.decode(base64)).toComposeImageBitmap()
    }.getOrNull()

actual fun decodeImageFromPath(path: String): ImageBitmap? =
    runCatching {
        Image.makeFromEncoded(readFileBytes(path)).toComposeImageBitmap()
    }.getOrNull()

private class AvatarPickerDelegate(
    private val onResult: (AvatarData?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    @OptIn(ExperimentalEncodingApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val result = image?.let { img ->
            runCatching {
                val png = UIImagePNGRepresentation(img.scaledToFit(AVATAR_MAX_DIM))
                    ?: return@runCatching null
                val tmp = NSTemporaryDirectory() + "avatar_${NSUUID().UUIDString}.png"
                if (!NSFileManager.defaultManager.createFileAtPath(tmp, png, null)) {
                    return@runCatching null
                }
                val bytes = readFileBytes(tmp)
                NSFileManager.defaultManager.removeItemAtPath(tmp, null)
                AvatarData(
                    base64 = Base64.encodeToString(bytes),
                    preview = Image.makeFromEncoded(bytes).toComposeImageBitmap(),
                )
            }.getOrNull()
        }
        onResult(result)
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onResult(null)
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

/** 等比缩放到最长边不超过 maxDim，本身已够小则原样返回 */
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
