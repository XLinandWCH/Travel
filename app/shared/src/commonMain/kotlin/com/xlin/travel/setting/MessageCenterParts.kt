package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.SettingDivider
import com.xlin.travel.color.SettingValueText
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite

/**
 * 消息中心的复用组件：分段标签、反馈卡片、公告卡片、空态提示。
 * 卡片风格对齐设置分组卡片（SettingCardBackground + 16dp 圆角）。
 */

/** 「反馈 / 公告」两枚分段标签，选中项底部带强调橙指示条 */
@Composable
fun MessageTabRow(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    color = if (index == selected) TextWhite else TextGray500,
                    fontSize = 15.sp,
                    fontWeight = if (index == selected) FontWeight.Medium else FontWeight.Normal,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index == selected) AccentOrange else androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        }
    }
}

/** 反馈卡片：工单号（橙）+ 时间 + 内容；管理员视角额外带用户名与回复入口；
 *  已有回复（或管理员视角选中回复中）展示回复气泡 */
@Composable
fun FeedbackCard(
    item: FeedbackDto,
    showUsername: Boolean,
    onReply: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SettingCardBackground)
            .padding(16.dp),
    ) {
        // 工单号 + 时间
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.ticketId,
                color = AccentOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(text = formatMsgTime(item.createdAt), color = TextGray500, fontSize = 12.sp)
        }
        if (showUsername) {
            Spacer(Modifier.height(4.dp))
            Text(text = "来自：${item.username}", color = TextGray500, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = item.content, color = TextGray300, fontSize = 14.sp, lineHeight = 21.sp)

        // 管理员回复（若有）
        item.reply?.let { answer ->
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentOrange.copy(alpha = 0.12f))
                    .padding(12.dp),
            ) {
                Column {
                    Text(
                        text = "管理员回复" + (item.repliedAt?.let { " · ${formatMsgTime(it)}" } ?: ""),
                        color = AccentOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text = answer, color = TextWhite, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }
        }

        // 管理员专属：回复入口（已回复则显示“再次回复”）
        if (onReply != null) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SettingDivider),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onReply)
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (item.reply == null) "回复" else "再次回复",
                    color = AccentOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** 公告卡片：管理员图标 + 标题（白）+ 时间 + 正文 */
@Composable
fun BroadcastCard(
    item: BroadcastDto,
    adminIcon: androidx.compose.ui.graphics.painter.Painter,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SettingCardBackground)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = adminIcon,
                contentDescription = "管理员",
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(AccentOrange),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.title,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(text = formatMsgTime(item.createdAt), color = TextGray500, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = item.content, color = TextGray300, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

/** 空态提示：列表为空时居中显示一行灰字 */
@Composable
fun MessageEmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = SettingValueText, fontSize = 14.sp)
    }
}

/** 服务端 LocalDateTime 字符串（2026-10-25T12:30:00.123）转展示格式，精确到分钟 */
private fun formatMsgTime(raw: String): String =
    raw.replace('T', ' ').take(16)
