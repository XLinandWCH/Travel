package com.xlin.travel.chat_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ReasoningBarColor
import com.xlin.travel.color.ReasoningTextColor
import com.xlin.travel.color.TextGray400
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.chevron_right
import travel.app.shared.generated.resources.down
import kotlin.math.PI
import kotlin.math.sin

/**
 * AI 思考区（参考主流 AI 应用的「深度思考」折叠交互）：
 * - 思考中：标题「思考中」逐字上下波动（图标保持静止）+ down 图标，默认展开，
 *   思考内容放在仅左侧带强调橙边框、其余全透明的卡片里；点击可收起（思考继续）；
 * - 思考结束：自动收起，标题变灰色「已深度思考（用时X秒）」+ chevron_right 图标，
 *   点击可重新展开回看思考内容。
 * 两个图标尺寸与标题字号保持统一（16.dp / 14.sp）。
 */
@Composable
fun ReasoningSection(
    message: ChatMessage,
    isThinking: Boolean,
    modifier: Modifier = Modifier,
) {
    // 没有思考内容且不在思考中：整块不渲染
    if (!isThinking && message.reasoning.isEmpty()) return

    // null = 跟随默认（思考中展开、思考结束收起）；用户点击后记录手动状态
    var userExpanded by remember(message.id) { mutableStateOf<Boolean?>(null) }
    // 思考刚结束：清掉手动状态，让标题回到默认的收起态
    LaunchedEffect(isThinking) { if (!isThinking) userExpanded = null }
    val expanded = userExpanded ?: isThinking

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { userExpanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isThinking) {
                ThinkingLabel()
                Spacer(Modifier.width(4.dp))
                // 展开=箭头朝下（内容在下方可见），收起=旋转成朝右
                RotateChevron(
                    icon = Res.drawable.down,
                    expanded = expanded,
                    collapsedRotation = -90f,
                )
            } else {
                val elapsed = message.thinkingSeconds?.let { "（用时${it}秒）" }.orEmpty()
                Text(
                    text = "已深度思考$elapsed",
                    color = TextGray400,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.width(4.dp))
                // 收起=朝右（可展开），展开=旋转成朝下
                RotateChevron(
                    icon = Res.drawable.chevron_right,
                    expanded = expanded,
                    collapsedRotation = 0f,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            // 左强调橙竖条卡片：其余边框全透明，视觉上只有一条思考内容引导线；
            // 竖条用降低 30% 透明度的 ReasoningBarColor，正文可选中复制
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val barWidth = 2.dp.toPx()
                        drawRect(
                            color = ReasoningBarColor,
                            topLeft = Offset.Zero,
                            size = Size(barWidth, size.height),
                        )
                    }
                    .padding(start = 12.dp, top = 2.dp, bottom = 6.dp),
            ) {
                SelectionBox {
                    Text(
                        text = message.reasoning.trimEnd(),
                        color = ReasoningTextColor,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                }
            }
        }
    }
}

/** 「思考中」三个字依次上下波动，像大厂 AI 应用的呼吸加载；末尾省略号静止 */
@Composable
private fun ThinkingLabel() {
    val transition = rememberInfiniteTransition(label = "thinking-wave")
    // 一个 1.2s 的匀速循环相位，各字按序错开，形成波浪传递
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "phase",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        "思考中".forEachIndexed { index, char ->
            Text(
                text = char.toString(),
                color = TextGray400,
                fontSize = 14.sp,
                modifier = Modifier.graphicsLayer {
                    // ±2dp 的竖直偏移，相位差 0.9rad 依次滞后
                    translationY = sin((phase - index * 0.9f).toDouble()).toFloat() * 2.dp.toPx()
                },
            )
        }
        Text(text = "…", color = TextGray400, fontSize = 14.sp)
    }
}

/** 折叠指示图标：展开 / 收起时平滑旋转；图标尺寸与 14sp 标题匹配 */
@Composable
private fun RotateChevron(
    icon: org.jetbrains.compose.resources.DrawableResource,
    expanded: Boolean,
    collapsedRotation: Float,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) collapsedRotation + 90f else collapsedRotation,
        animationSpec = tween(180),
        label = "chevron-rotation",
    )
    Image(
        painter = painterResource(icon),
        contentDescription = if (expanded) "收起思考内容" else "展开思考内容",
        colorFilter = ColorFilter.tint(TextGray400),
        modifier = Modifier
            .size(16.dp)
            .graphicsLayer {
                rotationZ = rotation
                // 以图标中心旋转
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
            },
    )
}
