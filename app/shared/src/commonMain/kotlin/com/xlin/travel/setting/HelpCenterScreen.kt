package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlin.travel.chat_screen.ChatToastState
import com.xlin.travel.chat_screen.TopToast
import com.xlin.travel.chat_screen.rememberClipboardWriter
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.login.UserSession
import com.xlin.travel.navigation.PageTopBar
import com.xlin.travel.sheet.MultilineInputField
import com.xlin.travel.sheet.SheetSubmitButton
import com.xlin.travel.sheet.TravelBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 帮助中心子页面（与「账号控制」「主题颜色」一样是独立页面）：
 * 常见问题手风琴 + 底部「意见反馈」入口，意见反馈本身仍是真正的底部面板；
 * 提交后经 /api/feedback/submit 写入云端 feedback_tickets，服务端生成 #7 位工单号，
 * 管理员在消息中心回复，用户在消息中心查看回复（需已登录）。
 */
@Composable
fun HelpCenterScreen(
    session: UserSession,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toast = remember { ChatToastState() }
    val writeClipboard = rememberClipboardWriter()
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf<String?>(null) }
    var feedbackOpen by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    fun submitFeedback() {
        if (!session.loggedIn) {
            toast.show("请先登录后再提交反馈")
            return
        }
        if (submitting) return
        submitting = true
        val content = draft.trim()
        scope.launch {
            when (val result = MessageApi.submit(session.token, content)) {
                is MessageMutationResult.Submitted -> {
                    draft = ""
                    feedbackOpen = false
                    // 面板是独立窗口层，会遮住页面顶部的提示，因此等它收起后再弹
                    delay(SheetDismissMillis)
                    toast.show("反馈已提交，工单号 ${result.ticketId}")
                }
                is MessageMutationResult.Failed -> toast.show(result.message)
                is MessageMutationResult.Done -> toast.show(result.message)
            }
            submitting = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = "帮助中心", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            HelpFaqs.items.forEachIndexed { index, item ->
                if (index > 0) FaqDivider()
                FaqRow(
                    item = item,
                    expanded = expanded == item.question,
                    onToggle = { expanded = if (expanded == item.question) null else item.question },
                    onCopy = { text ->
                        writeClipboard(text)
                        toast.show("邮箱已复制")
                    },
                )
            }
        }

        SheetSubmitButton(enabled = true, text = "意见反馈", onClick = { feedbackOpen = true })
    }

    // 意见反馈：真正的底部面板（Material3 ModalBottomSheet）
    TravelBottomSheet(
        visible = feedbackOpen,
        onDismiss = { feedbackOpen = false },
        title = "意见反馈",
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            MultilineInputField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = HelpFaqs.FeedbackPlaceholder,
                minHeight = 160.dp,
                maxHeight = 300.dp,
            )
        }
        SheetSubmitButton(
            enabled = draft.isNotBlank() && !submitting,
            text = if (submitting) "提交中…" else "提交反馈",
            onClick = ::submitFeedback,
        )
    }

    TopToast(toast, modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}

/** 底部面板的收起动画时长 */
private const val SheetDismissMillis = 300L
