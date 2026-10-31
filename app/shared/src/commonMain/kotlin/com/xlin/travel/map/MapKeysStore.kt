package com.xlin.travel.map

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
 * 服务端下发的高德地图密钥（对应 server 的 MapKeysResponse / map 表）。
 * [securityCode] 仅 Android 有值（SDK 安全码），其余平台为 null。
 */
@Serializable
data class RemoteMapKeys(
    val platform: String,
    val apiKey: String,
    val securityCode: String? = null,
)

/** 与服务端 MapKeysRequest 对应 */
@Serializable
private data class MapKeysRequest(val token: String, val platform: String)

/** 本地缓存包装：记录拉取时间，超期缓存不再用于离线兜底 */
@Serializable
private data class CachedMapKeys(val savedAt: Long, val keys: RemoteMapKeys)

/**
 * 地图密钥的获取与缓存，与 ModelConfigStore 同一套约定：
 *
 * 1. 优先复用进程内配置（10 分钟内不重复请求服务端）；
 * 2. 否则携带登录 token 调用 POST /api/map/keys，成功后写入 app_setting 缓存；
 * 3. 网络 / 服务端异常时退回本地缓存（7 天内有效）；
 * 4. 三者都拿不到则抛错，绝不回退到硬编码密钥。
 *
 * 按平台分开缓存：设备平台行（ANDROID / IOS）的 Key 是 SDK 类型，供定位与后续 SDK 初始化；
 * 静态地图等 Web 服务（[fetchFor] 传 "WEB"）必须用 Web服务类型的 WEB 行 Key，
 * 用 SDK Key 调 restapi 会返回 USERKEY_PLAT_NOMATCH。
 */
object MapKeysStore {

    private const val MEMORY_TTL_MILLIS = 10 * 60 * 1000L
    private const val DISK_TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L

    private val json = Json { ignoreUnknownKeys = true }

    /** 平台 → 密钥 / 拉取时间（设备平台与 WEB 各自一份，互不覆盖） */
    private val memory = mutableMapOf<String, RemoteMapKeys>()
    private val memoryAt = mutableMapOf<String, Long>()

    /** 已加载的某平台密钥快照；尚未加载时为 null */
    fun current(platform: String = mapPlatformKey): RemoteMapKeys? = memory[platform]

    /** 丢弃内存副本（退出登录 / 注销），本地缓存由调用方清空 */
    fun forget() {
        memory.clear()
        memoryAt.clear()
    }

    /** 取指定平台可用密钥（默认设备平台），失败时抛出带用户可读信息的异常 */
    suspend fun ensure(repository: ChatRepository, platform: String = mapPlatformKey): RemoteMapKeys {
        memory[platform]?.takeIf {
            currentEpochMillis() - (memoryAt[platform] ?: 0L) < MEMORY_TTL_MILLIS
        }?.let { return it }

        val token = repository.getSetting(SettingKeys.AuthToken).orEmpty()
        if (token.isBlank()) error("未登录，无法获取地图配置，请先登录")

        val cacheKey = diskKeyFor(platform)
        val fetched = runCatching { MapKeysApi.fetch(token, platform) }
        val keys = fetched.getOrNull()?.takeIf { it.apiKey.isNotBlank() }
        if (keys != null) {
            val now = currentEpochMillis()
            memory[platform] = keys
            memoryAt[platform] = now
            repository.setSetting(
                cacheKey,
                json.encodeToString(CachedMapKeys.serializer(), CachedMapKeys(now, keys)),
            )
            return keys
        }

        // 断网 / 服务端异常：退回上次成功的本地缓存
        val cached = repository.getSetting(cacheKey)
            ?.let { runCatching { json.decodeFromString(CachedMapKeys.serializer(), it) }.getOrNull() }
        cached?.takeIf { currentEpochMillis() - it.savedAt < DISK_TTL_MILLIS }?.let {
            memory[platform] = it.keys
            memoryAt[platform] = it.savedAt
            return it.keys
        }

        val reason = fetched.exceptionOrNull()?.message ?: "服务端未返回地图配置"
        error("地图配置获取失败（$reason），请检查网络或稍后重试")
    }

    /** 各平台的磁盘缓存键：WEB 行单独一份，不与设备平台缓存互相顶掉 */
    private fun diskKeyFor(platform: String): String =
        if (platform == "WEB") SettingKeys.MapWebConfigCache else SettingKeys.MapConfigCache
}

/** /api/map/keys 接口客户端 */
private object MapKeysApi {

    private val client by lazy { createHttpClient() }

    suspend fun fetch(token: String, platform: String): RemoteMapKeys {
        val response = client.post("$SERVER_BASE_URL/api/map/keys") {
            contentType(ContentType.Application.Json)
            setBody(MapKeysRequest(token, platform))
        }
        if (!response.status.isSuccess()) {
            val message = runCatching { response.body<ApiError>().message }
                .getOrElse { "服务异常（${response.status.value}）" }
            error(message)
        }
        return response.body()
    }
}
