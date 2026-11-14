package com.xlin.travel.setting

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.xlin.travel.color.*
import com.xlin.travel.image_picker.decodeAvatar
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.*

/**
 * 客服顶栏：返回 + 客服头像 + 标题/副标题 + 可选「新建对话」按钮。
 * 头像常驻，保证客服身份始终可见。
 */
@Composable
fun CustomerServiceTopBar(
    onBack: () -> Unit,
    title: String,
    subtitle: String? = null,
    onNewChat: (() -> Unit)? = null,
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
        Spacer(Modifier.width(10.dp))
        Image(
            painter = painterResource(Res.drawable.customer_avatar),
            contentDescription = "客服头像",
            modifier = Modifier.size(34.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, color = TextGray500, fontSize = 12.sp)
            }
        }
        if (onNewChat != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IconButtonBackground)
                    .clickable(onClick = onNewChat),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.new_chat),
                    contentDescription = "新建对话",
                    colorFilter = ColorFilter.tint(TextWhite),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * 客服消息（对标微信 / Telegram）：客服在左、用户在右，各带头像。
 * 图片与文字拆成独立气泡——图片为无底色圆角缩略图、文字为浅色圆角卡片，互不包裹。
 * 客服气泡用浅杏橙 [ServiceBubbleBackground]，用户气泡用 [UserBubbleBackground]，仅做基础区分。
 */
@Composable
fun CustomerServiceBubble(
    mine: Boolean,
    content: String,
    imageData: String?,
    createdAt: String,
    userAvatar: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!mine) {
            ServiceAvatar()
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = 260.dp),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            imageData?.let { data ->
                var bitmap by remember(data) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(data) { bitmap = runCatching { decodeAvatar(data) }.getOrNull() }
                bitmap?.let { CustomerImageMessage(it) }
            }
            if (content.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = if (mine) 16.dp else 4.dp,
                                topEnd = if (mine) 4.dp else 16.dp,
                                bottomEnd = 16.dp,
                                bottomStart = 16.dp,
                            )
                        )
                        .background(if (mine) UserBubbleBackground else ServiceBubbleBackground)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(text = content, color = TextWhite, fontSize = 15.sp, lineHeight = 21.sp)
                }
            }
            Text(text = formatCustomerTime(createdAt), color = TextGray500, fontSize = 11.sp)
        }
        if (mine) {
            Spacer(Modifier.width(8.dp))
            UserAvatar(userAvatar)
        }
    }
}

/** 客服头像（固定 customer_avatar.png） */
@Composable
private fun ServiceAvatar() {
    Image(
        painter = painterResource(Res.drawable.customer_avatar),
        contentDescription = "客服头像",
        modifier = Modifier.size(38.dp).clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

/** 用户头像：有头像用头像，否则用默认用户图标圆底 */
@Composable
private fun UserAvatar(bitmap: ImageBitmap?) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "我的头像",
            modifier = Modifier.size(38.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(AvatarBackground),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.user),
                contentDescription = "我的头像",
                colorFilter = ColorFilter.tint(TextGray400),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 图片消息：按原始比例缩放到最长边 ≤180dp 的圆角缩略图，无气泡底色，随发送方靠边 */
@Composable
private fun CustomerImageMessage(bitmap: ImageBitmap) {
    val longest = maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
    val scale = minOf(1f, 180f / longest)
    val w = (bitmap.width * scale).toInt().coerceAtLeast(1).dp
    val h = (bitmap.height * scale).toInt().coerceAtLeast(1).dp
    Image(
        bitmap = bitmap,
        contentDescription = "图片消息",
        modifier = Modifier.size(w, h).clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.FillBounds,
    )
}

/** 服务端 LocalDateTime 字符串（2026-10-25T12:30:00.123）转展示格式，精确到分钟 */
internal fun formatCustomerTime(raw: String): String = raw.replace('T', ' ').take(16)