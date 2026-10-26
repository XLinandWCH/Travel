package com.xlin.travel.db

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

/**
 * 模型配置初始化：保证 llm_models / voice_models 各有一条可直接下发的默认记录。
 *
 * 表已按用途从 model_api_keys 拆分为 llm_models（对话大模型）与 voice_models（语音模型，
 * 含必填类型列 ASR/TTS）。本 Seeder：
 *   1) 先把历史 model_api_keys 里的行迁移到对应新表（INSERT-if-absent，绝不 DROP / 删除旧表旧数据）；
 *   2) 再补齐新表里缺省的默认记录。
 *
 * 安全约定：API_KEY 是机密，正式环境通过 JVM 系统属性或环境变量注入轮换：
 *   java -DMODEL_API_KEY=sk-xxx -DASR_API_KEY=sk-xxx -jar server.jar
 *   export MODEL_API_KEY=sk-xxx ASR_API_KEY=sk-xxx && java -jar server.jar
 * beta 期用户不会手动注入，故 ASR 给一个内置默认密钥（见 ASR_DEFAULT_API_KEY）首次启动自动落库；
 * 环境变量 / -D 优先级更高。旧表已有数据一律保留，不覆盖已手工编辑过的记录。
 */
object ModelKeySeeder {

    private val log = LoggerFactory.getLogger(ModelKeySeeder::class.java)

    private const val DEFAULT_MODEL_ID = "Sage-A1.5"
    private const val DEFAULT_BASE_URL = "https://discovery-api.intern-ai.org.cn/"
    private const val DEFAULT_TEMPERATURE = 0.2

    /**
     * 语音识别（Fun-ASR-Flash）配置：落在 voice_models 表，类型列固定 ASR。
     * 密钥同样是机密，正式环境通过 ASR_API_KEY（-D > 环境变量）注入；
     * url 存的是完整调用端点，客户端直接 POST。
     */
    private const val ASR_MODEL_ID = "fun-asr-flash-2026-06-15"
    private const val ASR_DEFAULT_URL =
        "https://cn-beijing.maas.qianwenaiapi.com/api/v1/services/aigc/multimodal-generation/generation"

    /** voice_models.type 必填取值：语音识别 */
    private const val VOICE_TYPE_ASR = "ASR"

    /** voice_models.type 必填取值：语音合成 */
    private const val VOICE_TYPE_TTS = "TTS"

    /**
     * 语音合成（Qwen-Audio-TTS-Flash）配置：同样落在 voice_models，类型列 TTS。
     * 与 ASR 共用同一把百炼 / DashScope 密钥，缺记录时直接复制 ASR 行的 api_key；
     * url 存 WebSocket 推理入口，客户端按 run-task 协议直连合成。
     */
    private const val TTS_MODEL_ID = "qwen-audio-3.0-tts-flash"
    private const val TTS_DEFAULT_URL = "wss://cn-beijing.maas.qianwenaiapi.com/api-ws/v1/inference"

    /**
     * beta 期便捷写入：用户不会在启动命令里注入 ASR_API_KEY，这里给一个默认密钥，
     * 启动时由 seedAsrModel() 以 INSERT-if-absent 方式写入 voice_models（绝不 DROP / 重建表、
     * 也不覆盖已有记录）。环境变量 / -D ASR_API_KEY 优先级更高，正式环境请改用注入轮换。
     */
    private const val ASR_DEFAULT_API_KEY =
        "sk-ws-H.PDEYIEY.RYBO.MEYCIQDX8HnLWNBePTO56b59vQfApxvLQ_DQl3HF0CZN1TGrowIhAOOHMy6xMR35YeYTqVVDNeaSJL9tcNyLBwwWMu7F43m9"

    /** 旅行助手默认系统提示词，仅在表中未配置时写入 */
    private val DEFAULT_SYSTEM_PROMPT = """
        你是一个名为 Claude 的 AI 助手，由 Anthropic 制作。（如果有人问起你是什么模型你就如实回答就行，没人问AI模型就不要说这个

        你现在运行在一个旅行助手产品里（主要），用户问旅行相关问题时：
        - 给出可执行的行程、地点、交通、预算建议
        - 需要工具调用时按工具结果回答
        - 保持 Claude 的克制、温和、专业语气

        图片 / 文件识别：
        - 当用户消息里附带了图片（会给出本地绝对路径）或你需要识别图片 / 文档内容时，
          调用 media_analysis 工具，instruction 填写用户诉求，imagePath 填写给出的图片路径

        工具调用铁律（避免重复调用）：
        - 同一个工具针对相同参数最多只调用一次
        - 一旦收到工具返回的结果，就必须直接基于该结果给出最终回答，绝对不能再重复上一次的调用！牢记！
    """.trimIndent()

