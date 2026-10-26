package com.xlin.travel.tts

import android.media.MediaPlayer
import java.io.File

/**
 * Android 播放：mp3 字节写入缓存目录临时文件，MediaPlayer 播放；
 * 新的 [play] 会先停旧文件，自然播完或 [stop] 后删除临时文件。
 *
 * MediaPlayer 的 setOnCompletionListener 回调不改变 [isPlaying]（内部代理播放器状态），
 * 上层通过轮询 [isPlaying] 感知播放结束，无需额外回调。
 */
actual class AudioPlayer actual constructor() {

    private var player: MediaPlayer? = null
    private var tempFile: File? = null

    actual fun play(bytes: ByteArray) {
        releaseInternal()
        if (bytes.isEmpty()) return
        val file = try {
            File.createTempFile("tts_", ".mp3").apply { writeBytes(bytes) }
        } catch (_: Exception) {
            return
        }
        tempFile = file
        player = try {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { deleteTemp() }
                prepare()
                start()
            }
        } catch (_: Exception) {
            runCatching { file.delete() }
            tempFile = null
            null
        }
    }

    actual fun isPlaying(): Boolean = player?.isPlaying == true

    actual fun stop() {
        releaseInternal()
    }

    private fun releaseInternal() {
        player?.let {
            runCatching { if (it.isPlaying) it.stop() }
            it.release()
        }
        player = null
        deleteTemp()
    }

    private fun deleteTemp() {
        tempFile?.let { runCatching { it.delete() } }
        tempFile = null
    }
}
