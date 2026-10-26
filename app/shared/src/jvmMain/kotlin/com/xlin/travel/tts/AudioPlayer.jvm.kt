package com.xlin.travel.tts

import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * JVM（桌面 / 测试宿主）播放：Java Sound 只解 WAV/PCM，mp3 需要 SPI 解码器插件。
 * 桌面端不是发布目标，解码失败时静默忽略（[isPlaying] 保持 false，上层轮询立即视为播完），
 * 不影响 Android / iOS 真机链路。
 */
actual class AudioPlayer actual constructor() {

    private var clip: Clip? = null

    actual fun play(bytes: ByteArray) {
        stop()
        if (bytes.isEmpty()) return
        runCatching {
            val input = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
            clip = AudioSystem.getClip().apply {
                open(input)
                start()
            }
        }.onFailure { clip?.close(); clip = null }
    }

    actual fun isPlaying(): Boolean = clip?.isRunning == true

    actual fun stop() {
        clip?.let {
            runCatching { it.stop(); it.close() }
        }
        clip = null
    }
}
