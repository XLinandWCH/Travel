package com.xlin.travel.videoexplain

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import kotlin.math.cos
import kotlin.math.sin

/**
 * 「正在生成讲解视频…」的 3 秒动画：
 * 进度环按真实时长推进到 100%，环上一颗光点绕圈、画面里有一条扫描光带上下扫过，
 * 中间百分比 + 当前步骤文案跟着变。走满 [ExplainGeneratingMillis] 后由页面切到视频。
 */
private val GenText = Color(0xFFF2F3F5)
private val GenTextDim = Color(0xFFB9B4AF)

@Composable
internal fun GeneratingStage(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(ExplainGeneratingMillis.toInt(), easing = LinearEasing),
        )
    }
    val transition = rememberInfiniteTransition(label = "generating")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing)),
        label = "spin",
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing)),
        label = "sweep",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF241A13), Color(0xFF120D0A)))
            ),
    ) {
        // 扫描光带：一条暖橙横向渐隐带从上往下匀速扫过，表示正在渲染画面
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * sweep
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.5f to AccentOrange.copy(alpha = 0.16f),
                        1f to Color.Transparent,
                    ),
                    startY = y - 90.dp.toPx(),
                    endY = y + 90.dp.toPx(),
                ),
                topLeft = Offset(0f, (y - 90.dp.toPx()).coerceAtLeast(0f)),
                size = Size(size.width, 180.dp.toPx()),
            )
        }

        // 进度环：底环 + 橙色进度弧 + 绕圈光点
        Canvas(Modifier.align(Alignment.Center).size(176.dp)) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.minDimension / 2f - stroke,
                style = Stroke(stroke),
            )
            drawArc(
                color = AccentOrange,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            // 光点：沿半径 1 的圆周按 spin 角度落在环上
            val radius = size.minDimension / 2f - stroke
            val rad = spin * (kotlin.math.PI / 180.0)
            drawCircle(
                color = Color(0xFFFFE0B2),
                radius = stroke * 0.62f,
                center = Offset(
                    x = size.width / 2f + (radius * cos(rad)).toFloat(),
                    y = size.height / 2f + (radius * sin(rad)).toFloat(),
                ),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${(progress.value * 100).toInt()}%",
                color = GenText,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = stepLabel(progress.value), color = GenTextDim, fontSize = 12.sp)
        }

        // 顶部标题：呼吸点 + 正在生成讲解视频…
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.32f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulsingDot(size = 9.dp)
            Spacer(Modifier.width(8.dp))
            Text("正在生成讲解视频…", color = GenText, fontSize = 14.sp)
        }

        Text(
            text = "根据这句话为你编排镜头与解说",
            color = GenTextDim.copy(alpha = 0.8f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
        )
    }
}

/** 步骤文案：按进度切四段，和百分比一起变化 */
private fun stepLabel(progress: Float): String = when {
    progress < 0.3f -> "理解你的问题…"
    progress < 0.62f -> "编排镜头脚本…"
    progress < 0.9f -> "渲染讲解画面…"
    else -> "合成讲解视频…"
}
