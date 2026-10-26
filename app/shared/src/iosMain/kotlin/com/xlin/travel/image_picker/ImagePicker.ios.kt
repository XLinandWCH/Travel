package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.LocalUIViewController
import com.xlin.travel.io.readFileBytes
import com.xlin.travel.image_processor.IosImageProcessor
import com.xlin.travel.image_processor.NormalizedImage
import org.jetbrains.skia.Image
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

/**
 * iOS 图片选择：使用 UIImagePickerController 从相册选取。
 * 需要 Info.plist 声明 NSPhotoLibraryUsageDescription（见 iosApp/Info.plist）。
 *
 * 选中后先经 IosImageProcessor 规范化（png 保留、其余统一转 JPEG 并压缩尺寸）
 * 写入临时目录，回调其绝对路径供 media_analysis 工具读取。
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
        // 规范化为 AI 可接收的 jpeg / png 后写入临时目录，再读回字节解码出预览位图
        val result = image?.let { img ->
            runCatching {
                val processed: NormalizedImage = IosImageProcessor.normalizeImage(img)
                    ?: return@runCatching null
                val file = NSTemporaryDirectory() + "img_${NSUUID().UUIDString}.${processed.extension}"
                if (!NSFileManager.defaultManager.createFileAtPath(file, processed.data, null)) {
                    return@runCatching null
                }
                val bitmap = runCatching {
                    Image.makeFromEncoded(readFileBytes(file)).toComposeImageBitmap()
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
