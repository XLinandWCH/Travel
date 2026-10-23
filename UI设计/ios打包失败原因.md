这个 Job 失败的直接原因是：iOS 端的 Kotlin/Native 编译失败，导致后面的 Xcode 打包阶段也跟着失败。

核心日志是：
- Task :app:shared:compileKotlinIosArm64 FAILED

失败点主要有两类：

1. expect/actual 不匹配
   很多 iOS 实现文件里定义了 actual 方法/对象，但在公共层没有对应的 expect 声明，比如：
- Platform.ios.kt
- DatabasePlatform.ios.kt
- AI_Agent.ios.kt
- ImagePicker.ios.kt
- AppBackHandler.ios.kt

日志里明确出现了：
- “has no corresponding 'expect' declaration”

这说明 Shared 模块和 iOS 模块的 API 定义已经失衡了，Kotlin Multiplatform 编译器无法对它们建立正确的桥接。

2. iOS 代码本身有语法/类型错误
   例如：
- Unresolved reference 'App'
- Unresolved reference 'ChatDatabase'
- Unresolved reference 'LocalUIViewController'
- Syntax error: Expecting a '>'
- Cannot extend an object
- 'name' overrides nothing

尤其是 ImagePicker.ios.kt 里有明显的 iOS/Swift/Kotlin interop 语法问题，像：
- “Function 'imagePickerController' without a body must be abstract”
- “Syntax error: Expecting a '>'”
- “Unresolved reference 'didFinishPickingMediaWithInfo'”

这些都表明这个文件的 Objective-C / UIKit 回调签名或 Kotlin 语法有问题，编译器根本没法通过。

为什么后面又报 Xcode 构建失败？
因为 iOS 端的 Kotlin framework 没有生成成功，所以后面的：
- PhaseScriptExecution Compile Kotlin Framework
- Building project iosApp...
  也随之失败。

也就是说，这不是“随机 CI 环境问题”，而是前面的 Kotlin 编译错误导致的连锁失败。

一句话总结：
这个 job 失败的根因是：app/shared 的 iOS 目标编译失败，主要是 expect/actual 声明不一致，以及 iOS 实现文件存在语法和类型错误，尤其在 Platform.ios.kt、DatabasePlatform.ios.kt 和 ImagePicker.ios.kt。

如果你愿意，我可以继续帮你定位：
- 哪些具体文件需要修
- 这些 expect/actual 应该怎么补齐
- ImagePicker.ios.kt 里最可能的语法错误在哪一行

## 定位输出
我定位看了，结论很明确：这不是 GitHub Actions 环境问题，而是代码里 iOS / KMP 相关实现本身有明显编译错误，并且它们会串起来导致整个构建失败。

最直接的证据在这些文件里：

1) `MainViewController.kt` 直接调用了不存在的 `App`
```kotlin name=app/shared/src/iosMain/kotlin/com/xlin/travel/MainViewController.kt url=https://github.com/XLinandWCH/Travel/blob/ebadf366c41398a3eec3e9986923b67736826d6f/app/shared/src/iosMain/kotlin/com/xlin/travel/MainViewController.kt#L1-L5
package com.xlin.travel

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }
```

这和日志中的：
- `Unresolved reference 'App'`
  完全对应。`App()` 这个符号在 iOS 目标下没有定义或没被正确导入。

2) `Platform.ios.kt` 也是明显问题：`actual`/`expect` 关系和 iOS API 注解都不完整
```kotlin name=app/shared/src/iosMain/kotlin/com/xlin/travel/Platform.ios.kt url=https://github.com/XLinandWCH/Travel/blob/ebadf366c41398a3eec3e9986923b67736826d6f/app/shared/src/iosMain/kotlin/com/xlin/travel/Platform.ios.kt#L1-L9
package com.xlin.travel

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()
```

对应的 common 声明是：
```kotlin name=app/shared/src/commonMain/kotlin/com/xlin/travel/Platform.kt url=https://github.com/XLinandWCH/Travel/blob/ebadf366c41398a3eec3e9986923b67736826d6f/app/shared/src/commonMain/kotlin/com/xlin/travel/Platform.kt#L1-L7
package com.xlin.travel

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
```

