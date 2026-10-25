package com.xlin.travel.chat_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.InputCardBackground
import com.xlin.travel.color.ScrollButtonIdleIcon
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.down

/**
 * 「滚到底部」悬浮圆形按钮：
 * - 圆形底 + 强调橙 down 图标；
 * - [spinning] 为 true（AI 正在输出）时，底环 + 一段带渐隐尾巴的橙色弧绕圆心旋转
 *   （indeterminate spinner 尾迹），提示“下方还有正在生成的内容”；
 *   非输出态不画任何橙色（圆环也隐藏），只留安静的白 70% 图标；
 * - 仅在用户离开底部时可见，点击一路滚到最新一行并恢复吸底跟随。
 */
@Composable
fun ScrollToBottomButton(
    visible: Boolean,
    spinning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.7f),
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.7f),
        modifier = modifier,
    ) {
        // 旋转相位：1.1s 匀速转一圈，尾巴弧跟随头部
        val spin = rememberInfiniteTransition(label = "tail-spin")
        val angle by spin.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
            label = "angle",
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                // 普通中性投影：早前用橙色 ambient/spot 投影在空闲态仍透橙，不符合“非输出态零橙色”
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(InputCardBackground)
                .drawBehind {
                    // 橙色只在 AI 输出中绘制：底环 + 旋转尾巴；非输出态保持“安静”，不画任何橙色
                    if (spinning) {
                        val stroke = 2.5.dp.toPx()
                        val radius = (size.minDimension - stroke) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        // 底环：淡橙描边
                        drawCircle(
                            color = AccentOrange.copy(alpha = 0.28f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        // 旋转尾巴：头部最亮、尾部逐段渐隐（sweep 逐段向后叠加）
                        val segments = listOf(1.00f, 0.62f, 0.34f, 0.14f)
                        val segmentSweep = 26f
                        segments.forEachIndexed { i, alpha ->
                            drawArc(
                                color = AccentOrange.copy(alpha = alpha),
                                startAngle = angle - (i + 1) * segmentSweep,
                                sweepAngle = segmentSweep + 4f,
                                useCenter = false,
                                topLeft = Offset.Zero,
                                size = Size(size.width, size.height),
                                style = Stroke(
                                    width = stroke,
                                    cap = if (i == 0) StrokeCap.Round else StrokeCap.Butt,
                                ),
                            )
                        }
                    }
                }
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.down),
                contentDescription = "回到底部",
                // 输出中跟着尾迹用强调橙；空闲时白 70%，不再抢眼
                colorFilter = ColorFilter.tint(if (spinning) AccentOrange else ScrollButtonIdleIcon),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
