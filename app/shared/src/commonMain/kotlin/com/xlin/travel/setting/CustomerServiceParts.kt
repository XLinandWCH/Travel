package com.xlin.travel.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.IconButtonBackground
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.color.UserBubbleBackground
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.customer_avatar
import travel.app.shared.generated.resources.back

/** 客服服务顶栏：返回按钮 + "客服服务" 标题 */
@Composable
fun CustomerServiceTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "客服服务",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 客服会话气泡：[mine] 为 true（用户消息）靠右、中性气泡；false（客服消息）靠左、强调橙气泡并带客服头像。
 * 底部统一显示到分钟的时间。
 */
@Composable
fun CustomerServiceBubble(
    mine: Boolean,
    content: String,
    createdAt: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!mine) {
            // 客服头像
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .padding(end = 8.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.customer_avatar),
                    contentDescription = "客服头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        ) {
            if (!mine) {
                Text(
                    text = "客服",
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

        if (mine) {
            // 用户头像位置留空，保持对齐
            Spacer(Modifier.size(36.dp))
        }
    }
}

/**
 * 客服聊天输入条（仿 inputbox 卡片底 + 发送按钮）：
 * 输入为空时发送按钮不可用；点击发送或键盘「发送」触发 [onSend]。
 */
@Composable
fun CustomerServiceInputBar(
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(UserBubbleBackground)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = value,
                color = TextWhite,
                fontSize = 14.sp,
                maxLines = 4,
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = TextGray500,
                    fontSize = 14.sp,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (enabled) AccentOrange else TextGray500.copy(alpha = 0.3f))
                .clickable(enabled = enabled, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "发送",
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** 服务端 LocalDateTime 字符串（2026-10-25T12:30:00.123）转展示格式，精确到分钟 */
private fun formatDetailTime(raw: String): String = raw.replace('T', ' ').take(16)