    /** 参数优先级：-D 系统属性 > 环境变量 > 空串（空串表示本次不写入该字段） */
    private fun env(key: String): String =
        System.getProperty(key)?.takeIf { it.isNotBlank() }
            ?: System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: ""

    fun seed() {
        // model_api_keys 已不再创建：旧库存在则一次性迁移，新库无此表时忽略失败（不影响下面默认记录写入）
        runCatching { migrateLegacyRows() }
            .onFailure { log.info("跳过 model_api_keys 迁移（旧表可能不存在或已迁移）：${it.message}") }
        seedChatModel()
        seedAsrModel()
        seedTtsModel()
    }

    /**
     * 一次性迁移：把历史 model_api_keys 的行搬到拆分后的新表，仅在目标缺同 id 记录时写入。
     * ASR 记录（[ASR_MODEL_ID]）进 voice_models 并补 type=ASR，其余进 llm_models。
     * 全程 INSERT-if-absent，绝不 DROP / 删除旧表或旧数据。
     */
    private fun migrateLegacyRows() {
        transaction {
            ModelApiKeys.selectAll().forEach { legacy ->
                val mid = legacy[ModelApiKeys.modelId]
                if (mid == ASR_MODEL_ID) {
                    if (VoiceModels.selectAll().where { VoiceModels.modelId eq mid }.firstOrNull() == null) {
                        VoiceModels.insert {
                            it[VoiceModels.modelId] = mid
                            it[apiKey] = legacy[ModelApiKeys.apiKey]
                            it[url] = legacy[ModelApiKeys.url]
                            it[type] = VOICE_TYPE_ASR
                        }
                        log.info("已从 model_api_keys 迁移语音记录到 voice_models：$mid")
                    }
                } else if (LlmModels.selectAll().where { LlmModels.modelId eq mid }.firstOrNull() == null) {
                    LlmModels.insert {
                        it[LlmModels.modelId] = mid
                        it[apiKey] = legacy[ModelApiKeys.apiKey]
                        it[url] = legacy[ModelApiKeys.url]
                        it[systemPrompt] = legacy[ModelApiKeys.systemPrompt]
                        it[temperature] = legacy[ModelApiKeys.temperature]
                    }
                    log.info("已从 model_api_keys 迁移对话记录到 llm_models：$mid")
                }
            }
        }
    }

    /** 语音模型记录：写入 voice_models（type=ASR）；缺记录才补，已有则仅在显式注入时轮换 */
    private fun seedAsrModel() {
        // 密钥优先级：-D/环境变量 ASR_API_KEY > 内置默认（beta 便捷写入），保证首次启动即落库
        val injectedKey = env("ASR_API_KEY").ifBlank { ASR_DEFAULT_API_KEY }
        val asrUrl = env("ASR_BASE_URL").ifBlank { ASR_DEFAULT_URL }
        transaction {
            val row = VoiceModels.selectAll().where { VoiceModels.modelId eq ASR_MODEL_ID }.firstOrNull()
            if (row == null) {
                VoiceModels.insert {
                    it[VoiceModels.modelId] = ASR_MODEL_ID
                    it[apiKey] = injectedKey
                    it[url] = asrUrl
                    it[type] = VOICE_TYPE_ASR
                }
                log.info("已写入语音模型默认记录：$ASR_MODEL_ID（type=$VOICE_TYPE_ASR）")
                return@transaction
            }
            // 已存在：仅在显式注入密钥（非内置默认）时才轮换，url 为空时补默认，其余保留
            val explicitKey = env("ASR_API_KEY")
            val needUpdate = explicitKey.isNotBlank() || row[VoiceModels.url].isBlank()
            if (!needUpdate) return@transaction
            VoiceModels.update({ VoiceModels.modelId eq ASR_MODEL_ID }) {
                if (explicitKey.isNotBlank()) it[apiKey] = explicitKey
                if (row[VoiceModels.url].isBlank()) it[url] = asrUrl
            }
            log.info("已更新语音模型配置：$ASR_MODEL_ID")
        }
    }

