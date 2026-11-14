package com.xlin.travel.videoexplain

import com.xlin.travel.agent.aiErrorText
import com.xlin.travel.agent.withServerExpiryNotice
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.inputbox.voice.AsrConfigStore
import com.xlin.travel.inputbox.voice.AudioRecorder
import com.xlin.travel.inputbox.voice.SpeechToTextClient
import com.xlin.travel.inputbox.voice.VoiceInputState
import com.xlin.travel.inputbox.voice.VoicePhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 录音上限：60 秒；最后 10 秒显示倒计时并到点自动结束（与输入框语音模式一致） */
private const val MAX_DURATION_MS = 60_000L
private const val COUNTDOWN_START_MS = 10_000L

/** 说完之后的静音判定：响度低于阀值连续约 1.5 秒就自动结束，不用手动点完成 */
private const val QUIET_LEVEL = 0.12f
private const val QUIET_TICKS_BEFORE_FINISH = 25

/**
 * 视频讲解页的语音驱动：录音 → ASR 识别 → 回调文本。
 *
 * 复用输入框同一套 AudioRecorder（16kHz 单声道 WAV）与 SpeechToTextClient
 * （Fun-ASR-Flash，配置同样从 [AsrConfigStore] 按登录态拉取），
 * 只是识别结果不发给聊天，而是交给页面判断要不要生成讲解视频。
 */
internal class ExplainVoiceRunner(
    private val voice: VoiceInputState,
    private val recorder: AudioRecorder,
    private val scope: CoroutineScope,
    private val repository: ChatRepository,
    private val onRecognized: (String) -> Unit,
    private val onExit: () -> Unit,
) {

    /** 权限通过后开始录音，并在协程里轮询真实响度驱动波形与末尾倒计时 */
    fun start() {
        if (!recorder.start()) {
            voice.phase = VoicePhase.Starting
            voice.error = "无法访问麦克风，请检查权限后重试"
            return
        }
        voice.error = null
        voice.resetLevels()
        voice.phase = VoicePhase.Recording
        scope.launch {
            var elapsed = 0L
            // 静音自动结束：先说过话（spoke）之后，连续安静 1.5 秒就当作一句说完
            var spoke = false
            var quietTicks = 0
            while (voice.phase == VoicePhase.Recording) {
                val level = recorder.level()
                voice.pushLevel(level)
                if (level > QUIET_LEVEL) {
                    spoke = true
                    quietTicks = 0
                } else if (spoke) {
                    quietTicks++
                }
                delay(60)
                elapsed += 60
                if (spoke && quietTicks >= QUIET_TICKS_BEFORE_FINISH) break
                val remaining = MAX_DURATION_MS - elapsed
                if (remaining <= 0L) break
                if (remaining <= COUNTDOWN_START_MS) {
                    voice.showCountdown = true
                    voice.remainingSeconds = ((remaining + 999) / 1000).toInt()
                }
            }
            // 到点 / 说完自动结束则转写；手动取消时 phase 已非 Recording，不处理
            if (voice.phase == VoicePhase.Recording) transcribeAndFinish()
        }
    }

    /** 录音中点「完成」：停止并识别 */
    fun finish() {
        if (voice.phase == VoicePhase.Recording) transcribeAndFinish()
    }

    private fun transcribeAndFinish() {
        if (voice.phase == VoicePhase.Transcribing) return
        voice.phase = VoicePhase.Transcribing
        val bytes = recorder.stop()
        scope.launch {
            val result = runCatching {
                SpeechToTextClient.transcribe(AsrConfigStore.ensure(repository), bytes)
            }
            val text = result.getOrNull().orEmpty()
            recorder.release()
            if (result.isSuccess && text.isNotBlank()) {
                voice.recognizedText = text
                onRecognized(text)
            } else {
                // 与输入框语音模式同款报错：可读原因 + （后端停维后）自部署说明
                voice.error = result.exceptionOrNull()?.let { aiErrorText(it, "语音识别") }
                    ?: if (text.isBlank()) "没听清，请再说一次"
                    else withServerExpiryNotice("语音识别失败，请再说一次")
                // 失败不退出面板，直接重新开录
                start()
            }
        }
    }

    /** 关闭面板：丢弃当前录音 */
    fun cancel() {
        if (voice.phase == VoicePhase.Recording) recorder.stop()
        recorder.release()
        voice.phase = VoicePhase.Starting
        onExit()
    }

    /** 面板离开组合时释放录音资源 */
    fun dispose() {
        recorder.release()
    }
}
