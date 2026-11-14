package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.chat_screen.ChatToastState
import com.xlin.travel.chat_screen.TopToast
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.TextGray500
import com.xlin.travel.image_picker.rememberAvatarPicker
import com.xlin.travel.login.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 客服服务（扫码页"客服"按钮进入），对标微信 / Telegram 的左右聊天：
 * - 普通用户：与客服聊天，可发文字与图片;客服的全局通知对所有人可见（一视同仁）;
 * - 管理员：会话列表含置顶「全局通知」（向所有用户发布）与各客户会话，点选后查看并回复。
 * 「新建对话」仅清空当前屏幕（本地边界，不写历史），之后只展示新消息。
 */
@Composable
fun CustomerServiceScreen(
    session: UserSession,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toast = remember { ChatToastState() }
    val scope = rememberCoroutineScope()
    val admin = session.isAdmin

    var conversations by remember { mutableStateOf<List<CustomerConversationDto>?>(null) }
    var selectedUsername by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<CustomerMessageDto>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var pendingImage by remember { mutableStateOf<String?>(null) }
    var pendingPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    var sending by remember { mutableStateOf(false) }
    var clearBoundary by remember { mutableStateOf<String?>(null) }

    val pickImage = rememberAvatarPicker { data ->
        if (data != null) {
            pendingImage = data.base64
            pendingPreview = data.preview
        }
    }

    suspend fun loadMessages() {
        when (val result = CustomerServiceApi.getMessages(session.token, if (admin) selectedUsername else null)) {
            is CustomerServiceResult.Messages -> {
                messages = result.data.messages
                loadError = null
            }
            is CustomerServiceResult.Failed -> if (messages == null) loadError = result.message
            else -> {}
        }
        loading = false
    }

    suspend fun loadConversations() {
        when (val result = CustomerServiceApi.getConversations(session.token)) {
            is CustomerServiceResult.Conversations -> {
                conversations = result.data.conversations
                loadError = null
            }
            is CustomerServiceResult.Failed -> loadError = result.message
            else -> {}
        }
        loading = false
    }

    val inConversationList = admin && selectedUsername == null
    val isGlobalChat = admin && selectedUsername == CUSTOMER_GLOBAL_USERNAME

    LaunchedEffect(admin, selectedUsername) {
        loading = true
        loadError = null
        clearBoundary = null
        if (inConversationList) {
            loadConversations()
            while (isActive) {
                delay(8000)
                loadConversations()
            }
        } else {
            messages = null
            loadMessages()
            while (isActive) {
                delay(5000)
                loadMessages()
            }
        }
    }

    fun newChat() {
        clearBoundary = messages.orEmpty().maxOfOrNull { it.createdAt }
        draft = ""
        pendingImage = null
        pendingPreview = null
    }

    fun send() {
        val text = draft.trim()
        val image = pendingImage
        if (sending || (text.isEmpty() && image == null)) return
        sending = true
        scope.launch {
            try {
                val result = if (admin) {
                    val target = selectedUsername
                    if (target == null) {
                        toast.show("请先选择要回复的客户")
                        return@launch
                    }
                    CustomerServiceApi.adminReply(session.token, target, text, image)
                } else {
                    CustomerServiceApi.send(session.token, text, image)
                }
                when (result) {
                    is CustomerServiceMutationResult.Done -> {
                        draft = ""
                        pendingImage = null
                        pendingPreview = null
                        loadMessages()
                    }
                    is CustomerServiceMutationResult.Failed -> toast.show(result.message)
                }
            } finally {
                sending = false
            }
        }
    }

    val boundary = clearBoundary
    val visibleMessages = messages.orEmpty().filter { boundary == null || it.createdAt > boundary }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        CustomerServiceTopBar(
            onBack = {
                if (admin && selectedUsername != null) {
                    selectedUsername = null
                    messages = null
                } else {
                    onBack()
                }
            },
            title = "客服服务",
            subtitle = when {
                isGlobalChat -> "全局通知 · 所有用户可见"
                admin -> selectedUsername
                else -> null
            },
            onNewChat = if (inConversationList) null else ::newChat,
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                loading && (if (inConversationList) conversations == null else messages == null) -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).padding(top = 48.dp),
                        color = AccentOrange,
                        strokeWidth = 2.dp,
                    )
                }
                loadError != null && (if (inConversationList) conversations.isNullOrEmpty() else messages.isNullOrEmpty()) -> {
                    Text(
                        text = loadError ?: "加载失败",
                        color = TextGray500,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                inConversationList -> {
                    val list = conversations.orEmpty()
                    if (list.isEmpty()) {
                        Text(
                            text = "暂无用户会话，等待用户发起咨询",
                            color = TextGray500,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(list, key = { it.username }) { conv ->
                                CustomerConversationItem(
                                    username = conv.username,
                                    lastMessage = conv.lastMessage,
                                    lastMessageAt = conv.lastMessageAt,
                                    isGlobal = conv.username == CUSTOMER_GLOBAL_USERNAME,
                                    onClick = { selectedUsername = conv.username },
                                )
                            }
                        }
                    }
                }
                else -> {
                    if (visibleMessages.isEmpty()) {
                        Text(
                            text = if (isGlobalChat) "还没有全局通知，发送内容即向所有用户发布"
                            else "暂无消息，发送内容即可联系客服",
                            color = TextGray500,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(visibleMessages.size) {
                            if (visibleMessages.isNotEmpty()) listState.animateScrollToItem(visibleMessages.size - 1)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(visibleMessages) { msg ->
                                CustomerServiceBubble(
                                    mine = msg.senderRole != "admin",
                                    content = msg.content,
                                    imageData = msg.imageData,
                                    createdAt = msg.createdAt,
                                    userAvatar = session.avatar,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!inConversationList) {
            pendingPreview?.let { preview ->
                CustomerImagePreview(
                    preview = preview,
                    onRemove = {
                        pendingImage = null
                        pendingPreview = null
                    },
                )
            }
            CustomerServiceInputBar(
                value = draft,
                onValueChange = { draft = it },
                placeholder = when {
                    isGlobalChat -> "发布全局通知（所有用户可见）…"
                    admin -> "回复用户…"
                    else -> "发送消息…"
                },
                onSend = ::send,
                onImageClick = { pickImage() },
                hasImage = pendingImage != null,
                sending = sending,
                modifier = Modifier.imePadding(),
            )
        }
    }

    TopToast(toast, modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}