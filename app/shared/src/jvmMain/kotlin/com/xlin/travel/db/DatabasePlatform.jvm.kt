package com.xlin.travel.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.util.UUID

/**
 * JVM 适配：数据库放在用户目录的 .travel 文件夹，便于桌面端调试。
 */
@Composable
actual fun rememberChatDatabase(): ChatDatabase = remember {
    val directory = File(System.getProperty("user.home"), ".travel").apply { mkdirs() }
    ChatDatabase(
        driver = BundledSQLiteDriver(),
        path = File(directory, ChatDatabaseName).absolutePath,
    )
}

internal actual fun currentEpochMillis(): Long = System.currentTimeMillis()

internal actual fun randomUuid(): String = UUID.randomUUID().toString()
