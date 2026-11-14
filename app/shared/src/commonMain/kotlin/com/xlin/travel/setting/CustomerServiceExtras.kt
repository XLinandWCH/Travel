package com.xlin.travel.setting

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.xlin.travel.color.*
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.*

/**
 * 客服输入条：图片选择 + 文本框 + 发送。
 * 文本为空且未选图片时发送不可用;[sending] 时禁用避免重复提交。
 */
@Composable
fun CustomerServiceInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSend: () -> Unit,
    onImageClick: () -> Unit,
    hasImage: Boolean,
    sending: Boolean,
    modifier: Modifier = Modifier,
) {
    val enabled = (value.isNotBlank() || hasImage) && !sending
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(IconButtonBackground)
                .clickable(onClick = onImageClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.add_file),
                contentDescription = "添加图片",
                colorFilter = ColorFilter.tint(TextWhite),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(UserBubbleBackground)
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp, max = 120.dp),
                textStyle = TextStyle(color = TextWhite, fontSize = 14.sp, lineHeight = 21.sp),
                cursorBrush = SolidColor(AccentOrange),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (enabled) onSend() }),
                maxLines = 4,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(text = placeholder, color = TextGray500, fontSize = 14.sp)
                        }
                        inner()
                    }
                },
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (enabled) AccentOrange else TextGray500.copy(alpha = 0.3f))
                .then(if (enabled) Modifier.clickable(onClick = onSend) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.send),
                contentDescription = "发送",
                colorFilter = ColorFilter.tint(TextWhite),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * 管理员会话列表项。[isGlobal] 为 true 时是「全局通知」入口（对所有用户发布），
 * 否则为单个客户会话：用户名 + 最后一条消息摘要 + 时间。
 */
@Composable
fun CustomerConversationItem(
    username: String,
    lastMessage: String?,
    lastMessageAt: String?,
    isGlobal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IconButtonBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isGlobal) AccentOrange.copy(alpha = 0.15f) else AvatarBackground),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(if (isGlobal) Res.drawable.message else Res.drawable.user),
                contentDescription = if (isGlobal) "全局通知" else "客户",
                colorFilter = ColorFilter.tint(if (isGlobal) AccentOrange else TextGray400),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isGlobal) "全局通知" else username,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    isGlobal && lastMessage.isNullOrBlank() -> "向所有用户发布通知"
                    else -> lastMessage?.takeIf { it.isNotBlank() } ?: "暂无消息"
                },
                color = TextGray500,
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
        if (!lastMessageAt.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(text = formatCustomerTime(lastMessageAt), color = TextGray500, fontSize = 11.sp)
        }
    }
}

/** 待发送图片预览：缩略图 + 右上角移除按钮 */
@Composable
fun CustomerImagePreview(
    preview: ImageBitmap,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Image(
            bitmap = preview,
            contentDescription = "待发送图片",
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(RemoveButtonBackground)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.close_path),
                contentDescription = "移除图片",
                colorFilter = ColorFilter.tint(TextWhite),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}