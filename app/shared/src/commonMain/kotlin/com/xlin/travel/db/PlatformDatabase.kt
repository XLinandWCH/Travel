package com.xlin.travel.db

import androidx.compose.runtime.Composable

/**
 * 创建当前平台的数据库实例：
 * - Android：应用私有 databases 目录
 * - iOS：Documents/travel 目录
 * - JVM：用户目录下的 .travel 目录
 *
 * Android / iOS 的驱动与路径适配分别在 androidMain / iosMain 中实现。
 */
@Composable
expect fun rememberChatDatabase(): ChatDatabase

/** 当前毫秒时间戳 */
internal expect fun currentEpochMillis(): Long

/** 生成一个 UUID 字符串作为主键 */
internal expect fun randomUuid(): String
