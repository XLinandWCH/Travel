package com.xlin.travel.inputbox.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * - 进入末尾倒计时（录音满 50 秒后）改为橘色的「X 秒后自动发送」。
 *
 * 不再提供“自动发送 开/关”开关：默认策略固定为——1 分钟内用户可自行点发送，
 * 满 1 分钟（最后 10 秒倒计时）则自动发送。
 */
@Composable
internal fun StatusLine(state: VoiceInputState) {
    val countdown = state.phase == VoicePhase.Recording && state.showCountdown
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (text, color) = when {
            countdown -> "${state.remainingSeconds} 秒后自动发送" to AccentOrange
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
    }
}
