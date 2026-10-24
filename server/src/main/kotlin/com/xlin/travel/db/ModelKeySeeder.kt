package com.xlin.travel.db

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * 模型密钥初始化：把客户端 AI_Agent.{android,ios,jvm}.kt 里硬编码的模型信息
 * 落到 model_api_keys 表，后续切换模型 / 由服务端下发密钥时只需改这里或表数据。
 *
 * 仅写入数据，不开放接口（按要求：除登录服务外其它服务暂不连接）。
 * TODO 安全：API_KEY 建议迁移到环境变量 / 密钥管理，表中明文存储仅适合开发阶段。
 */
object ModelKeySeeder {

    private data class SeedModel(
        val modelId: String,
        val apiKey: String,
        val url: String,
    )

    private val seeds = listOf(
        SeedModel(
            modelId = "Sage-A1.5",
            apiKey = "sk-d4ccf0c247f52781cf7f82ca3528c587246799952545d4d0d2e485174ee0a1ea",
            url = "https://discovery-api.intern-ai.org.cn/",
        ),
    )

    fun seed() {
        transaction {
            seeds.forEach { seed ->
                val exists = ModelApiKeys.selectAll().where { ModelApiKeys.modelId eq seed.modelId }.firstOrNull()
                if (exists == null) {
                    ModelApiKeys.insert {
                        it[modelId] = seed.modelId
                        it[apiKey] = seed.apiKey
                        it[url] = seed.url
                    }
                } else {
                    // 客户端密钥轮换后重启服务端即可同步最新值
                    ModelApiKeys.update({ ModelApiKeys.modelId eq seed.modelId }) {
                        it[apiKey] = seed.apiKey
                        it[url] = seed.url
                    }
                }
            }
        }
    }
}
