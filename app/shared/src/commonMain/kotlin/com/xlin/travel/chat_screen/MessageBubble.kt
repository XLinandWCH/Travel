package com.xlin.travel.chat_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AssistantTextColor
import com.xlin.travel.color.ReasoningTextColor
import com.xlin.travel.color.UserBubbleBackground
import com.xlin.travel.color.UserMessageText

/** 用户消息：右侧气泡，靠右对齐 */
@Composable
fun UserMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(UserBubbleBackground)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                color = UserMessageText,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

/** AI 消息：左侧展示，思考内容灰色，正文白色 */
@Composable
fun AssistantMessageBlock(message: ChatMessage) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (message.reasoning.isNotEmpty()) {
            Text(
                text = message.reasoning,
                color = ReasoningTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            if (message.content.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
            }
        }

        if (message.content.isNotEmpty()) {
            Text(
                text = message.content,
                color = AssistantTextColor,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
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
