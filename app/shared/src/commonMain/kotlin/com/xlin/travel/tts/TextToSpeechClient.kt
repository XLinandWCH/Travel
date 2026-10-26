package com.xlin.travel.tts

import com.xlin.travel.agent.RemoteModelConfig
import com.xlin.travel.db.randomUuid
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * 语音合成客户端：按 DashScope WebSocket Inference 协议（run-task 事件族）调用
 * Qwen-Audio-TTS-Flash，收齐 binary 音频帧后返回完整 mp3 字节。
 *
 * 协议流程（见 UI设计/Qwen3_TTS_Flah 使用说明文档.md 与百炼 Qwen-Audio-TTS WebSocket API）：
 * 1. 连接服务端下发的 url（wss://.../api-ws/v1/inference），握手头携带 Authorization: Bearer <apiKey>；
 * 2. 发送 run-task（task_group=audio / task=tts / function=SpeechSynthesizer），等待 task-started；
 * 3. 按序发送 continue-task 提交文本（单次上限 2 万字符，这里按 [CHUNK_CHARS] 分段），再 finish-task；
 * 4. 接收 binary 帧即音频流，收到 task-finished 结束；task-failed 携带错误信息则抛出。
 * 同一任务的 task_id 必须一致（UUID），本实现一次连接只跑一个任务，用完即关。
 */
object TextToSpeechClient {

    /** 需求文档指定的默认音色（longanfengyue · 温柔女声），暂不支持切换 */
    private const val VOICE = "longanfengyue"

    /** continue-task 单条文本分段上限，留出余量避免超限 */
    private const val CHUNK_CHARS = 2000

    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy { createTtsHttpClient() }

    /**
     * 把 [text] 合成为 mp3 字节。
     * @throws Exception 连接失败、鉴权失败、任务失败或协程取消时抛出，消息可直接展示给用户
     */
    suspend fun synthesize(config: RemoteModelConfig, text: String): ByteArray {
        require(text.isNotBlank()) { "没有可朗读的正文" }
        val taskId = randomUuid()
        val audio = mutableListOf<ByteArray>()

        client.webSocket(urlString = config.baseUrl, request = {
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
        }) {
            send(runTask(config.modelId, taskId))
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val event = frame.readText().serverEvent()
                        when (event?.first) {
                            // 任务确认开启后才能提交文本；finish-task 之后仍需继续收音频帧
                            "task-started" -> {
                                splitChunks(text).forEach { send(continueTask(taskId, it)) }
                                send(finishTask(taskId))
                            }
                            "task-finished" -> { close(); return@webSocket }
                            "task-failed" -> error(event.second ?: "语音合成失败，请稍后重试")
                        }
                    }
                    is Frame.Binary -> audio += frame.data
                    else -> Unit
                }
            }
        }

        if (audio.isEmpty()) error("未收到音频数据，请稍后重试")
        return merge(audio)
    }

    /** 解析服务端事件帧：返回 (header.event, header.error_message) */
    private fun String.serverEvent(): Pair<String, String?>? =
        runCatching {
            val header = json.parseToJsonElement(this).jsonObject["header"]?.jsonObject ?: return@runCatching null
            val event = header["event"]?.jsonPrimitive?.contentOrNull
                ?: header["action"]?.jsonPrimitive?.contentOrNull
                ?: return@runCatching null
            event to header["error_message"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()

    private fun runTask(modelId: String, taskId: String): Frame = Frame.Text(
        json.encodeToString(JsonObject.serializer(), buildJsonObject {
            putJsonObject("header") {
                put("action", "run-task")
                put("task_id", taskId)
                put("streaming", "duplex")
            }
            putJsonObject("payload") {
                put("task_group", "audio")
                put("task", "tts")
                put("function", "SpeechSynthesizer")
                put("model", modelId)
                putJsonObject("parameters") {
                    put("text_type", "PlainText")
                    put("voice", VOICE)
                    put("format", "mp3")
                    put("sample_rate", 22050)
                    put("volume", 50)
                    put("rate", 1)
                    put("pitch", 1)
                }
                putJsonObject("input") { }
            }
        }),
    )

    private fun continueTask(taskId: String, text: String): Frame = Frame.Text(
        json.encodeToString(JsonObject.serializer(), buildJsonObject {
            putJsonObject("header") {
                put("action", "continue-task")
                put("task_id", taskId)
                put("streaming", "duplex")
            }
            putJsonObject("payload") {
                putJsonObject("input") { put("text", text) }
            }
        }),
    )

    private fun finishTask(taskId: String): Frame = Frame.Text(
        json.encodeToString(JsonObject.serializer(), buildJsonObject {
            putJsonObject("header") {
                put("action", "finish-task")
                put("task_id", taskId)
                put("streaming", "duplex")
            }
            putJsonObject("payload") {
                putJsonObject("input") { }
            }
        }),
    )

    /** 优先按句末标点切段，保证每段不超过 [CHUNK_CHARS]，服务端会自动分句合成 */
    private fun splitChunks(text: String): List<String> {
        if (text.length <= CHUNK_CHARS) return listOf(text)
        val chunks = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < text.length) {
            i++
            if (i - start < CHUNK_CHARS) continue
            // 达到上限：往回找最近的句末标点收尾，找不到就硬切
            val tail = text.substring(start, i)
            val cut = tail.lastIndexOfAny(charArrayOf('。', '！', '？', '\n', '.', '!', '?'))
            if (cut >= 0) {
                chunks += tail.substring(0, cut + 1)
                start += cut + 1
            } else {
                chunks += tail
                start = i
            }
        }
        if (start < text.length) chunks += text.substring(start)
        return chunks
    }

    private fun merge(chunks: List<ByteArray>): ByteArray {
        val out = ByteArray(chunks.sumOf { it.size })
        var pos = 0
        chunks.forEach { it.copyInto(out, pos); pos += it.size }
        return out
    }
}
