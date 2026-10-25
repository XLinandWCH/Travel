package com.xlin.travel.models

import kotlinx.serialization.Serializable

/**
 * /api/model 接口的请求 / 响应体（kotlinx.serialization）。
 * 客户端不再硬编码 API_KEY 与 baseUrl，登录后凭 token 拉取这份配置。
 */

/**
 * 模型配置拉取请求：token 为登录令牌（复用 /api/auth 签发的 HMAC token）。
 * modelId 为空时服务端返回 id 最小的一条记录，即默认模型。
 */
@Serializable
data class ModelConfigRequest(
    val token: String,
    val modelId: String? = null,
)

/** 模型配置响应：与 model_api_keys 表列一一对应 */
@Serializable
data class ModelConfigResponse(
    val modelId: String,
    val apiKey: String,
    val baseUrl: String,
    val systemPrompt: String? = null,
    val temperature: Double? = null,
)
