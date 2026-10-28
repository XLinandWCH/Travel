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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.inputbox.ChatInputState
import com.xlin.travel.tts.TtsController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 聊天界面：展示用户与 AI 的消息列表，采用主流 AI 应用的「智能吸底」滚动
 * （判定逻辑见 [rememberChatScrollFollow]）：
 * 在底部时跟随流式输出持续下滚；用户向上滑动则打断跟随、保持视口不动，
 * 并在底部还有内容没露出来时浮现「回到底部」悬浮按钮（[ScrollToBottomButton]），
 * 点击一路滚到最新并恢复跟随；回到底部 / 发新消息 / 切会话自动恢复跟随。
 *
 * 全新对话（消息为空）时，居中随机显示一条欢迎语。
 * 每一条 AI 回答输出结束后，正文下方都会出现赞同 / 不赞同 / 重新生成 / 复制四个按钮
 * （正在流式输出的那一条除外）；其中「重新生成」只在最后一条 AI 回答上可用，
 * 顶部提示（[TopToast]）与点踩后的反馈面板（[FeedbackBottomSheet]）：面板是真正的底部面板
 * （ModalBottomSheet，独立窗口层），提示仍画在聊天区之上。
 *
 * 长按用户消息弹出 [UserMessageContextMenu]（与抽屉历史项同款的上下文菜单）：
 * 「编辑」只给最新一条用户消息——点击后原文预填输入框、原消息原地保留，
 * 直到用户再次「发送」时 ChatState 才删掉原文及其后全部历史；中途清空输入框 / 切换会话自动取消；
 * 「删除」先弹 [ConfirmDeleteMessageDialog] 确认，确认后把这条用户消息和它对应的
 * AI 回答作为一组一起删掉（见 [ChatState.deleteMessage]）。
 */
