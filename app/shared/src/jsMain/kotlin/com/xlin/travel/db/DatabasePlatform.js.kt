// jsMain/kotlin/com/xlin/travel/db/DatabasePlatform.js.kt
package com.xlin.travel.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.random.Random

/**
 * Web 适配：浏览器没有可同步调用的 SQLite（androidx.sqlite 的 Web 驱动是异步 Worker），
 * 用内存列表 + localStorage 快照持久化实现 [ChatRepository]，刷新页面后历史仍在。
 */
@Composable
actual fun rememberChatRepository(): ChatRepository = remember { WebChatRepository() }

internal actual fun currentEpochMillis(): Long {
    val now: Double = js("Date.now()")
    return now.toLong()
}

/** 浏览器端没有 java.util.UUID / NSUUID，用随机字节拼 v4 格式 */
internal actual fun randomUuid(): String {
    val bytes = Random.nextBytes(16)
    bytes[6] = (bytes[6].toInt() and 0x0f or 0x40).toByte() // version 4
    bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte() // variant 10
    return buildString(36) {
        val hex = "0123456789abcdef"
        bytes.forEachIndexed { index, b ->
            append(hex[(b.toInt() shr 4) and 0x0f])
            append(hex[b.toInt() and 0x0f])
            if (index == 3 || index == 5 || index == 7 || index == 9) append('-')
        }
    }
}
