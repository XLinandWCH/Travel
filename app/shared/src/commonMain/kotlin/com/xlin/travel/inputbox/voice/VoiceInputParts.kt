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
import androidx.compose.material3.CircularProgressIndicator
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

/** 波形柱区域高度与单柱最低高度 */
private val WaveAreaHeight = 44.dp
private val BarMinHeight = 5.dp

/** 实时波形柱：读取 state.levels（滚动时间序列），居中对称生长，静音贴基线 */
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

/** 「自动发送」开关：点击切换 state.autoSend */
@Composable
internal fun AutoSendToggle(state: VoiceInputState) {
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

/** 识别中显示小进度指示器，非识别态时占位保持布局稳定 */
@Composable
internal fun TranscribingSpinner(state: VoiceInputState) {
    if (state.phase == VoicePhase.Transcribing) {
        CircularProgressIndicator(
            modifier = Modifier.size(26.dp),
            color = AccentOrange,
            strokeWidth = 2.5.dp,
        )
    } else {
        Spacer(Modifier.size(26.dp))
    }
}
