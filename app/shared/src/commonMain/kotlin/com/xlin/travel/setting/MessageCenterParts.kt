package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.FormErrorText
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

/** 反馈列表项（精简）：一行「工单号 + 时间」，下一行首次反馈正文（单行，超出用 …）；
 *  管理员视角额外带「来自：xxx」；整项可点进入工单详情。其它（状态 / 回复预览 / 抽屉回复）已移至详情页 */
@Composable
fun FeedbackCard(
    item: FeedbackDto,
    showUsername: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SettingCardBackground)
            .clickable(onClick = onClick)
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
        // 用户第一次反馈的问题：单行，超出省略
        Text(
            text = item.content,
            color = TextGray300,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 公告卡片：管理员图标 + 标题（白）+ 时间 + 正文（最多一行，超出用 …，避免一大坠挤在列表里）。
 * 整项可点进入公告详情页；[canDelete] 为真（管理员）时长按弹出「删除」上下文菜单。
 */
@Composable
fun BroadcastCard(
    item: BroadcastDto,
    adminIcon: androidx.compose.ui.graphics.painter.Painter,
    canDelete: Boolean,
    menuExpanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SettingCardBackground)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (canDelete) onLongClick else null,
                )
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(text = formatMsgTime(item.createdAt), color = TextGray500, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            // 正文只展示一行，超出用省略号；完整内容进详情页看
            Text(
                text = item.content,
                color = TextGray300,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // 管理员长按菜单：与用户消息 / 抽屉历史项同形态（DropdownMenu），仅「删除」一项
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = onDismissMenu,
            containerColor = SettingCardBackground,
            modifier = Modifier.width(140.dp),
        ) {
            DropdownMenuItem(
                text = { Text("删除", color = FormErrorText, fontSize = 15.sp) },
                onClick = onDelete,
            )
        }
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
