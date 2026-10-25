package com.xlin.travel.inputbox.voice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.SearchScrim
import com.xlin.travel.color.TextWhite
import com.xlin.travel.color.WhiteBtnBackground
import com.xlin.travel.color.WhiteBtnIcon
import com.xlin.travel.db.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.close_path
import travel.app.shared.generated.resources.send

/**
 * 语音输入面板：整屏遮罩 + 底部卡片，实时波形由录音器真实响度驱动。
 *
 * 交互对齐主流语音助手的习惯（HTML 初稿仅为参考，未 1:1 复刻）：
 * - 打开即申请麦克风权限并开始录音，静音时波形贴基线、说话才鼓起；
 * - 左侧「✕」取消，右侧「↑」结束录音并识别；
 * - 中部「自动发送」开关：开则识别结果直接发送，关则仅回填输入框。
 *
 * 波形、开关、转圈指示器见 [VoiceInputParts.kt]。
 *
 * @param onResult 识别成功回调：(文本, 是否直接发送)
 */
@Composable
fun VoiceInputPanel(
    repository: ChatRepository,
    state: VoiceInputState,
    onResult: (text: String, autoSend: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val recorder = remember { AudioRecorder() }
    val scope = rememberCoroutineScope()
    var startedOnce by remember { mutableStateOf(false) }

    // 面板离开组合（无论成功 / 取消 / 返回）都释放录音器，避免采集线程残留
    DisposableEffect(Unit) {
        onDispose { recorder.release() }
    }

    // 开始 / 重新开始录音：启动录音器并轮询真实响度写入波形
    fun startRecording() {
        if (recorder.start()) {
            state.error = null
            state.resetLevels()
            state.phase = VoicePhase.Recording
            scope.launch {
                while (state.phase == VoicePhase.Recording) {
                    state.pushLevel(recorder.level())
                    delay(60)
                }
            }
        } else {
            state.phase = VoicePhase.Starting
            state.error = "无法访问麦克风，请检查权限后重试"
        }
    }

    val requestMic = rememberMicPermission(onResult = { granted ->
        if (granted) startRecording()
        else {
            state.phase = VoicePhase.Starting
            state.error = "需要麦克风权限才能使用语音输入"
        }
    })

    LaunchedEffect(Unit) {
        if (!startedOnce) {
            startedOnce = true
            requestMic()
        }
    }

    fun finishAndTranscribe() {
        if (state.phase != VoicePhase.Recording) return
        scope.launch {
            state.phase = VoicePhase.Transcribing
            val bytes = recorder.stop()
            val result = runCatching {
                val config = AsrConfigStore.ensure(repository)
                SpeechToTextClient.transcribe(config, bytes)
            }
            val text = result.getOrNull().orEmpty()
            if (result.isSuccess && text.isNotBlank()) {
                state.recognizedText = text
                recorder.release()
                onResult(text, state.autoSend)
                onDismiss()
            } else {
                // 空录音 / 识别失败：保留面板提示原因，并重新进入录音让用户再说一次
                recorder.release()
                state.error = result.exceptionOrNull()?.message
                    ?: if (text.isBlank()) "没听清，请再说一次" else "识别失败"
                startRecording()
            }
        }
    }

    fun cancel() {
        scope.launch {
            if (state.phase == VoicePhase.Recording) recorder.stop()
            recorder.release()
            onDismiss()
        }
    }

    // 遮罩：点击空白不关闭（避免误触丢失录音），仅按钮控制
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchScrim),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChatBackground, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .safeContentPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            // 状态文字 + 自动发送开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val status = when (state.phase) {
                    VoicePhase.Starting -> "准备中…"
                    VoicePhase.Recording -> "正在聆听，请说话"
                    VoicePhase.Transcribing -> "识别中…"
                }
                Text(status, color = TextWhite, fontSize = 15.sp)
                Spacer(Modifier.size(10.dp))
                AutoSendToggle(state)
            }

            Spacer(Modifier.height(16.dp))

            // 实时波形：柱子高度由真实响度决定，说话才鼓起
            Waveform(state)

            state.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = AccentOrange, fontSize = 13.sp)
            }

            Spacer(Modifier.height(18.dp))

            // 底部操作：取消 / 发送
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(WhiteBtnBackground)
                        .clickable(onClick = ::cancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.close_path),
                        contentDescription = "取消语音输入",
                        colorFilter = ColorFilter.tint(WhiteBtnIcon),
                        modifier = Modifier.size(22.dp),
                    )
                }

                TranscribingSpinner(state)

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentOrange)
                        .clickable(enabled = state.phase == VoicePhase.Recording) { finishAndTranscribe() },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.send),
                        contentDescription = "完成并发送",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
