package com.xlin.travel.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.util.UUID

/**
 * Android 适配：数据库放在应用私有的 databases 目录，
 * 驱动使用 androidx.sqlite 自带的 BundledSQLiteDriver（无需额外依赖系统 SQLite 版本）。
 */
@Composable
actual fun rememberChatRepository(): ChatRepository {
    val context = LocalContext.current.applicationContext
    val database = remember(context) {
        val dbFile = context.getDatabasePath(ChatDatabaseName)
        dbFile.parentFile?.mkdirs()
        ChatDatabase(
            driver = BundledSQLiteDriver(),
            path = dbFile.absolutePath,
        )
    }
    return remember(database) { SqliteChatRepository(database) }
}

internal actual fun currentEpochMillis(): Long = System.currentTimeMillis()

internal actual fun randomUuid(): String = UUID.randomUUID().toString()
