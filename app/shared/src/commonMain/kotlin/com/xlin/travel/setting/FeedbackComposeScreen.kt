package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.chat_screen.ChatToastState
import com.xlin.travel.chat_screen.TopToast
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.TextGray500
import com.xlin.travel.login.UserSession
import com.xlin.travel.navigation.PageTopBar
import kotlinx.coroutines.launch

/**
 * 用户反馈撰写页（消息中心「用户反馈」页签右下角「+」进入，普通用户与管理员通用）：
 * 仿工单详情页的聊天式界面——上方留白区用于展示引导语，底部一条聊天输入框。
 * 点击发送经 /api/feedback/submit 提交，服务端自动生成 #7 位工单号，成功后回调 [onCreated] 进入该工单详情，
 * 管理员在消息中心即可看到并回复。
 */
@Composable
fun FeedbackComposeScreen(
    session: UserSession,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toast = remember { ChatToastState() }
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || submitting) return
        submitting = true
        scope.launch {
            when (val result = MessageApi.submit(session.token, text)) {
                is MessageMutationResult.Submitted -> onCreated(result.ticketId)
                is MessageMutationResult.Failed -> {
                    toast.show(result.message)
                    submitting = false
                }
                is MessageMutationResult.Done -> onCreated("")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageTopBar(title = "用户反馈", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "描述你遇到的问题，发送后会自动生成工单号，管理员回复后可在「用户反馈」中查看。",
                    color = TextGray500,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }
            TicketInputBar(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "请输入反馈内容…",
                modifier = Modifier.imePadding(),
                onSend = ::send,
            )
        }

        TopToast(toast, modifier = Modifier.fillMaxSize())
    }
}
