package com.xlin.travel.inputbox.voice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.BtnAddBorder
import com.xlin.travel.color.BtnAddIcon
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.WhiteBtnBackground
import com.xlin.travel.color.WhiteBtnIcon
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.inputbox.ChatInputState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.close_path
import travel.app.shared.generated.resources.send

/** 录音上限：60 秒；进入最后 10 秒显示倒计时并到点自动完成。 */
private const val MAX_DURATION_MS = 60_000L
private const val COUNTDOWN_START_MS = 10_000L

/**
 * 语音模式的输入卡片内容：复用 ChatInputBox 的“上中下”结构——
 * 中部 [Waveform] 换成实时波形，底部左侧“+”变关闭、右侧麦克风变完成发送、中部放聆听状态。
 * 尺寸沿用普通工具栏（左 32dp 描边、右 36dp 白底），仅替换图标。
 *
 * @param onSend 直接发送识别文本（自动发送开）；回填场景由 [ChatInputState.appendRecognized] 处理
 */
@Composable
internal fun VoiceContent(
    state: ChatInputState,
    voice: VoiceInputState,
    repository: ChatRepository,
    onSend: (String) -> Unit,
) {
    val recorder = remember { AudioRecorder() }
    val scope = rememberCoroutineScope()
    val runner = remember(voice, recorder, scope) {
        VoiceRunner(
            voice = voice,
            recorder = recorder,
            scope = scope,
            repository = repository,
            onFinish = { text, autoSend ->
                state.stopVoice()
                if (autoSend) onSend(text) else state.appendRecognized(text)
            },
            onExit = { state.stopVoice() },
        )
    }

    // 进入语音模式：在组合中拿到权限申请回调，用 LaunchedEffect 触发；授权回调里启动录音
    val requestMic = rememberMicPermission { granted ->
        if (granted) runner.startRecording()
        else {
            voice.phase = VoicePhase.Starting
            voice.error = "需要麦克风权限才能使用语音输入"
        }
    }
    LaunchedEffect(Unit) { requestMic() }
    DisposableEffect(runner) {
        onDispose { runner.dispose() }
    }

    // 中：实时波形
    Waveform(voice)
    Spacer(Modifier.height(8.dp))

    // 下：关闭 / 聆听状态（含倒计时） / 完成发送
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 关闭：替换原“+”按钮的图标为 close_path，尺寸不变
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, BtnAddBorder, RoundedCornerShape(10.dp))
                .clickable { runner.cancel() },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.close_path),
                contentDescription = "关闭语音输入",
                colorFilter = ColorFilter.tint(BtnAddIcon),
                modifier = Modifier.size(18.dp),
            )
        }

        StatusLine(voice)

        // 完成发送：替换原麦克风图标为 send，转写中显示转圈
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WhiteBtnBackground)
                .clickable(enabled = voice.phase == VoicePhase.Recording) { runner.finish() },
            contentAlignment = Alignment.Center,
        ) {
            if (voice.phase == VoicePhase.Transcribing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AccentOrange,
                    strokeWidth = 2.dp,
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.send),
                    contentDescription = "完成并发送",
                    colorFilter = ColorFilter.tint(WhiteBtnIcon),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    // 错误提示（空录音 / 识别失败 / 无权限）展示在工具栏下方
    voice.error?.let {
        Spacer(Modifier.height(6.dp))
        Text(it, color = FormErrorText, fontSize = 12.sp)
    }
}

/**
 * 语音录音 / 转写驱动器（普通类，方法可互相调用，规避局部函数不能前向引用的限制）。
 * begin() 走权限→录音→轮询波形与倒计时；finish() 停止并转写；cancel() 丢弃退出。
 */
private class VoiceRunner(
    private val voice: VoiceInputState,
    private val recorder: AudioRecorder,
    private val scope: CoroutineScope,
    private val repository: ChatRepository,
    private val onFinish: (text: String, autoSend: Boolean) -> Unit,
    private val onExit: () -> Unit,
) {
    /** 权限已授予后启动录音，并在协程里轮询真实响度驱动波形与末尾倒计时 */
    fun startRecording() {
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
            while (voice.phase == VoicePhase.Recording) {
                voice.pushLevel(recorder.level())
                delay(60)
                elapsed += 60
                val remaining = MAX_DURATION_MS - elapsed
                if (remaining <= 0L) break
                if (remaining <= COUNTDOWN_START_MS) {
                    voice.showCountdown = true
                    voice.remainingSeconds = ((remaining + 999) / 1000).toInt()
                }
            }
            // 循环结束：到点自动结束则转写；手动取消时 phase 已非 Recording，不处理
            if (voice.phase == VoicePhase.Recording) transcribeAndFinish()
        }
    }

    /** 录音中点「完成」 */
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
                onFinish(text, voice.autoSend)
            } else {
                voice.error = result.exceptionOrNull()?.message
                    ?: if (text.isBlank()) "没听清，请再说一次" else "识别失败"
                startRecording()
            }
        }
    }

    /** 取消并退出：丢弃当前录音 */
    fun cancel() {
        if (voice.phase == VoicePhase.Recording) recorder.stop()
        recorder.release()
        voice.phase = VoicePhase.Starting
        onExit()
    }

    /** 离开语音模式时释放 */
    fun dispose() {
        recorder.release()
    }
}
