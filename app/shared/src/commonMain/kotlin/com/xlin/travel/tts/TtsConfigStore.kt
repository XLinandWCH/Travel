package com.xlin.travel.tts

import com.xlin.travel.agent.RemoteModelConfig
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.SettingKeys
import com.xlin.travel.db.currentEpochMillis
import com.xlin.travel.login.ApiError
import com.xlin.travel.login.SERVER_BASE_URL
import com.xlin.travel.login.createHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 语音合成配置的获取与缓存，与 [com.xlin.travel.inputbox.voice.AsrConfigStore] 同款四级降级：
 * 内存 10 分钟复用 → 在线拉取并缓存本地 → 断网退回 7 天内本地缓存 → 都失败抛错，绝不回退硬编码密钥。
 *
 * 配置存于服务端 voice_models 表（model_type=TTS），scope="voice" 按 [TTS_MODEL_ID] 拉取；
 * 服务端下发的 url 列即 DashScope WebSocket 推理入口（wss://.../api-ws/v1/inference），
 * apiKey 与 ASR 共用同一把百炼密钥。
 */
object TtsConfigStore {

    /** Qwen-Audio-TTS-Flash 模型 id（非机密，客户端与服务端约定的拉取键） */
    const val TTS_MODEL_ID = "qwen-audio-3.0-tts-flash"

    private const val MEMORY_TTL_MILLIS = 10 * 60 * 1000L
    private const val DISK_TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L

    private val json = Json { ignoreUnknownKeys = true }

    private var memory: RemoteModelConfig? = null
    private var memoryAt = 0L

    /** 丢弃内存副本（退出登录 / 注销），本地缓存由调用方清空 */
    fun forget() {
        memory = null
        memoryAt = 0L
    }

    suspend fun ensure(repository: ChatRepository): RemoteModelConfig {
        memory?.takeIf { currentEpochMillis() - memoryAt < MEMORY_TTL_MILLIS }?.let { return it }

        val token = repository.getSetting(SettingKeys.AuthToken).orEmpty()
        if (token.isBlank()) error("未登录，无法获取语音合成配置，请先登录")

        val fetched = runCatching { TtsConfigApi.fetch(token, TTS_MODEL_ID) }
        val config = fetched.getOrNull()?.takeIf { it.apiKey.isNotBlank() }
        if (config != null) {
            val now = currentEpochMillis()
            memory = config
            memoryAt = now
            repository.setSetting(
                SettingKeys.TtsConfigCache,
                json.encodeToString(CachedConfig.serializer(), CachedConfig(now, config)),
            )
            return config
        }

        val cached = repository.getSetting(SettingKeys.TtsConfigCache)
            ?.let { runCatching { json.decodeFromString(CachedConfig.serializer(), it) }.getOrNull() }
        cached?.takeIf { currentEpochMillis() - it.savedAt < DISK_TTL_MILLIS }?.let {
            memory = it.config
            memoryAt = it.savedAt
            return it.config
        }

        val reason = fetched.exceptionOrNull()?.message ?: "服务端未返回语音合成配置"
        error("语音合成配置获取失败（$reason），请检查网络或稍后重试")
    }
}

@Serializable
private data class CachedConfig(val savedAt: Long, val config: RemoteModelConfig)

/** /api/model/config 客户端，指定 modelId 拉取语音合成配置 */
private object TtsConfigApi {

    private val client by lazy { createHttpClient() }

    suspend fun fetch(token: String, modelId: String): RemoteModelConfig {
        val response = client.post("$SERVER_BASE_URL/api/model/config") {
            contentType(ContentType.Application.Json)
            setBody(TtsConfigRequest(token, modelId))
        }
        if (!response.status.isSuccess()) {
            val message = runCatching { response.body<ApiError>().message }
                .getOrElse { "服务异常（${response.status.value}）" }
            error(message)
        }
        return response.body()
    }
}

@Serializable
private data class TtsConfigRequest(val token: String, val modelId: String, val scope: String = "voice")
