package com.xlin.travel.tts

/**
 * 平台音频播放器（expect / actual），与 [com.xlin.travel.inputbox.voice.AudioRecorder] 对称。
 *
 * 播放 [TextToSpeechClient] 合成的 mp3 字节：
 * - Android 用 MediaPlayer 落临时文件播放；
 * - iOS 用 AVAudioPlayer 落临时文件播放；
 * - JVM（桌面测试环境）用 Java Sound 直接喂内存流。
 *
 * 生命周期：[play] 开始播放 → 轮询 [isPlaying] 判断是否仍在播 → [stop] 提前停止并释放。
 * 同一时刻只播一段；[play] 会先停掉上一段。所有方法可重复、跨线程安全调用。
 */
expect class AudioPlayer() {
    /** 开始播放 mp3 字节；自动停掉上一段。解码 / 写入失败时静默忽略（[isPlaying] 保持 false） */
    fun play(bytes: ByteArray)

    /** 当前是否仍在播放，供上层轮询到播完自动复位图标 */
    fun isPlaying(): Boolean

    /** 停止播放并释放资源（含临时文件），可重复调用 */
    fun stop()
}
