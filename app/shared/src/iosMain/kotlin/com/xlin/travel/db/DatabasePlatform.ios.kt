@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xlin.travel.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.posix.gettimeofday
import platform.posix.timeval

/**
 * iOS 适配：数据库放在沙盒 Documents/travel 目录，
 * 驱动使用 androidx.sqlite 的 BundledSQLiteDriver。
 */
@Composable
actual fun rememberChatRepository(): ChatRepository {
    val database = remember { ChatDatabase(BundledSQLiteDriver(), databasePath()) }
    return remember(database) { SqliteChatRepository(database) }
}

/**
 * iOS 26 的 Foundation 已被 Swift 重写，NSDate 的 timeIntervalSince1970
 * 在当前 Kotlin/Native 绑定中不可解析，改用 POSIX gettimeofday 取 epoch 毫秒。
 */
internal actual fun currentEpochMillis(): Long = memScoped {
    val tv = alloc<timeval>()
    gettimeofday(tv.ptr, null)
    tv.tv_sec * 1000L + tv.tv_usec / 1000L
}

internal actual fun randomUuid(): String = NSUUID().UUIDString

private fun databasePath(): String {
    val documents = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String ?: error("无法获取 iOS Documents 目录")

    val directory = "$documents/travel"
    NSFileManager.defaultManager.createDirectoryAtPath(
        directory,
        true,
        null,
        null,
    )
    return "$directory/$ChatDatabaseName"
}
