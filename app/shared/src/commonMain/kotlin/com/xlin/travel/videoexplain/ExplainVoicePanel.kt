package com.xlin.travel.videoexplain

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.WhiteBtnBackground
import com.xlin.travel.color.WhiteBtnIcon
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.inputbox.voice.AudioRecorder
import com.xlin.travel.inputbox.voice.StatusLine
import com.xlin.travel.inputbox.voice.VoiceInputState
import com.xlin.travel.inputbox.voice.VoicePhase
import com.xlin.travel.inputbox.voice.Waveform
import com.xlin.travel.inputbox.voice.rememberMicPermission
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.close_path
import travel.app.shared.generated.resources.send

/**
 * 视频讲解页的语音面板：说话样式与输入框语音模式完全相同——
 * 同一套实时波形（[Waveform]）、同一句状态文案（[StatusLine]）、
 * 左侧 32dp 描边关闭、右侧 36dp 白底完成，只是外壳换成讲解画面上的浮层卡片。
 *
 * 点「完成」后走 ASR 识别，结果经 [onRecognized] 交给页面：
 * 命中「生成视频」就进入 3 秒生成动画，否则把这句话当字幕回显。
 */
@Composable
internal fun ExplainVoicePanel(
    repository: ChatRepository,
    onRecognized: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val voice = remember { VoiceInputState() }
    val recorder = remember { AudioRecorder() }
    val scope = rememberCoroutineScope()
    val runner = remember(voice, recorder, scope) {
        ExplainVoiceRunner(
            voice = voice,
            recorder = recorder,
            scope = scope,
            repository = repository,
            onRecognized = onRecognized,
            onExit = onClose,
        )
    }

    // 进入面板先要麦克风权限，授权后立刻开录；拒绝则留在面板里给错误提示
    val requestMic = rememberMicPermission { granted ->
        if (granted) {
            runner.start()
        } else {
            voice.phase = VoicePhase.Starting
            voice.error = "需要麦克风权限才能使用语音"
        }
    }
    LaunchedEffect(Unit) { requestMic() }
    DisposableEffect(runner) {
        onDispose { runner.dispose() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SettingCardBackground.copy(alpha = 0.92f))
            .border(1.dp, ChatBackground, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Waveform(voice)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 关闭：与输入框语音模式同尺寸同图标
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
                    contentDescription = "关闭语音",
                    colorFilter = ColorFilter.tint(BtnAddIcon),
                    modifier = Modifier.size(18.dp),
                )
            }

            StatusLine(voice)

            // 完成：录音中可点，转写中显示转圈
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
                        contentDescription = "完成并识别",
                        colorFilter = ColorFilter.tint(WhiteBtnIcon),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        voice.error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = FormErrorText, fontSize = 12.sp)
        }
    }
}
