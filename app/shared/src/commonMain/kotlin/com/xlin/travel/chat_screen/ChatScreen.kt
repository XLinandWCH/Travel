package com.xlin.travel.chat_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 聊天界面：展示用户与 AI 的消息列表，并随流式输出自动滚动到底部。
 */
@Composable
fun ChatScreen(
    state: ChatState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val lastMessage = state.messages.lastOrNull()

    // 新消息或流式增量到达时滚动到底部
    LaunchedEffect(
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
