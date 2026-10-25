package com.xlin.travel.chat_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 聊天界面：展示用户与 AI 的消息列表，并随流式输出自动滚动到底部。
 * 全新对话（消息为空）时，居中随机显示一条欢迎语。
 *
 * 每一条 AI 回答输出结束后，正文下方都会出现赞同 / 不赞同 / 重新生成 / 复制四个按钮
 * （正在流式输出的那一条除外）；其中「重新生成」只在最后一条 AI 回答上可用，
 * 顶部提示（[TopToast]）与点踩后的反馈面板（[FeedbackBottomSheet]）：面板是真正的底部面板
 * （ModalBottomSheet，独立窗口层），提示仍画在聊天区之上。
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
    val toast = remember { ChatToastState() }
    val sheet = remember { FeedbackSheetState() }
    val scope = rememberCoroutineScope()
    val writeClipboard = rememberClipboardWriter()
    val lastMessage = state.messages.lastOrNull()
    val lastIndex = state.messages.lastIndex

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

    // 切换会话时收起可能残留的反馈面板
    LaunchedEffect(state.conversationId) { sheet.dismiss() }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 64.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(state.messages, key = { _, message -> message.id }) { index, message ->
                when (message.role) {
                    MessageRole.User -> UserMessageBubble(message)
                    MessageRole.Assistant -> AssistantMessageBlock(
                        message = message,
                        // 每条 AI 回答都展示动作条，仅隐藏正在流式输出的最后一条
                        showActions = !(state.isStreaming && index == lastIndex),
                        // 重新生成只会重跑最后一条回答，因此仅在它上面可用
                        canRegenerate = index == lastIndex && !state.isStreaming,
                        onLike = {
                            state.markFeedback(message, Feedback.Like)
                            toast.show("感谢你的反馈！")
                        },
                        onDislike = {
                            state.markFeedback(message, Feedback.Dislike)
                            sheet.show()
                        },
                        onRegenerate = { scope.launch { state.regenerate() } },
                        onCopy = {
                            // 只复制正文，思考内容不复制
                            writeClipboard(message.content)
                            toast.show("复制成功")
                        },
                    )
                }
            }
        }

        // 反馈面板是独立窗口，会遮住顶部提示，因此等它收起后再弹提示
        FeedbackBottomSheet(
            state = sheet,
            onSubmit = {
                scope.launch {
                    delay(SheetDismissMillis)
                    toast.show("已提交，感谢反馈")
                }
            },
        )
        TopToast(toast)
    }
}

/** 反馈面板的收起动画时长 */
private const val SheetDismissMillis = 300L
