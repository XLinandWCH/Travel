// jsMain/kotlin/com/xlin/travel/inputbox/voice/AudioRecorder.js.kt
package com.xlin.travel.inputbox.voice

/**
 * Web 录音桩实现：浏览器 getUserMedia 仅在 https / localhost 安全上下文可用，
 * 当前部署为 http://IP:端口，无法采集 PCM。与 JVM 相同处理：start 恒为 false，
 * 语音面板据此提示不可用。
 */
actual class AudioRecorder actual constructor() {
    actual fun start(): Boolean = false
    actual fun level(): Float = 0f
    actual fun stop(): ByteArray = ByteArray(0)
    actual fun release() = Unit
}
