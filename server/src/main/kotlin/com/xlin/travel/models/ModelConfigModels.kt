package com.xlin.travel.models

import kotlinx.serialization.Serializable

/**
 * /api/model 接口的请求 / 响应体（kotlinx.serialization）。
 * 客户端不再硬编码 API_KEY 与 baseUrl，登录后凭 token 拉取这份配置。
 */

/**
 * 模型配置拉取请求：token 为登录令牌（复用 /api/auth 签发的 HMAC token）。
 * scope 区分拆表：“llm”（默认，取自 llm_models）/ “voice”（取自 voice_models）。
 * modelId 为空时：llm 返回 id 最小的默认模型；voice 必传 modelId。
 */
@Serializable
data class ModelConfigRequest(
    val token: String,
    val modelId: String? = null,
    val scope: String? = null,
)

/** 模型配置响应：llm 来自 llm_models，voice 来自 voice_models；type 仅语音模型返回（ASR/TTS） */
@Serializable
data class ModelConfigResponse(
    val modelId: String,
    val apiKey: String,
    val baseUrl: String,
    val systemPrompt: String? = null,
    val temperature: Double? = null,
    val type: String? = null,
)
