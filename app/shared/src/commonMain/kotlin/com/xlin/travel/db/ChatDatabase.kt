package com.xlin.travel.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * SQLite 连接持有者。
 *
 * 整个应用共用一个连接，通过 [Mutex] 串行化访问；所有数据库操作都切到
 * [Dispatchers.Default]，避免阻塞 Compose 的主线程。
 */
class ChatDatabase internal constructor(
    private val driver: SQLiteDriver,
    private val path: String,
) {

    private val mutex = Mutex()
    private var connection: SQLiteConnection? = null

    /** 在同一个连接上执行一段数据库操作 */
    internal suspend fun <T> withConnection(block: (SQLiteConnection) -> T): T =
        withContext(Dispatchers.Default) {
            mutex.withLock { block(connection()) }
        }

    /** 在一个事务中执行数据库操作，失败时自动回滚 */
    internal suspend fun <T> withTransaction(block: (SQLiteConnection) -> T): T =
        withConnection { connection ->
            connection.execSQL("BEGIN IMMEDIATE")
            try {
                val result = block(connection)
                connection.execSQL("COMMIT")
                result
            } catch (t: Throwable) {
                runCatching { connection.execSQL("ROLLBACK") }
                throw t
            }
        }

    private fun connection(): SQLiteConnection =
        connection ?: openConnection().also { connection = it }

    private fun openConnection(): SQLiteConnection {
        val opened = driver.open(path)
        try {
            opened.execSQL("PRAGMA foreign_keys = ON")
            ChatSchema.statements.forEach { opened.execSQL(it) }
            migrateAddMessageImagePath(opened)
            return opened
        } catch (t: Throwable) {
            opened.close()
            throw t
        }
    }

    /**
     * 安全增量迁移：给已存在的旧库 message 表补上 image_path 列。
     * 全新安装时 CREATE TABLE 已带该列，这里检测到已存在就跳过，不会报错。
     */
    private fun migrateAddMessageImagePath(connection: SQLiteConnection) {
        val hasColumn = connection.withStatement("PRAGMA table_info(message)") { statement ->
            var found = false
            while (statement.step()) {
                if (statement.getText(1) == "image_path") found = true
            }
            found
        }
        if (!hasColumn) connection.execSQL("ALTER TABLE message ADD COLUMN image_path TEXT")
    }
}

/** 准备一条语句并在使用后确保关闭 */
internal inline fun <T> SQLiteConnection.withStatement(
    sql: String,
    block: (SQLiteStatement) -> T,
): T {
    val statement = prepare(sql)
    try {
        return block(statement)
    } finally {
        statement.close()
    }
}

internal fun SQLiteStatement.getTextOrNull(index: Int): String? =
    if (isNull(index)) null else getText(index)

internal fun SQLiteStatement.getIntOrNull(index: Int): Int? =
    if (isNull(index)) null else getInt(index)
