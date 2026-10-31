package com.xlin.travel.chat_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ReasoningBarColor
import com.xlin.travel.color.ReasoningTextColor
import com.xlin.travel.color.TextGray400
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.chevron_right
import travel.app.shared.generated.resources.down

/**
 * 内容校验区：与 [ReasoningSection] 同一套视觉（呼吸标题 + 左强调橙竖条折叠卡片），
 * 只是文案换成「校验中 / 校验完成」。
 * - 校验中：标题逐字波动、默认展开，进度行随校验器逐条出现；
 * - 校验结束：自动收起为灰色「校验完成」，点击可回看核对过程。
 *
 * 位置固定在「个性化界面」入口之后、动作按钮之前：界面与校验是并行产出的，
 * 但呈现顺序按「正文 → 界面 → 校验」排，读起来像收尾前的一道复核。
 */
@Composable
fun VerifySection(message: ChatMessage, modifier: Modifier = Modifier) {
    val isVerifying = message.verifyRunning
    // 既没在校验、也没有任何校验记录时整块不渲染（没调界面的回答就没有校验）
    if (!isVerifying && message.verifySteps.isEmpty()) return

    // null = 跟随默认（校验中展开、结束收起）；用户点击后记录手动状态
    var userExpanded by remember(message.id) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isVerifying) { if (!isVerifying) userExpanded = null }
    val expanded = userExpanded ?: isVerifying

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { userExpanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isVerifying) {
                WaveLabel("校验中")
                Spacer(Modifier.width(4.dp))
                RotateChevron(
                    icon = Res.drawable.down,
                    expanded = expanded,
                    collapsedRotation = -90f,
                )
            } else {
                Text(
                    text = "校验完成",
                    color = TextGray400,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.width(4.dp))
                RotateChevron(
                    icon = Res.drawable.chevron_right,
                    expanded = expanded,
                    collapsedRotation = 0f,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded && message.verifySteps.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            // 与思考区一致：只画左侧一条强调橙竖条，其余透明
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
                message.verifySteps.forEachIndexed { index, step ->
                    if (index > 0) Spacer(Modifier.height(4.dp))
                    SelectionBox {
                        Text(
                            text = step,
                            color = ReasoningTextColor,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        )
                    }
                }
            }
        }
    }
}
