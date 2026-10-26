package com.xlin.travel.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.refTo
import platform.posix.SEEK_END
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite
import platform.posix.rewind

/**
 * iOS 26 起 Foundation 被 Apple 用 Swift 重写，NSData 的 writeToFile / getBytes /
 * dataWithContentsOfFile 等不再是 ObjC 主 @interface 成员，Kotlin/Native interop
 * 无法解析（CI 实测 Unresolved reference）。文件读字节统一走 POSIX stdio。
 */
@OptIn(ExperimentalForeignApi::class)
internal fun readFileBytes(path: String): ByteArray {
    val file = fopen(path, "rb") ?: return ByteArray(0)
    try {
        fseek(file, 0L, SEEK_END)
        val size = ftell(file)
        rewind(file)
        if (size <= 0L) return ByteArray(0)
        val buffer = ByteArray(size.toInt())
        val read = fread(buffer.refTo(0), 1uL.convert(), size.convert(), file).toLong()
        return if (read == size) buffer else buffer.copyOf(read.coerceIn(0L, size).toInt())
    } finally {
        fclose(file)
    }
}

/**
 * iOS 26 同样不能用 NSData.writeToFile 落盘，语音合成的 mp3 统一走 POSIX stdio 写字节。
 * 写入成功返回 true；打开 / 写入失败返回 false（调用方据此放弃播放）。
 */
@OptIn(ExperimentalForeignApi::class)
internal fun writeFileBytes(path: String, bytes: ByteArray): Boolean {
    if (bytes.isEmpty()) return false
    val file = fopen(path, "wb") ?: return false
    try {
        val written = fwrite(bytes.refTo(0), 1uL.convert(), bytes.size.convert(), file).toLong()
        return written == bytes.size.toLong()
    } finally {
        fclose(file)
    }
}
