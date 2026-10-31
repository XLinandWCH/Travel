package com.xlin.travel.videoexplain.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.videoexplain.drawCityscape
import com.xlin.travel.videoexplain.drawViewfinder

// 画面内部固定近白，不跟随浅色主题反转
private val FallbackNoteText = Color(0xFFEDE7E2)
private val FallbackNoteBackground = Color(0xCC120D0A)

/**
 * 取景占位画面：暮色里的里份剪影 + 九宫格。
 *
 * 给没有摄像头能力的平台（iOS / JVM / Web）兜底，也用于 Android
 * 拿不到权限或绑定失败时的降级，此时 [note] 说明原因。
 */
@Composable
internal fun CameraFallback(note: String? = null, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3A2A1E), Color(0xFF1B140F), Color(0xFF120D0A)),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) { drawCityscape() }
        Canvas(Modifier.fillMaxSize()) { drawViewfinder() }
        note?.let { message ->
            Text(
                text = message,
                color = FallbackNoteText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FallbackNoteBackground)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}