    /**
     * 语音合成记录：写入 voice_models（type=TTS）；INSERT-if-absent，绝不覆盖已有记录。
     * 密钥优先级：-D/环境变量 TTS_API_KEY > 库内 ASR 行的 api_key > 显式注入的 ASR_API_KEY > 内置默认，
     * 与 Qwen-Audio-TTS 和 Fun-ASR 共用一把密钥的约定一致。
     */
    private fun seedTtsModel() {
        transaction {
            val existing = VoiceModels.selectAll().where { VoiceModels.modelId eq TTS_MODEL_ID }.firstOrNull()
            if (existing != null) {
                // 已存在：仅在显式注入 TTS_API_KEY 时轮换，其余一律保留
                val explicitKey = env("TTS_API_KEY")
                if (explicitKey.isNotBlank()) {
                    VoiceModels.update({ VoiceModels.modelId eq TTS_MODEL_ID }) { it[apiKey] = explicitKey }
                    log.info("已轮换语音合成密钥：$TTS_MODEL_ID")
                }
                return@transaction
            }
            // 缺记录：复用 ASR 行的密钥（与需求文档约定共用 api_key）
            val asrKey = VoiceModels.selectAll().where { VoiceModels.modelId eq ASR_MODEL_ID }.firstOrNull()
                ?.get(VoiceModels.apiKey)
                ?: env("ASR_API_KEY").ifBlank { ASR_DEFAULT_API_KEY }
            VoiceModels.insert {
                it[VoiceModels.modelId] = TTS_MODEL_ID
                it[apiKey] = asrKey
                it[url] = env("TTS_BASE_URL").ifBlank { TTS_DEFAULT_URL }
                it[type] = VOICE_TYPE_TTS
            }
            log.info("已写入语音合成默认记录：$TTS_MODEL_ID（type=$VOICE_TYPE_TTS）")
        }
    }

    /** 对话模型记录：写入 llm_models；缺记录且注入了密钥才建，已有则只补空列 */
    private fun seedChatModel() {
        val modelId = env("MODEL_ID").ifBlank { DEFAULT_MODEL_ID }
        val baseUrl = env("MODEL_BASE_URL").ifBlank { DEFAULT_BASE_URL }
        val injectedKey = env("MODEL_API_KEY")

        transaction {
            val row = LlmModels.selectAll().where { LlmModels.modelId eq modelId }.firstOrNull()

            if (row == null) {
                // 新建记录必须有密钥，否则无法下发；提示后跳过，避免写入空密钥
                if (injectedKey.isBlank()) {
                    log.warn("llm_models 无 '$modelId' 记录且未注入 MODEL_API_KEY，跳过初始化")
                    return@transaction
                }
                LlmModels.insert {
                    // 局部变量 modelId 与列同名，这里必须显式限定表列
                    it[LlmModels.modelId] = modelId
                    it[apiKey] = injectedKey
                    it[url] = baseUrl
                    it[systemPrompt] = DEFAULT_SYSTEM_PROMPT
                    it[temperature] = DEFAULT_TEMPERATURE
                }
                log.info("已写入 LLM 模型默认记录：$modelId")
                return@transaction
            }

            // 已存在：只补空列，密钥仅在显式注入时才轮换（表里改过的值一律保留）
            val needUpdate = injectedKey.isNotBlank() ||
                row[LlmModels.url].isBlank() ||
                row[LlmModels.systemPrompt].isNullOrBlank() ||
                row[LlmModels.temperature] == null
            if (!needUpdate) return@transaction

            LlmModels.update({ LlmModels.modelId eq modelId }) {
                if (injectedKey.isNotBlank()) it[apiKey] = injectedKey
                if (row[LlmModels.url].isBlank()) it[url] = baseUrl
                if (row[LlmModels.systemPrompt].isNullOrBlank()) it[systemPrompt] = DEFAULT_SYSTEM_PROMPT
                if (row[LlmModels.temperature] == null) it[temperature] = DEFAULT_TEMPERATURE
            }
            log.info("已补齐 LLM 模型配置缺省字段：$modelId")
        }
    }
}
