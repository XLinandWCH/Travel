package com.xlin.travel.inputbox.voice

/**
 * JVM（桌面）录音桩实现：桌面端仅用于本地测试，不接入麦克风，
 * [start] 恒返回 false，面板据此提示不可用，commonMain 的 expect 有 actual 可编译。
 */
actual class AudioRecorder actual constructor() {
    actual fun start(): Boolean = false
    actual fun level(): Float = 0f
    actual fun stop(): ByteArray = ByteArray(0)
    actual fun release() = Unit
}
