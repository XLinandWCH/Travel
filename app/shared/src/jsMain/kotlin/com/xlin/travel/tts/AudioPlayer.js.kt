// jsMain/kotlin/com/xlin/travel/tts/AudioPlayer.js.kt
package com.xlin.travel.tts

/**
 * Web 播放桩实现：TTS 依赖 DashScope WebSocket（run-task 协议），
 * 浏览器无法为 WS 握手附加 Authorization 头，故网页版暂不提供语音合成播放。
 * 与 JVM 桩一致：play 不发声，isPlaying 恒 false（上层轮询立即视为播完）。
 */
actual class AudioPlayer actual constructor() {
    actual fun play(bytes: ByteArray) = Unit
    actual fun isPlaying(): Boolean = false
    actual fun stop() = Unit
}
