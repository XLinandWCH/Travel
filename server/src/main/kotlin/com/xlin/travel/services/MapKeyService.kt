package com.xlin.travel.services

import com.xlin.travel.db.MapKeys
import com.xlin.travel.models.MapKeysResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 地图密钥下发业务：按 token 鉴权后从 map 表读取对应平台的一行返回给客户端。
 *
 * 安全说明：
 * - 必须携带有效登录 token，未登录用户拿不到密钥（与 ModelConfigService 一致）；
 * - 只返回请求平台那一行，不接受客户端指定任意列。
 */
object MapKeyService {

    /** 查询结果：成功带回密钥，失败带回 HTTP 状态码与提示 */
    sealed interface Result {
        data class Success(val keys: MapKeysResponse) : Result
        data class Failed(val status: Int, val message: String) : Result
    }

    /** @param token 登录令牌，无效或过期返回 401；@param platform ANDROID / IOS / WEB */
    fun getKeys(token: String, platform: String): Result {
        if (UserService.verifyToken(token) == null) {
            return Result.Failed(401, "登录已失效，请重新登录后再获取地图配置")
        }
        val normalized = platform.trim().uppercase()
        if (normalized !in SUPPORTED) {
            return Result.Failed(400, "不支持的平台：$platform（应为 ANDROID / IOS / WEB）")
        }
        return try {
            transaction {
                val row = MapKeys.selectAll().where { MapKeys.platform eq normalized }.firstOrNull()
                row?.let { Result.Success(it.toResponse()) }
                    ?: Result.Failed(404, "地图密钥未配置：$normalized")
            }
        } catch (e: Exception) {
            Result.Failed(500, "获取地图配置失败：${e.message}")
        }
    }

    private val SUPPORTED = setOf("ANDROID", "IOS", "WEB")

    private fun ResultRow.toResponse() = MapKeysResponse(
        platform = this[MapKeys.platform],
        apiKey = this[MapKeys.apiKey],
        securityCode = this[MapKeys.securityCode],
    )
}
