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
 * 工单详情页（从消息中心点击某条反馈进入）：聊天式一问一答，不使用底部抽屉。
 * - 顶栏「用户反馈」+ 右侧「工单状态」按钮：普通用户点开=确认解决+评分弹窗；管理员点开=选择状态弹窗；
 * - 会话区：用户消息靠右、管理员消息靠左，均附时间，历史回复逐条堆叠（同一条线程内）；
 * - 底部输入条：管理员可直接发消息给用户、用户可追问，像聊天一样发送后即时刷新。
 * 数据来自服务端 /api/feedback/detail，鉴权 token，服务端再校验归属 / 角色。
 */
@Composable
fun FeedbackDetailScreen(
    session: UserSession,
    ticketId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toast = remember { ChatToastState() }
    val scope = rememberCoroutineScope()
    val admin = session.isAdmin

    var data by remember { mutableStateOf<FeedbackDetailResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var showResolve by remember { mutableStateOf(false) }
    var showStatus by remember { mutableStateOf(false) }
    var replyDraft by remember { mutableStateOf("") }

    suspend fun reload() {
        loading = true
        when (val result = MessageApi.detail(session.token, ticketId)) {
            is MessageDetailResult.Success -> {
                data = result.data
                loadError = null
            }
            is MessageDetailResult.Failed -> loadError = result.message
        }
        loading = false
    }
    LaunchedEffect(ticketId) { reload() }

    val detail = data

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        DetailTopBar(
            onBack = onBack,
            onStatusClick = {
                if (detail == null) return@DetailTopBar
                when {
                    admin -> showStatus = true
                    detail.mine -> showResolve = true
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
        ) {
            when {
                loading && detail == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 48.dp),
                        color = AccentOrange,
                        strokeWidth = 2.dp,
                    )
                }
                detail == null -> {
                    // 拉取失败也要给出可见反馈，避免“点进去什么都没有”
                    Text(
                        text = loadError ?: "无法加载该工单",
                        color = TextGray500,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 48.dp),
                    )
                }
                else -> {
                    TicketIdRow(ticketId = detail.ticketId)
                    Spacer(Modifier.height(2.dp))
                    // 工单首条：用户提交的反馈原文（归属用户一侧）
                    ThreadBubble(mine = true, content = detail.content, createdAt = detail.createdAt)
                    // 同一线程内的往来消息，按时间正序，一问一答式堆叠
                    detail.messages.forEach { msg ->
                        ThreadBubble(
                            mine = msg.senderRole != "admin",
                            content = msg.content,
                            createdAt = msg.createdAt,
                        )
                    }
                    if (detail.rating != null && detail.rating > 0) {
                        Spacer(Modifier.height(10.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(text = "用户评分", color = TextGray500, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            StarRatingRow(rating = detail.rating)
                        }
                    }
                }
            }
        }

        // 底部发送条：管理员发消息给用户 / 用户追问（真人回复，无停止按钮）；详情就绪后才显示
        if (detail != null) {
            TicketInputBar(
                value = replyDraft,
                onValueChange = { replyDraft = it },
                placeholder = if (admin) "回复用户…" else "补充说明…",
                modifier = Modifier.imePadding(),
                onSend = {
                    val text = replyDraft.trim()
                    if (text.isEmpty()) return@TicketInputBar
                    scope.launch {
                        val result = if (admin) {
                            MessageApi.reply(session.token, ticketId, text)
                        } else {
                            MessageApi.userReply(session.token, ticketId, text)
                        }
                        when (result) {
                            is MessageMutationResult.Done -> {
                                replyDraft = ""
                                toast.show(result.message)
                                reload()
                            }
                            is MessageMutationResult.Submitted -> toast.show(result.message)
                            is MessageMutationResult.Failed -> toast.show(result.message)
                        }
                    }
                },
            )
        }
    }

    // 用户：确认解决 + 评分
    if (showResolve) {
        ResolveDialog(
            onDismiss = { showResolve = false },
            onConfirm = { resolved, rating ->
                showResolve = false
                scope.launch {
                    when (val r = MessageApi.resolve(session.token, ticketId, resolved, rating)) {
                        is MessageMutationResult.Done -> {
                            toast.show(r.message)
                            reload()
                        }
                        else -> toast.show("操作失败")
                    }
                }
            },
        )
    }

    // 管理员：选择工单状态
    if (showStatus) {
        StatusPickDialog(
            current = data?.status ?: "submitted",
            onDismiss = { showStatus = false },
            onSelect = { status ->
                showStatus = false
                scope.launch {
                    when (val r = MessageApi.adminStatus(session.token, ticketId, status)) {
                        is MessageMutationResult.Done -> {
                            toast.show(r.message)
                            reload()
                        }
                        else -> toast.show("状态更新失败")
                    }
                }
            },
        )
    }

    TopToast(toast, modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}
