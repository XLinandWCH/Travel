package com.xlin.travel.services

import com.xlin.travel.db.ModelApiKeys
import com.xlin.travel.models.ModelConfigResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 模型配置下发业务：按 token 鉴权后从 model_api_keys 读取一条配置返回给客户端。
 *
 * 安全说明：
 * - 必须携带有效登录 token，未登录用户拿不到密钥；
 * - 不下发 users 等其它表数据，也不接受客户端指定任意列；
 * - 表中 system_prompt / temperature 为空时按 null 返回，由客户端使用兜底默认值。
 */
object ModelConfigService {

    /** 查询结果：成功带回模型配置，失败带回 HTTP 状态码与提示 */
    sealed interface ConfigResult {
        data class Success(val config: ModelConfigResponse) : ConfigResult
        data class Failed(val status: Int, val message: String) : ConfigResult
    }

    /**
     * @param token 登录令牌，无效或过期返回 401
     * @param modelId 指定模型 id，为空则返回 id 最小的一条（默认模型）
     */
    fun getConfig(token: String, modelId: String?): ConfigResult {
        if (UserService.verifyToken(token) == null) {
            return ConfigResult.Failed(401, "登录已失效，请重新登录后再获取模型配置")
        }
        return try {
            transaction {
                val row = if (modelId.isNullOrBlank()) {
                    // 未指定模型：取 id 最小的一条作为默认模型（表内行数极少，在 Kotlin 侧比较即可）
                    ModelApiKeys.selectAll().toList().minByOrNull { it[ModelApiKeys.id] }
                } else {
                    ModelApiKeys.selectAll().where { ModelApiKeys.modelId eq modelId }.firstOrNull()
                }
                row?.let { ConfigResult.Success(it.toModelConfig()) }
                    ?: ConfigResult.Failed(404, "模型配置不存在：${modelId ?: "默认模型"}")
            }
        } catch (e: Exception) {
            ConfigResult.Failed(500, "获取模型配置失败：${e.message}")
        }
    }

    private fun ResultRow.toModelConfig() = ModelConfigResponse(
        modelId = this[ModelApiKeys.modelId],
        apiKey = this[ModelApiKeys.apiKey],
        baseUrl = this[ModelApiKeys.url],
        systemPrompt = this[ModelApiKeys.systemPrompt],
        temperature = this[ModelApiKeys.temperature],
    )
}
