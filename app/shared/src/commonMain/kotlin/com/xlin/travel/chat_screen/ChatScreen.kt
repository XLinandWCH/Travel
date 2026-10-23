package com.xlin.travel.chat_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange

/**
 * 聊天界面：展示用户与 AI 的消息列表，并随流式输出自动滚动到底部。
 * 全新对话（消息为空）时，居中随机显示一条欢迎语。
 */
@Composable
fun ChatScreen(
    state: ChatState,
    modifier: Modifier = Modifier,
) {
    if (state.messages.isEmpty()) {
        // 空会话：欢迎语居中，强调色显示；每次进入新会话重新随机
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val greeting = remember(state.conversationId) { ChatGreetings.random() }
            Text(
                text = greeting,
                color = AccentOrange,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val lastMessage = state.messages.lastOrNull()

    // 新消息或流式增量到达时滚动到底部
    LaunchedEffect(
        state.conversationId,
        state.messages.size,
        lastMessage?.content?.length,
        lastMessage?.reasoning?.length,
    ) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 64.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.messages, key = { it.id }) { message ->
            when (message.role) {
                MessageRole.User -> UserMessageBubble(message.content)
                MessageRole.Assistant -> AssistantMessageBlock(message)
            }
        }
    }
}
