package com.xlin.travel.agent

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
 * 服务端下发的模型配置（对应 server 的 ModelConfigResponse / model_api_keys 表）。
 * 替代此前硬编码在 AI_Agent.{android,ios,jvm}.kt 里的 API_KEY 与 baseUrl。
 */
@Serializable
data class RemoteModelConfig(
    val modelId: String,
    val apiKey: String,
    val baseUrl: String,
    val systemPrompt: String? = null,
    val temperature: Double? = null,
)

/** 与服务端 ModelConfigRequest 对应；modelId 为空表示取服务端默认模型 */
@Serializable
private data class ModelConfigRequest(val token: String, val modelId: String? = null)

/** 本地缓存的包装：记录拉取时间，超期的缓存不再用于离线兜底 */
@Serializable
private data class CachedModelConfig(val savedAt: Long, val config: RemoteModelConfig)

/**
 * 模型配置的获取与缓存：
 *
 * 1. 优先复用进程内配置（10 分钟内不再重复请求服务端）；
 * 2. 否则携带登录 token 调用 POST /api/model/config，成功后写入 app_setting 缓存；
 * 3. 网络 / 服务端异常时退回本地缓存（7 天内有效），保证离线仍能继续对话；
 * 4. 三者都拿不到则直接抛错，绝不回退到任何硬编码密钥。
 *
 * 安全说明：密钥只存在于服务端数据库与登录设备内存 / 本地缓存中，不再打进安装包；
 * 退出登录与注销会调用 [forget] 同步清掉内存与本地缓存。
 */
object ModelConfigStore {

    /** 进程内配置的复用时长 */
    private const val MEMORY_TTL_MILLIS = 10 * 60 * 1000L

    /** 本地缓存允许离线使用的最长时长 */
    private const val DISK_TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L

    /** 服务端未配置温度时的兜底值 */
    const val DEFAULT_TEMPERATURE = 0.2

    private val json = Json { ignoreUnknownKeys = true }

    private var memory: RemoteModelConfig? = null
    private var memoryAt = 0L

    /** 已加载的配置快照，供工具等非挂起入口使用；尚未加载时为 null */
    fun current(): RemoteModelConfig? = memory

    /** 丢弃内存副本（退出登录 / 注销），本地缓存由调用方清空 */
    fun forget() {
        memory = null
        memoryAt = 0L
    }

    /**
     * 取可用配置，失败时抛出带用户可读信息的异常（由 AssistantRound 展示到 AI 气泡）。
     */
    suspend fun ensure(repository: ChatRepository): RemoteModelConfig {
        memory?.takeIf { currentEpochMillis() - memoryAt < MEMORY_TTL_MILLIS }?.let { return it }

        val token = repository.getSetting(SettingKeys.AuthToken).orEmpty()
        if (token.isBlank()) error("未登录，无法获取模型配置，请先登录")

        val fetched = runCatching { ModelConfigApi.fetch(token) }
        val config = fetched.getOrNull()?.takeIf { it.apiKey.isNotBlank() }
        if (config != null) {
            val now = currentEpochMillis()
            memory = config
            memoryAt = now
            repository.setSetting(
                SettingKeys.ModelConfigCache,
                json.encodeToString(CachedModelConfig.serializer(), CachedModelConfig(now, config)),
            )
            return config
        }

        // 断网 / 服务端异常：退回上次成功的本地缓存
        val cached = repository.getSetting(SettingKeys.ModelConfigCache)
            ?.let { runCatching { json.decodeFromString(CachedModelConfig.serializer(), it) }.getOrNull() }
        cached?.takeIf { currentEpochMillis() - it.savedAt < DISK_TTL_MILLIS }?.let {
            memory = it.config
            memoryAt = it.savedAt   // 沿用原拉取时间计算有效期，避免缓存被无限续期
            return it.config
        }

        val reason = fetched.exceptionOrNull()?.message ?: "服务端未返回模型配置"
        error("模型配置获取失败（$reason），请检查网络或稍后重试")
    }
}

/** /api/model/config 接口客户端 */
private object ModelConfigApi {

    private val client by lazy { createHttpClient() }

    suspend fun fetch(token: String): RemoteModelConfig {
        val response = client.post("$SERVER_BASE_URL/api/model/config") {
            contentType(ContentType.Application.Json)
            setBody(ModelConfigRequest(token))
        }
        if (!response.status.isSuccess()) {
            // 优先展示服务端原因（如“登录已失效，请重新登录”），解析失败则回退状态码
            val message = runCatching { response.body<ApiError>().message }
                .getOrElse { "服务异常（${response.status.value}）" }
            error(message)
        }
        return response.body()
    }
}
