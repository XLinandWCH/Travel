package com.xlin.travel.inputbox.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.SettingValueText
import com.xlin.travel.color.TextWhite

/** 波形柱区域高度与单柱最低高度 */
private val WaveAreaHeight = 36.dp
private val BarMinHeight = 4.dp

/**
 * 实时波形：读取 [VoiceInputState.levels]（滚动时间序列），每根柱子居中对称生长。
 * 柱高由录音器真实响度决定——静音贴基线、说话才鼓起，非随机动画。
 */
@Composable
internal fun Waveform(state: VoiceInputState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(WaveAreaHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.levels.forEach { level ->
            val height = BarMinHeight + (WaveAreaHeight - BarMinHeight) * level
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentOrange.copy(alpha = (0.35f + level * 0.65f).coerceAtMost(1f))),
            )
        }
    }
}

/**
 * 底部居中状态行：
 * - 准备中 / 正在聆听 / 识别中…（默认白字）；
 * - 进入末尾倒计时时改为橘色的「X 秒后自动发送 / 结束」；
 * - 录音态右侧带「自动发送」开关。
 */
@Composable
internal fun StatusLine(state: VoiceInputState) {
    val countdown = state.phase == VoicePhase.Recording && state.showCountdown
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (text, color) = when {
            countdown -> "${state.remainingSeconds} 秒后" +
                (if (state.autoSend) "自动发送" else "结束") to AccentOrange
            state.phase == VoicePhase.Starting -> "准备中…" to SettingValueText
            state.phase == VoicePhase.Transcribing -> "识别中…" to SettingValueText
            else -> "正在聆听，请说话" to TextWhite
        }
        Text(
            text = text,
            color = color,
            fontSize = 15.sp,
            fontWeight = if (countdown) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (state.phase == VoicePhase.Recording && !countdown) {
            Spacer(Modifier.size(8.dp))
            AutoSendToggle(state)
        }
    }
}

/** 「自动发送」开关：点击切换 [VoiceInputState.autoSend] */
@Composable
private fun AutoSendToggle(state: VoiceInputState) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { state.autoSend = !state.autoSend }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("自动发送", color = SettingValueText, fontSize = 13.sp)
        Spacer(Modifier.size(6.dp))
        Text(
            text = if (state.autoSend) "开" else "关",
            color = if (state.autoSend) AccentOrange else SettingValueText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
