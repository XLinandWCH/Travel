package com.xlin.travel.db

import androidx.compose.runtime.Composable

/**
 * 创建当前平台的会话历史仓库：
 * - Android / iOS / JVM：SqliteChatRepository，本地 SQLite 文件
 *   （数据库实现与 DAO 在 dbSqliteMain 中间源集，Web 目标不参与编译）
 * - Web（Kotlin/JS）：WebChatRepository，内存 + localStorage
 *   （浏览器没有可同步调用的 SQLite）
 */
@Composable
expect fun rememberChatRepository(): ChatRepository

/** 当前毫秒时间戳 */
internal expect fun currentEpochMillis(): Long

/** 生成一个 UUID 字符串作为主键 */
internal expect fun randomUuid(): String
