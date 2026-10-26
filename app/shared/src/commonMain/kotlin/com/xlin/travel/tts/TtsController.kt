package com.xlin.travel.tts

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xlin.travel.chat_screen.ChatMessage
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.SettingKeys
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * 朗读总控：驱动消息动作条的「朗读 / 停止」图标与顶栏的「自动朗读」开关。
 *
 * 两处入口的区别（见需求文档）：
 * - [toggleRead] 用户手动点某条回答的朗读按钮，只读这一条；
 * - [autoRead] 顶栏开关打开后，每轮流式结束自动朗读最新正文（不含思考内容）。
 *
 * 同一时刻只有一段在读：新的朗读会取消旧任务（含合成中的 WebSocket 会话）；
 * 合成完成后交给平台 [AudioPlayer] 播放，轮询播放状态直到播完自动复位图标。
 * 调用约定：挂起方法需由界面层在某个协程作用域 launch，取消 / 异常由调用方处理。
 */
@Stable
class TtsController(private val repository: ChatRepository) {

    /** 正在朗读（合成或播放中）的消息 id；驱动动作条图标 start_reading ↔ reading */
    var readingMessageId: Long? by mutableStateOf(null)
        private set

    /** 顶栏「自动朗读最新回复」开关；图标 close_reading（关）↔ start_reading（开） */
    var autoReadEnabled: Boolean by mutableStateOf(false)
        private set

    private var lastAutoReadId: Long? = null
    private var job: Job? = null
    private val player = AudioPlayer()

    fun isReading(message: ChatMessage): Boolean = readingMessageId == message.id

    /** 启动时恢复上次的开关偏好 */
    suspend fun loadPreferences() {
        autoReadEnabled = repository.getSetting(SettingKeys.TtsAutoRead) == "1"
    }

    /** 顶栏点击：切换开关并持久化；关闭时顺手停掉正在读的内容 */
    suspend fun toggleAutoRead() {
        autoReadEnabled = !autoReadEnabled
        repository.setSetting(SettingKeys.TtsAutoRead, if (autoReadEnabled) "1" else "0")
        if (!autoReadEnabled) stop()
    }

    /** 手动朗读入口：同一条正在读 → 停止；否则读这一条 */
    suspend fun toggleRead(message: ChatMessage) {
        if (readingMessageId == message.id) {
            stop()
            return
        }
        read(message)
    }

    /** 自动朗读入口：开关关闭、消息为空、或这条已经自动读过时不动作 */
    suspend fun autoRead(message: ChatMessage?) {
        if (message == null || !autoReadEnabled) return
        if (message.id == lastAutoReadId || message.content.isBlank()) return
        lastAutoReadId = message.id
        read(message)
    }

    /** 停止朗读并复位图标（新一轮流式开始、关闭自动朗读开关时也会被调用） */
    fun stop() {
        job?.cancel()
        job = null
        readingMessageId = null
        player.stop()
    }

    /** 切换会话时调用：连同「已自动读过」记录一起清空（消息 id 每个会话都从 0 重新编号） */
    fun resetForConversation() {
        lastAutoReadId = null
        stop()
    }

    private suspend fun read(message: ChatMessage) {
        val speakable = message.content.toSpeakableText()
        if (speakable.isBlank()) error("这条回复没有可朗读的正文")
        // 先取消上一段（不清 reading 状态，紧接着就要点亮新的）
        job?.cancel()
        job = null
        player.stop()
        readingMessageId = message.id
        job = coroutineContext[Job]
        try {
            val config = TtsConfigStore.ensure(repository)
            val audio = TextToSpeechClient.synthesize(config, speakable)
            player.play(audio)
            // 起播有毫秒级延迟，先等一小段再轮询，避免刚 play 就被误判播完
            delay(StartWarmupMillis)
            while (player.isPlaying()) delay(PollMillis)
        } finally {
            player.stop()
            if (readingMessageId == message.id) readingMessageId = null
        }
    }

    private companion object {
        const val StartWarmupMillis = 400L
        const val PollMillis = 250L
    }
}
