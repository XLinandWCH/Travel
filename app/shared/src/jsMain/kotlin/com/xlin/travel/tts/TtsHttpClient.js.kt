// jsMain/kotlin/com/xlin/travel/tts/TtsHttpClient.js.kt
package com.xlin.travel.tts

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets

/** Web：浏览器引擎 + WebSockets；保留与 JVM 相同的超时配置（当前 TTS 为桩，不走真实合成） */
internal actual fun createTtsHttpClient(): HttpClient = HttpClient(Js) {
    install(WebSockets)
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 5 * 60_000
        socketTimeoutMillis = 60_000
    }
}
