package com.xlin.travel.tts

import io.ktor.client.HttpClient

/**
 * TTS 专用 HTTP 客户端（带 WebSockets 插件）。
 *
 * 登录 / 模型配置用的 [com.xlin.travel.login.createHttpClient] 未安装 WebSockets，
 * 且其 20 秒请求超时对长文本合成太短，这里单独构造：超时放宽到分钟级，
 * 引擎与各平台 createHttpClient 保持一致（CIO / Darwin）。
 */
internal expect fun createTtsHttpClient(): HttpClient
