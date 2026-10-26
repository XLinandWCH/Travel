package com.xlin.travel.tts

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets

/** iOS：Darwin 引擎 + WebSockets；长文本合成预留 5 分钟请求超时 */
internal actual fun createTtsHttpClient(): HttpClient = HttpClient(Darwin) {
    install(WebSockets)
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 5 * 60_000
        socketTimeoutMillis = 60_000
    }
}
