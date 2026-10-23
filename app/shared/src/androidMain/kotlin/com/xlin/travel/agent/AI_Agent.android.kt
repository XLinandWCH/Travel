// androidMain/kotlin/com/xlin/travel/agent/AI_Agent.android.kt
package com.xlin.travel.agent

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor

actual fun createChatExecutor(): PromptExecutor {
    val client = OpenAILLMClient(
        apiKey = "sk-d4ccf0c247f52781cf7f82ca3528c587246799952545d4d0d2e485174ee0a1ea",
        settings = OpenAIClientSettings(
            baseUrl = "https://discovery-api.intern-ai.org.cn/"
        ),
        httpClientFactory = KtorKoogHttpClient.Factory()
    )
    return MultiLLMPromptExecutor(client)
}
