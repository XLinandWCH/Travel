package com.xlin.travel.videoexplain.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange

/**
 * 桌面 / Web 的播放占位：这两端无法在 Compose 画布里内嵌系统播放器，
 * 用封面 + 呼吸播放键 + 循环进度条表达「正在播放」，不出现报错文案。
 */
private val CoverText = Color(0xFFF2F3F5)
private val CoverTextDim = Color(0xFFB9B4AF)

@Composable
internal fun VideoPlayerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "video-cover")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(1_600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "progress",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF3A2A1E), Color(0xFF171009)))
            ),
    ) {
        Canvas(
            Modifier
                .align(Alignment.Center)
                .size(78.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                },
        ) {
            drawCircle(Color.White.copy(alpha = 0.08f))
            drawCircle(Color.White.copy(alpha = 0.16f), radius = size.minDimension * 0.34f)
            val triangle = Path().apply {
                moveTo(size.width * 0.43f, size.height * 0.34f)
                lineTo(size.width * 0.43f, size.height * 0.66f)
                lineTo(size.width * 0.69f, size.height * 0.50f)
                close()
            }
            drawPath(triangle, CoverText)
        }

        Text(
            text = "讲解视频",
            color = CoverTextDim,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )

        // 底部进度条：模拟播放进度从 0 走到满再重来
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.18f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(AccentOrange),
            )
        }
    }
}
