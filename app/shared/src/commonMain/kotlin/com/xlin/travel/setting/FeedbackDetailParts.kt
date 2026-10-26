package com.xlin.travel.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.IconButtonBackground
import com.xlin.travel.color.InputCardBackground
import com.xlin.travel.color.InputPlaceholderColor
import com.xlin.travel.color.InputTextColor
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.SubmitReadyBackground
import com.xlin.travel.color.SubmitReadyText
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.color.UserBubbleBackground
import com.xlin.travel.color.WhiteBtnBackground
import com.xlin.travel.color.WhiteBtnIcon
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.back
import travel.app.shared.generated.resources.pentagram
import travel.app.shared.generated.resources.send

/**
 * 工单详情页复用组件（聊天式一问一答，不使用底部抽屉）：
 * - DetailTopBar：返回 + 「用户反馈」标题 + 右侧「工单状态」胶囊按钮（配色对齐意见反馈提交按钮，双主题反转）；
 * - 会话气泡：用户消息靠右、管理员消息靠左并带「管理员」标签，均附时间；
 * - TicketInputBar：仿聊天输入框的精简发送条（无停止 / 语音 / 图片）；
 * - ResolveDialog（评分 + 未解决 / 解决）与 StatusPickDialog（管理员选择状态）。
 * 配色全部取自 com.xlin.travel.color 双主题表。
 */

/** 工单状态英文存储值 → 中文展示（状态选择弹窗用） */
fun feedbackStatusLabel(status: String): String = when (status) {
    "resolved" -> "已解决"
    "in_progress" -> "处理中"
    else -> "提交"
}

/** 详情页顶栏：返回按钮 + 「用户反馈」标题 + 右侧「工单状态」按钮（只显示这四个字，不显示具体状态词） */
@Composable
fun DetailTopBar(
    onBack: () -> Unit,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IconButtonBackground)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.back),
                contentDescription = "返回",
                colorFilter = ColorFilter.tint(TextWhite),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "用户反馈",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        // 工单状态胶囊：深主题白底黑字 / 浅主题黑底白字，与意见反馈提交按钮配色一致
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SubmitReadyBackground)
                .clickable(onClick = onStatusClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "工单状态",
                color = SubmitReadyText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** 工单号一行（保留强调橙），置于会话内容顶部 */
@Composable
fun TicketIdRow(ticketId: String, modifier: Modifier = Modifier) {
    Text(
        text = ticketId,
        color = AccentOrange,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * 会话气泡：[mine] 为 true（工单归属用户的消息）靠右、中性气泡；false（管理员消息）靠左、强调橙气泡并带「管理员」标签。
 * 底部统一显示到分钟的时间。
 */
@Composable
fun ThreadBubble(
    mine: Boolean,
    content: String,
    createdAt: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        if (!mine) {
            Text(
                text = "管理员",
                color = AccentOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp),
            )
        }
        Column(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = if (mine) 16.dp else 4.dp,
                        topEnd = if (mine) 4.dp else 16.dp,
                        bottomEnd = 16.dp,
                        bottomStart = 16.dp,
                    )
                )
                .background(if (mine) UserBubbleBackground else AccentOrange.copy(alpha = 0.12f))
                .padding(12.dp),
        ) {
            Text(
                text = content,
                color = TextWhite,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatDetailTime(createdAt),
                color = TextGray500,
                fontSize = 11.sp,
                modifier = Modifier.align(if (mine) Alignment.End else Alignment.Start),
            )
        }
    }
}

/**
 * 精简聊天输入条（仿 inputbox 卡片底 + 发送按钮，无停止 / 语音 / 图片）：
 * 输入为空时发送按钮不可用；点击发送或键盘「发送」触发 [onSend]。
 */
@Composable
fun TicketInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = value.isNotBlank()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(InputCardBackground)
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 24.dp, max = 120.dp),
            textStyle = TextStyle(color = InputTextColor, fontSize = 16.sp, lineHeight = 24.sp),
            cursorBrush = SolidColor(AccentOrange),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSend = { if (enabled) onSend() },
            ),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = InputPlaceholderColor, fontSize = 16.sp)
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WhiteBtnBackground)
                .then(if (enabled) Modifier.clickable(onClick = onSend) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.send),
                contentDescription = "发送",
                colorFilter = ColorFilter.tint(WhiteBtnIcon.copy(alpha = if (enabled) 1f else 0.4f)),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 一排可点击五角星：已评分位染强调橙，未评分位灰色 */
@Composable
fun StarRatingRow(
    rating: Int,
    modifier: Modifier = Modifier,
    onRate: ((Int) -> Unit)? = null,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(5) { index ->
            val filled = index < rating
            Image(
                painter = painterResource(Res.drawable.pentagram),
                contentDescription = if (filled) "已评分 ${index + 1} 星" else "评分 ${index + 1} 星",
                colorFilter = ColorFilter.tint(if (filled) AccentOrange else TextGray500),
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .then(if (onRate != null) Modifier.clickable { onRate(index + 1) } else Modifier),
            )
        }
    }
}

/**
 * 确认解决弹窗：询问「是否解决」，带一排五颗空星供评分，底部「未解决」「解决」两个动作。
 */
@Composable
fun ResolveDialog(
    onDismiss: () -> Unit,
    onConfirm: (resolved: Boolean, rating: Int) -> Unit,
) {
    var picked by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SettingCardBackground)
                .padding(20.dp),
        ) {
            Text(
                text = "问题是否已解决？",
                color = TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "请为本次处理体验评分（可选），然后选择处理结果。",
                color = TextGray500,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StarRatingRow(rating = picked, onRate = { picked = it })
            }
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "未解决",
                    color = TextGray500,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onConfirm(false, picked) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "解决",
                    color = SubmitReadyText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SubmitReadyBackground)
                        .clickable { onConfirm(true, picked) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** 管理员选择工单状态弹窗：提交 / 处理中 / 已解决 三选一 */
@Composable
fun StatusPickDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val options = listOf("submitted" to "提交", "in_progress" to "处理中", "resolved" to "已解决")
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SettingCardBackground)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = "设置工单状态",
                color = TextGray500,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            options.forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(value) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = label, color = TextWhite, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (value == current) {
                        Text("✓", color = AccentOrange, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** 服务端 LocalDateTime 字符串（2026-10-25T12:30:00.123）转展示格式，精确到分钟 */
private fun formatDetailTime(raw: String): String = raw.replace('T', ' ').take(16)
