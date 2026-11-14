package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.chat_screen.ChatToastState
import com.xlin.travel.chat_screen.TopToast
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.TextGray500
import com.xlin.travel.login.UserSession
import kotlinx.coroutines.launch

/**
 * 客服服务页面：1:1复制用户反馈页面，去掉工单状态和工单号，添加客服头像。
 * 客服（管理员）回复消息时显示客服头像。
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

    var sessionId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<CustomerServiceMessageDto>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var replyDraft by remember { mutableStateOf("") }

    suspend fun ensureSession() {
        loading = true
        // 首先获取会话列表
        val listResult = CustomerServiceApi.getSessionList(session.token, admin)
        if (listResult is CustomerServiceResult.SessionList) {
            val sessions = listResult.data.sessions
            if (sessions.isNotEmpty()) {
                // 使用最新的会话
                sessionId = sessions.first().sessionId
            } else if (!admin) {
                // 普通用户没有会话，创建新会话
                val createResult = CustomerServiceApi.createSession(session.token)
                if (createResult is CustomerServiceResult.Created) {
                    sessionId = createResult.sessionId
                } else if (createResult is CustomerServiceResult.Failed) {
                    loadError = createResult.message
                }
            }
        } else if (listResult is CustomerServiceResult.Failed) {
            loadError = listResult.message
        }
        loading = false
    }

    suspend fun reloadMessages() {
        sessionId?.let { sid ->
            when (val result = CustomerServiceApi.getMessages(session.token, sid)) {
                is CustomerServiceResult.Success -> {
                    messages = result.data.messages
                    loadError = null
                }
                is CustomerServiceResult.Failed -> loadError = result.message
                else -> {} // Ignore other result types
            }
        }
    }

    LaunchedEffect(Unit) {
        ensureSession()
    }

    LaunchedEffect(sessionId) {
        sessionId?.let { reloadMessages() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        CustomerServiceTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
        ) {
            when {
                loading && messages == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 48.dp),
                        color = AccentOrange,
                        strokeWidth = 2.dp,
                    )
                }
                loadError != null && messages.isNullOrEmpty() -> {
                    Text(
                        text = loadError ?: "加载失败",
                        color = TextGray500,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 48.dp),
                    )
                }
                messages.isNullOrEmpty() -> {
                    Text(
                        text = "暂无消息",
                        color = TextGray500,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 48.dp),
                    )
                }
                else -> {
                    messages?.forEach { msg ->
                        CustomerServiceBubble(
                            mine = msg.senderRole != "admin",
                            content = msg.content,
                            createdAt = msg.createdAt,
                        )
                    }
                }
            }
        }

        CustomerServiceInputBar(
            value = replyDraft,
            onValueChange = { replyDraft = it },
            placeholder = if (admin) "回复用户…" else "发送消息…",
            modifier = Modifier.imePadding(),
            onSend = {
                val text = replyDraft.trim()
                val sid = sessionId
                if (text.isEmpty() || sid == null) return@CustomerServiceInputBar
                scope.launch {
                    val result = if (admin) {
                        CustomerServiceApi.adminReply(session.token, sid, text)
                    } else {
                        CustomerServiceApi.userSend(session.token, sid, text)
                    }
                    when (result) {
                        is CustomerServiceMutationResult.Done -> {
                            replyDraft = ""
                            toast.show(result.message)
                            reloadMessages()
                        }
                        is CustomerServiceMutationResult.Failed -> toast.show(result.message)
                    }
                }
            },
        )
    }

    TopToast(toast, modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}
