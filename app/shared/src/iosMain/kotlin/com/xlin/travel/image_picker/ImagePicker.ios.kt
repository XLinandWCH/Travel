package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.LocalUIViewController
import com.xlin.travel.io.readFileBytes
import org.jetbrains.skia.Image
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

/**
 * iOS 图片选择：使用 UIImagePickerController 从相册选取。
 * 需要 Info.plist 声明 NSPhotoLibraryUsageDescription（见 iosApp/Info.plist）。
 *
 * PNG 数据写入临时目录，回调其绝对路径供 media_analysis 工具读取。
 *
 * iOS 26 / Kotlin 2.4 interop 注意（CI 实测）：
 * - LocalUIViewController 位于 androidx.compose.ui.interop 包；
 * - 不设置 sourceType：其默认值就是 PhotoLibrary
 *   （UIImagePickerControllerSourceType* 枚举常量在当前绑定中不可解析）；
 * - Foundation 已被 Swift 重写，NSData 的 writeToFile / getBytes 无法互操作，
 *   写文件走 NSFileManager.createFileAtPath，读回字节走 POSIX（readFileBytes）。
 */
@Composable
actual fun rememberImagePicker(onResult: (bitmap: ImageBitmap?, path: String?) -> Unit): () -> Unit {
    val host = LocalUIViewController.current
    val delegate = remember(onResult) { ImagePickerDelegate(onResult) }
    return {
        val picker = UIImagePickerController()
        picker.delegate = delegate
        host.presentViewController(picker, animated = true, completion = null)
    }
}

private class ImagePickerDelegate(
    private val onResult: (bitmap: ImageBitmap?, path: String?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        // PNG 写入临时目录供工具读取，再读回字节解码出预览位图
        val result = image?.let { img ->
            runCatching {
                val data = UIImagePNGRepresentation(img) ?: return@runCatching null
                val file = NSTemporaryDirectory() + "img_${NSUUID().UUIDString}.png"
                if (!NSFileManager.defaultManager.createFileAtPath(file, data, null)) {
                    return@runCatching null
                }
                val bytes = readFileBytes(file)
                val bitmap = runCatching {
                    Image.makeFromEncoded(bytes).toComposeImageBitmap()
                }.getOrNull()
                bitmap to file
            }.getOrNull()
        }
        onResult(result?.first, result?.second)
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onResult(null, null)
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}
