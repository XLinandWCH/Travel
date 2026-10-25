// androidMain/kotlin/com/xlin/travel/agent/AI_Agent.android.kt
package com.xlin.travel.agent

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor

// 平台 actual 只负责选 http 引擎；apiKey / baseUrl 由 ModelConfigStore 从服务端拉取，不写死在代码里
actual fun createChatExecutor(config: RemoteModelConfig): PromptExecutor {
    val client = OpenAILLMClient(
        apiKey = config.apiKey,
        settings = OpenAIClientSettings(
            baseUrl = config.baseUrl
        ),
        httpClientFactory = KtorKoogHttpClient.Factory()
    )
    return MultiLLMPromptExecutor(client)
}
