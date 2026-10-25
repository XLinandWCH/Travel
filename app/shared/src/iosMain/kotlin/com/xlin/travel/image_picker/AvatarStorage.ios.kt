package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.LocalUIViewController
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.*
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceTypePhotoLibrary
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.darwin.NSObject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * iOS 头像持久化：UIImagePickerController 从相册选图 → 缩放 → UIImagePNGRepresentation → base64。
 *
 * 注意：本文件仅在 iOS 目标编译；当前开发机（非 Apple 芯片）无法 native 编译 iOS，
 * 请在 Xcode 真机 / Mac 上首次构建时对照报错微调（同 ImagePicker.ios.kt）。
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberAvatarPicker(onResult: (AvatarData?) -> Unit): () -> Unit {
    val host = LocalUIViewController.current
    val delegate = remember(onResult) { AvatarPickerDelegate(onResult) }
    return {
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary
        picker.delegate = delegate
        host.presentViewController(picker, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun decodeAvatar(base64: String): ImageBitmap? =
    runCatching {
        NSData.create(base64EncodedString = base64, options = 0UL)
            ?.toByteArray()
            ?.let { Image.makeFromEncoded(it).toComposeImageBitmap() }
    }.getOrNull()

@OptIn(ExperimentalForeignApi::class)
actual fun decodeImageFromPath(path: String): ImageBitmap? =
    runCatching {
        NSData.create(contentsOfFile = path)
            ?.toByteArray()
            ?.let { Image.makeFromEncoded(it).toComposeImageBitmap() }
    }.getOrNull()

@OptIn(ExperimentalForeignApi::class)
private class AvatarPickerDelegate(
    private val onResult: (AvatarData?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val data = image?.let { UIImagePNGRepresentation(it.scaledToFit(AVATAR_MAX_DIM)) }
        val result = data?.let {
            val bytes = it.toByteArray()
            runCatching {
                AvatarData(
                    base64 = it.base64EncodedStringWithOptions(0UL),
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

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) getBytes(bytes.refTo(0), length)
    return bytes
}