这里的错误不是“单纯没导入”，而是 iOS 实现层和公共 expect 定义之间有断裂；日志里也明确报了：
- `actual fun getPlatform(): Platform has no corresponding 'expect' declaration`
- `This declaration needs opt-in...`
- `'name' overrides nothing`

另外，`UIDevice.currentDevice` 这类 Foundation/UIKit API 在 Kotlin/Native 里通常需要 `@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)`，这个文件里没有加，这也是一部分原因。

3) `ImagePicker.ios.kt` 有明显的 API 签名/语法问题，直接造成大量编译错误
```kotlin name=app/shared/src/iosMain/kotlin/com/xlin/travel/image_picker/ImagePicker.ios.kt url=https://github.com/XLinandWCH/Travel/blob/ebadf366c41398a3eec3e9986923b67736826d6f/app/shared/src/iosMain/kotlin/com/xlin/travel/image_picker/ImagePicker.ios.kt#L1-L66
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
...
}
```

这里的问题非常集中：
- `LocalUIViewController` 这类 Compose iOS API 可能不在这个依赖版本里，导致 `Unresolved reference`
- `UIImagePickerControllerDelegateProtocol` 的重写方法签名写法不对，日志里明确报：
    - `Syntax error: Expecting a '>'`
    - `Function 'imagePickerController' without a body must be abstract`
    - `didFinishPickingMediaWithInfo` 相关重写签名错误
- `NSData.toByteArray()` 里 `getBytes(bytes.refTo(0), length)` 也有 Kotlin/Native interop 细节问题

这和日志中大量：
- `Unresolved reference 'LocalUIViewController'`
- `Unresolved reference 'UIImagePickerControllerSourceTypePhotoLibrary'`
- `Syntax error...`
- `Unresolved reference 'didFinishPickingMediaWithInfo'`
  是对上的。

4) `DatabasePlatform.ios.kt` 也有同类问题
```kotlin name=app/shared/src/iosMain/kotlin/com/xlin/travel/db/DatabasePlatform.ios.kt url=https://github.com/XLinandWCH/Travel/blob/ebadf366c41398a3eec3e9986923b67736826d6f/app/shared/src/iosMain/kotlin/com/xlin/travel/db/DatabasePlatform.ios.kt#L1-L44
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xlin.travel.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask

@Composable
actual fun rememberChatDatabase(): ChatDatabase = remember {
    ChatDatabase(BundledSQLiteDriver(), databasePath())
}

internal actual fun currentEpochMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

internal actual fun randomUuid(): String = NSUUID().UUIDString

private fun databasePath(): String {
    ...
    return "$directory/$ChatDatabaseName"
}
```

日志里直接报：
- `Unresolved reference 'ChatDatabase'`
- `Unresolved reference 'ChatDatabaseName'`
- `actual fun ... has no corresponding 'expect' declaration`

这说明数据库平台层的 common/expect 声明和 iOS actual 实现也没有对齐。

结论：
这次失败的根因不是单一的一行，而是“iOS 目标的 KMP 实现层整体不完整/不匹配”，具体集中在：

- `MainViewController.kt`: 调用不存在的 `App()`
- `Platform.ios.kt`: iOS 实现和 common expect 不一致，且缺少必要的 `@OptIn`
- `ImagePicker.ios.kt`: UIKit/Kotlin/Native 代理方法签名和 API 绑定有明显错误
- `DatabasePlatform.ios.kt`: common DB expect 和 iOS actual 也未对齐，很多类型和名字都 unresolved

所以，CI 上的失败本质上是：
- iOS Kotlin/Native 编译失败
- 接着 Xcode PhaseScript 也因为 Kotlin framework 生成失败而报错

如果你愿意，我下一步可以直接帮你把这些“最关键的 3 个文件”修成能编译的版本，按 iOS KMP 的正确写法改。