@Composable
fun ChatScreen(
    state: ChatState,
    inputState: ChatInputState,
    tts: TtsController,
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
    val lastIndex = state.messages.lastIndex

    // 长按用户消息弹出的上下文菜单：非空 = 菜单展开且记录目标消息
    var actionTarget by remember { mutableStateOf<ChatMessage?>(null) }
    // 菜单里点「删除」后等待确认弹窗答复的目标消息
    var pendingDelete by remember { mutableStateOf<ChatMessage?>(null) }
    // 只有「最新一条用户消息」可编辑；流式输出中不提供编辑入口
    val lastUserMessage = state.messages.lastOrNull { it.role == MessageRole.User }

    // 智能吸底：跟随意图 + 按钮可见性 + 用户上翻意图，都收敛在 ChatScrollFollow 里
    val follow = rememberChatScrollFollow(listState, state)

    // 编辑重发的取消条件：输入框从非空被清空 → 视为放弃编辑，原对话毫发无损
    var prevInput by remember { mutableStateOf(inputState.text) }
    LaunchedEffect(inputState.text) {
        if (prevInput.isNotEmpty() && inputState.text.isEmpty()) state.cancelEdit()
        prevInput = inputState.text
    }

    // 切换会话时收起可能残留的反馈面板、菜单、确认弹窗与待发送的编辑，并复位朗读
    LaunchedEffect(state.conversationId) {
        sheet.dismiss()
        actionTarget = null
        pendingDelete = null
        state.cancelEdit()
        tts.resetForConversation()
    }

    // 自动朗读：只在「本轮流式刚结束」的边沿触发（打开历史会话不算新一轮生成）；
    // 开关关闭时 autoRead 内部直接返回。新一轮流式开始时本效应被取消，
    // 连带取消正在合成 / 播放的朗读任务，图标在 read 的 finally 里复位。
    var wasStreaming by remember { mutableStateOf(state.isStreaming) }
    LaunchedEffect(state.isStreaming) {
        val finishedRound = wasStreaming && !state.isStreaming
        wasStreaming = state.isStreaming
        if (!finishedRound) return@LaunchedEffect
        val latest = state.messages.lastOrNull { it.role == MessageRole.Assistant }
        runCatching { tts.autoRead(latest) }
            .onFailure { if (it !is CancellationException) toast.show(it.message ?: "朗读失败") }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            // follow.intentModifier() 只观察手指竖向位移、不消费事件，列表滚动不受影响
            modifier = Modifier.fillMaxSize().then(follow.intentModifier()),
            contentPadding = PaddingValues(start = 16.dp, top = 64.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(state.messages, key = { _, message -> message.id }) { index, message ->
                when (message.role) {
                    MessageRole.User -> UserMessageBubble(
                        message = message,
                        onLongPress = { actionTarget = message },
                        menuContent = {
                            UserMessageContextMenu(
                                expanded = actionTarget === message,
                                canEdit = message === lastUserMessage && !state.isStreaming,
                                onDismissRequest = { actionTarget = null },
                                onEdit = {
                                    // 只预填输入框并记住目标，原消息与后续历史原地不动，发送时才回删
                                    actionTarget = null
                                    state.startEdit(message)
                                    inputState.onTextChange(message.content)
                                    if (message.imageBitmap != null || message.imagePath != null) {
                                        inputState.onImagePicked(message.imageBitmap, message.imagePath)
                                    }
                                },
                                onCopy = {
                                    actionTarget = null
                                    writeClipboard(message.content)
                                    toast.show("复制成功")
                                },
                                onDelete = {
                                    // 不立即删除：先弹确认弹窗
                                    actionTarget = null
                                    pendingDelete = message
                                },
                            )
                        },
                    )
                    MessageRole.Assistant -> AssistantMessageBlock(
                        message = message,
                        // 每条 AI 回答都展示动作条，仅隐藏正在流式输出的最后一条
                        showActions = !(state.isStreaming && index == lastIndex),
                        // 重新生成只会重跑最后一条回答，因此仅在它上面可用
                        canRegenerate = index == lastIndex && !state.isStreaming,
                        // 思考阶段：正在流式的最后一条。工具调用前后模型常会先输出一段文本，
                        // 若用「正文为空」判定会让卡片在工具调用时过早塌成“已深度思考”且不再恢复；
                        // 思考/工具是贯穿整轮的过程，故仅在流式期间保持“思考中…”，整轮结束才折叠
                        isThinking = state.isStreaming && index == lastIndex,
                        isReading = tts.isReading(message),
                        onLike = {
                            state.markFeedback(message, Feedback.Like)
                            toast.show("感谢你的反馈！")
                        },
                        onDislike = {
                            state.markFeedback(message, Feedback.Dislike)
                            sheet.show()
                        },
                        onRegenerate = { scope.launch { state.regenerate() } },
                        onRead = {
                            scope.launch {
                                runCatching { tts.toggleRead(message) }
                                    .onFailure { if (it !is CancellationException) toast.show(it.message ?: "朗读失败") }
                            }
                        },
                        onCopy = {
                            // 只复制正文，思考内容不复制
                            writeClipboard(message.content)
                            toast.show("复制成功")
                        },
                    )
                }
            }
        }

        // 底部还有内容没露出来、且当前不处于吸底跟随中时浮现（见 [ChatScrollFollow.buttonVisible]）：
        // 点击一路滚到真正底部并恢复吸底跟随；
        // 橙色（底环 + 旋转尾迹 + 图标）只在 AI 输出中出现，空闲态零橙色
        ScrollToBottomButton(
            visible = follow.buttonVisible,
            spinning = state.isStreaming,
            onClick = follow::scrollToBottom,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
        )

        // 删除确认弹窗：与历史会话删除同款，确认后删掉这条用户消息与它的 AI 回答
        pendingDelete?.let { msg ->
            ConfirmDeleteMessageDialog(
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    pendingDelete = null
                    scope.launch {
                        state.deleteMessage(msg)
                        toast.show("已删除")
                    }
                },
            )
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
