package com.xlin.travel.db

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

/**
 * 模型配置初始化：保证 model_api_keys 里有一条可直接下发的默认模型记录。
 *
 * 安全约定（与 DB_PASSWORD 同规则）：API_KEY 是机密，不写进源码，
 * 轮换 / 首次建库时通过 JVM 系统属性或环境变量注入：
 *   java -DMODEL_API_KEY=sk-xxx -jar server.jar
 *   export MODEL_API_KEY=sk-xxx && java -jar server.jar
 * 未注入时不会覆盖表里已有的密钥，因此线上库已有数据则无需注入。
 *
 * 模型 id / baseUrl / 系统提示词 / 温度同样以表数据为准：
 * 客户端不再硬编码这些信息，改表即可生效，无需重新打包客户端。
 * 这里的默认值只在表里对应列为空时补写，不覆盖手工编辑过的值。
 */
object ModelKeySeeder {

    private val log = LoggerFactory.getLogger(ModelKeySeeder::class.java)

    private const val DEFAULT_MODEL_ID = "Sage-A1.5"
    private const val DEFAULT_BASE_URL = "https://discovery-api.intern-ai.org.cn/"
    private const val DEFAULT_TEMPERATURE = 0.2

    /**
     * 语音识别（Fun-ASR-Flash）配置：与普通对话模型共用 model_api_keys 表，
     * 只是 model_id / url 指向 ASR 的多模态生成接口，客户端凭 modelId 单独拉取。
     * 密钥同样是机密，不写进源码：通过 ASR_API_KEY（-D 系统属性 > 环境变量）注入，
     * 未注入且表里无记录时跳过（不写空密钥）。url 存的是完整调用端点，客户端直接 POST。
     */
    private const val ASR_MODEL_ID = "fun-asr-flash-2026-06-15"
    private const val ASR_DEFAULT_URL =
        "https://cn-beijing.maas.qianwenaiapi.com/api/v1/services/aigc/multimodal-generation/generation"

    /**
     * beta 期便捷写入：用户不会在启动命令里注入 ASR_API_KEY，这里给一个默认密钥，
     * 启动时由 seedAsrModel() 以 INSERT-if-absent 方式写入 model_api_keys（绝不 DROP / 重建表、
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
        seedChatModel()
        seedAsrModel()
    }

    /** 语音识别模型记录：只在缺记录时写入，已有则保留（改表即可轮换密钥 / 换端点） */
    private fun seedAsrModel() {
        // 密钥优先级：-D/环境变量 ASR_API_KEY > 内置默认（beta 便捷写入），保证首次启动即落库
        val injectedKey = env("ASR_API_KEY").ifBlank { ASR_DEFAULT_API_KEY }
        val asrUrl = env("ASR_BASE_URL").ifBlank { ASR_DEFAULT_URL }
        transaction {
            val row = ModelApiKeys.selectAll().where { ModelApiKeys.modelId eq ASR_MODEL_ID }.firstOrNull()
            if (row == null) {
                ModelApiKeys.insert {
                    it[ModelApiKeys.modelId] = ASR_MODEL_ID
                    it[apiKey] = injectedKey
                    it[url] = asrUrl
                    // ASR 无系统提示词 / 温度概念，留 null
                }
                log.info("已写入语音识别配置默认记录：$ASR_MODEL_ID")
                return@transaction
            }
            // 已存在：仅在显式注入密钥（非内置默认）时才轮换，url 为空时补默认，其余保留
            val explicitKey = env("ASR_API_KEY")
            val needUpdate = explicitKey.isNotBlank() || row[ModelApiKeys.url].isBlank()
            if (!needUpdate) return@transaction
            ModelApiKeys.update({ ModelApiKeys.modelId eq ASR_MODEL_ID }) {
                if (explicitKey.isNotBlank()) it[apiKey] = explicitKey
                if (row[ModelApiKeys.url].isBlank()) it[url] = asrUrl
            }
            log.info("已更新语音识别配置：$ASR_MODEL_ID")
        }
    }

    private fun seedChatModel() {
        val modelId = env("MODEL_ID").ifBlank { DEFAULT_MODEL_ID }
        val baseUrl = env("MODEL_BASE_URL").ifBlank { DEFAULT_BASE_URL }
        val injectedKey = env("MODEL_API_KEY")

        transaction {
            val row = ModelApiKeys.selectAll().where { ModelApiKeys.modelId eq modelId }.firstOrNull()

            if (row == null) {
                // 新建记录必须有密钥，否则无法下发；提示后跳过，避免写入空密钥
                if (injectedKey.isBlank()) {
                    log.warn("model_api_keys 无 '$modelId' 记录且未注入 MODEL_API_KEY，跳过初始化")
                    return@transaction
                }
                ModelApiKeys.insert {
                    // 局部变量 modelId 与列同名，这里必须显式限定表列
                    it[ModelApiKeys.modelId] = modelId
                    it[apiKey] = injectedKey
                    it[url] = baseUrl
                    it[systemPrompt] = DEFAULT_SYSTEM_PROMPT
                    it[temperature] = DEFAULT_TEMPERATURE
                }
                log.info("已写入模型配置默认记录：$modelId")
                return@transaction
            }

            // 已存在：只补空列，密钥仅在显式注入时才轮换（表里改过的值一律保留）
            val needUpdate = injectedKey.isNotBlank() ||
                row[ModelApiKeys.url].isBlank() ||
                row[ModelApiKeys.systemPrompt].isNullOrBlank() ||
                row[ModelApiKeys.temperature] == null
            if (!needUpdate) return@transaction

            ModelApiKeys.update({ ModelApiKeys.modelId eq modelId }) {
                if (injectedKey.isNotBlank()) it[apiKey] = injectedKey
                if (row[ModelApiKeys.url].isBlank()) it[url] = baseUrl
                if (row[ModelApiKeys.systemPrompt].isNullOrBlank()) it[systemPrompt] = DEFAULT_SYSTEM_PROMPT
                if (row[ModelApiKeys.temperature] == null) it[temperature] = DEFAULT_TEMPERATURE
            }
            log.info("已补齐模型配置缺省字段：$modelId")
        }
    }
}
