package com.xlin.travel.services

import com.xlin.travel.db.LlmModels
import com.xlin.travel.db.VoiceModels
import com.xlin.travel.models.ModelConfigResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 模型配置下发业务：按 token 鉴权后从拆分后的表读取一条配置返回给客户端。
 *
 * scope="voice" → voice_models（返回携带 type，如 ASR/TTS）；
 * 其余（含 null/"llm"）→ llm_models（modelId 为空时取 id 最小为默认模型）。
 *
 * 安全说明：
 * - 必须携带有效登录 token，未登录用户拿不到密钥；
 * - 不下发 users 等其它表数据，也不接受客户端指定任意列。
 */
object ModelConfigService {

    /** 查询结果：成功带回模型配置，失败带回 HTTP 状态码与提示 */
    sealed interface ConfigResult {
        data class Success(val config: ModelConfigResponse) : ConfigResult
        data class Failed(val status: Int, val message: String) : ConfigResult
    }

    /**
     * @param token 登录令牌，无效或过期返回 401
     * @param modelId 指定模型 id；llm 下为空则返回 id 最小的一条（默认模型）
     * @param scope "voice" 取 voice_models，其余取 llm_models
     */
    fun getConfig(token: String, modelId: String?, scope: String?): ConfigResult {
        if (UserService.verifyToken(token) == null) {
            return ConfigResult.Failed(401, "登录已失效，请重新登录后再获取模型配置")
        }
        return try {
            transaction {
                if (scope == "voice") {
                    if (modelId.isNullOrBlank()) {
                        return@transaction ConfigResult.Failed(400, "获取语音模型配置必须指定 modelId")
                    }
                    val row = VoiceModels.selectAll().where { VoiceModels.modelId eq modelId }.firstOrNull()
                    row?.let { ConfigResult.Success(it.toVoiceConfig()) }
                        ?: ConfigResult.Failed(404, "语音模型配置不存在：$modelId")
                } else {
                    val row = if (modelId.isNullOrBlank()) {
                        // 未指定模型：取 id 最小的一条作为默认模型（表内行数极少，在 Kotlin 侧比较即可）
                        LlmModels.selectAll().toList().minByOrNull { it[LlmModels.id] }
                    } else {
                        LlmModels.selectAll().where { LlmModels.modelId eq modelId }.firstOrNull()
                    }
                    row?.let { ConfigResult.Success(it.toLlmConfig()) }
                        ?: ConfigResult.Failed(404, "模型配置不存在：${modelId ?: "默认模型"}")
                }
            }
        } catch (e: Exception) {
            ConfigResult.Failed(500, "获取模型配置失败：${e.message}")
        }
    }

    private fun ResultRow.toLlmConfig() = ModelConfigResponse(
        modelId = this[LlmModels.modelId],
        apiKey = this[LlmModels.apiKey],
        baseUrl = this[LlmModels.url],
        systemPrompt = this[LlmModels.systemPrompt],
        temperature = this[LlmModels.temperature],
    )

    private fun ResultRow.toVoiceConfig() = ModelConfigResponse(
        modelId = this[VoiceModels.modelId],
        apiKey = this[VoiceModels.apiKey],
        baseUrl = this[VoiceModels.url],
        type = this[VoiceModels.type],
    )
}
