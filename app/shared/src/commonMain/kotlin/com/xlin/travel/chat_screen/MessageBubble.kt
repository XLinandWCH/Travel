package com.xlin.travel.chat_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ReasoningTextColor
import com.xlin.travel.color.UserBubbleBackground
import com.xlin.travel.color.UserMessageText

/**
 * 用户消息：靠右对齐。
 * 若附带了图片，则缩略图在上、提示词气泡在下（与输入框预览图同大小，均为 64.dp）。
 */
@Composable
fun UserMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 图片缩略图：尺寸与输入框预览一致，右上角圆角方形
            message.imageBitmap?.let { image ->
                Image(
                    bitmap = image,
                    contentDescription = "用户发送的图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }

            // 提示词气泡：无文字时（仅发图）不渲染空气泡
            if (message.content.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(UserBubbleBackground)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = message.content,
                        color = UserMessageText,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }
}

/**
 * AI 消息：左侧展示，思考内容灰色小字，正文按 Markdown 渲染。
 * 正文输出结束后（[showActions] 为 true）在下方从左到右排开四个动作按钮。
 */
@Composable
fun AssistantMessageBlock(
    message: ChatMessage,
    showActions: Boolean,
    canRegenerate: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onRegenerate: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (message.reasoning.isNotEmpty()) {
            // 思考内容末尾常带换行，trimEnd 后与正文只保留一个很小的间距
            Text(
                text = message.reasoning.trimEnd(),
                color = ReasoningTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            if (message.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
            }
        }

        if (message.content.isNotEmpty()) {
            MarkdownText(message.content)

            if (showActions) {
                Spacer(Modifier.height(6.dp))
                MessageActionRow(
                    message = message,
                    canRegenerate = canRegenerate,
                    onLike = onLike,
                    onDislike = onDislike,
                    onRegenerate = onRegenerate,
                    onCopy = onCopy,
                )
            }
        } else if (message.reasoning.isEmpty()) {
            // 还没有任何输出时展示等待提示
            Text(
                text = "正在思考…",
                color = ReasoningTextColor,
                fontSize = 14.sp,
            )
        }
    }
}
