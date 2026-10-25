package com.xlin.travel.inputbox.voice

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
 * 语音识别配置的获取与缓存，沿用 [com.xlin.travel.agent.ModelConfigStore] 的四级降级策略：
 * 内存 10 分钟复用 → 在线拉取并缓存本地 → 断网退回 7 天内本地缓存 → 都失败抛错，绝不回退硬编码密钥。
 *
 * 与普通对话模型复用同一张服务端 model_api_keys 表，只是按 [ASR_MODEL_ID] 单独拉取；
 * 服务端下发的 url 列即为 Fun-ASR 的完整调用端点，apiKey 为百炼 / DashScope 密钥。
 */
object AsrConfigStore {

    /** Fun-ASR-Flash 模型 id（非机密，客户端与服务端约定的拉取键） */
    const val ASR_MODEL_ID = "fun-asr-flash-2026-06-15"

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
        if (token.isBlank()) error("未登录，无法获取语音识别配置，请先登录")

        val fetched = runCatching { AsrConfigApi.fetch(token, ASR_MODEL_ID) }
        val config = fetched.getOrNull()?.takeIf { it.apiKey.isNotBlank() }
        if (config != null) {
            val now = currentEpochMillis()
            memory = config
            memoryAt = now
            repository.setSetting(
                SettingKeys.AsrConfigCache,
                json.encodeToString(CachedConfig.serializer(), CachedConfig(now, config)),
            )
            return config
        }

        val cached = repository.getSetting(SettingKeys.AsrConfigCache)
            ?.let { runCatching { json.decodeFromString(CachedConfig.serializer(), it) }.getOrNull() }
        cached?.takeIf { currentEpochMillis() - it.savedAt < DISK_TTL_MILLIS }?.let {
            memory = it.config
            memoryAt = it.savedAt
            return it.config
        }

        val reason = fetched.exceptionOrNull()?.message ?: "服务端未返回语音识别配置"
        error("语音识别配置获取失败（$reason），请检查网络或稍后重试")
    }
}

@Serializable
private data class CachedConfig(val savedAt: Long, val config: RemoteModelConfig)

/** /api/model/config 客户端，指定 modelId 拉取语音识别配置 */
private object AsrConfigApi {

    private val client by lazy { createHttpClient() }

    suspend fun fetch(token: String, modelId: String): RemoteModelConfig {
        val response = client.post("$SERVER_BASE_URL/api/model/config") {
            contentType(ContentType.Application.Json)
            setBody(AsrConfigRequest(token, modelId))
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
private data class AsrConfigRequest(val token: String, val modelId: String)
