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

/**
 * iOS 适配：数据库放在沙盒 Documents/travel 目录，
 * 驱动使用 androidx.sqlite 的 BundledSQLiteDriver。
 */
@Composable
actual fun rememberChatDatabase(): ChatDatabase = remember {
    ChatDatabase(BundledSQLiteDriver(), databasePath())
}

internal actual fun currentEpochMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

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
