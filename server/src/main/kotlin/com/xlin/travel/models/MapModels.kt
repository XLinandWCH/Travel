package com.xlin.travel.models

import kotlinx.serialization.Serializable

/**
 * /api/map 接口的请求 / 响应体（kotlinx.serialization）。
 * 客户端不硬编码高德 Key，登录后按平台凭 token 拉取 map 表中对应行。
 */

/** 地图密钥拉取请求：token 为登录令牌；platform 取值 ANDROID / IOS / WEB（大小写不敏感） */
@Serializable
data class MapKeysRequest(
    val token: String,
    val platform: String,
)

/** 地图密钥响应：securityCode 仅 Android 有值（SDK 安全码），其余平台为 null */
@Serializable
data class MapKeysResponse(
    val platform: String,
    val apiKey: String,
    val securityCode: String? = null,
)
