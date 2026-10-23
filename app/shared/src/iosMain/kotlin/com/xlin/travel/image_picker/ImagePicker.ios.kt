package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.LocalUIViewController
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceTypePhotoLibrary
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

/**
 * iOS 图片选择：使用 UIImagePickerController 从相册选取。
 * 需要 Info.plist 声明 NSPhotoLibraryUsageDescription（见 iosApp/Info.plist）。
 *
 * 注意：本文件仅在 iOS 目标编译。当前开发机（非 Apple 芯片）无法 native 编译 iOS，
 * 若 interop 方法签名在真机上有出入，请对照 Xcode 报错微调 didFinishPickingMediaWithInfo 的重写名。
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePicker(onResult: (ImageBitmap?) -> Unit): () -> Unit {
    val host = LocalUIViewController.current
    val delegate = remember(onResult) { ImagePickerDelegate(onResult) }
    return {
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary
        picker.delegate = delegate
        host.presentViewController(picker, animated = true, completion = null)
    }
}

private class ImagePickerDelegate(
    private val onResult: (ImageBitmap?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *?>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val data = image?.let { UIImagePNGRepresentation(it) }
        onResult(data?.toByteArray()?.let { runCatching { Image.makeFromEncoded(it).toComposeImageBitmap() }.getOrNull() })
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onResult(null)
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) getBytes(bytes.refTo(0), length)
    return bytes
}
