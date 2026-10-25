package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.LocalUIViewController
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
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
 * PNG 数据写入临时目录，回调其绝对路径供 media_analysis 工具读取。
 *
 * 注意：本文件仅在 iOS 目标编译。当前开发机（非 Apple 芯片）无法 native 编译 iOS，
 * 若 interop 方法签名在真机上有出入，请对照 Xcode 报错微调 didFinishPickingMediaWithInfo 的重写名。
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePicker(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit {
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
    private val onResult: (bitmap: ImageBitmap?, path: String?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *?>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val data = image?.let { UIImagePNGRepresentation(it) }
        val bitmap = data?.toByteArray()?.let {
            runCatching { Image.makeFromEncoded(it).toComposeImageBitmap() }.getOrNull()
        }
        // 写入临时目录，得到工具可读的本地绝对路径
        val path = data?.let {
            runCatching {
                val file = NSTemporaryDirectory() + "img_${NSUUID().UUIDString}.png"
                if (it.writeToFile(file, atomically = true)) file else null
            }.getOrNull()
        }
        onResult(bitmap, path)
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onResult(null, null)
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
