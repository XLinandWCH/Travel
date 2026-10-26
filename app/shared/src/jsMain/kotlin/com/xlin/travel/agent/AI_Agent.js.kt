// jsMain/kotlin/com/xlin/travel/agent/AI_Agent.js.kt
package com.xlin.travel.agent

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import com.xlin.travel.login.SERVER_BASE_URL

/**
 * Web 端执行器：浏览器直连模型服务商会被 CORS 拦截，
 * 统一改走服务端代理（路径 /api/llm 下的 chat/completions，服务端按 body.model 查库转发并鉴权）。
 */
actual fun createChatExecutor(config: RemoteModelConfig): PromptExecutor {
    val client = OpenAILLMClient(
        apiKey = config.apiKey,
        settings = OpenAIClientSettings(
            baseUrl = "$SERVER_BASE_URL/api/llm"
        ),
        httpClientFactory = KtorKoogHttpClient.Factory()
    )
    return MultiLLMPromptExecutor(client)
}
