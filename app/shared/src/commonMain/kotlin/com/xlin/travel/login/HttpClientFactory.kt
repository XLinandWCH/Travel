package com.xlin.travel.login

import io.ktor.client.HttpClient

/**
 * 各平台 Ktor 客户端工厂（引擎不同：Android/JVM 用 CIO，iOS 用 Darwin）。
 * 与 clipboard / image_picker 相同的 expect / actual 组织方式。
 */
expect fun createHttpClient(): HttpClient
