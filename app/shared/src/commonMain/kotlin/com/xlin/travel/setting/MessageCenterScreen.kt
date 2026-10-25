package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import com.xlin.travel.chat_screen.ChatToastState
import com.xlin.travel.chat_screen.TopToast
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.login.UserSession
import com.xlin.travel.navigation.PageTopBar
import com.xlin.travel.sheet.MultilineInputField
import com.xlin.travel.sheet.SheetSubmitButton
import com.xlin.travel.sheet.TravelBottomSheet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.admin

/**
 * 消息中心子页面（设置 → 应用 进入）：
 * - 普通用户：查看管理员公告 + 自己提交的反馈及管理员回复（默认只收不发，反馈入口在帮助中心）；
 * - 管理员（users.type=admin）：查看全部用户反馈并逐条回复，可发布公告。
 * 数据一律来自服务端 /api/feedback（token 鉴权，服务端再校验管理员角色）。
 */
@Composable
fun MessageCenterScreen(
    session: UserSession,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toast = remember { ChatToastState() }
    val scope = rememberCoroutineScope()
    val admin = session.isAdmin

    var tab by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<MessageListResponse?>(null) }
    var loading by remember { mutableStateOf(true) }

    // 管理员回复面板：当前回复的工单 + 草稿
    var replyTarget by remember { mutableStateOf<FeedbackDto?>(null) }
    var replyDraft by remember { mutableStateOf("") }
    // 管理员公告面板：标题 + 正文
    var broadcastOpen by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastBody by remember { mutableStateOf("") }

    // 拉取列表：管理员看全量反馈，普通用户看自己的收件箱
    suspend fun reload() {
        loading = true
        when (val result = if (admin) MessageApi.adminList(session.token) else MessageApi.inbox(session.token)) {
            is MessageListResult.Success -> data = result.data
            is MessageListResult.Failed -> toast.show(result.message)
        }
        loading = false
    }
    LaunchedEffect(session.token, admin) { reload() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = if (admin) "消息中心（管理员）" else "消息中心", onBack = onBack)
        MessageTabRow(
            tabs = listOf(if (admin) "用户反馈" else "我的反馈", "公告"),
            selected = tab,
            onSelect = { tab = it },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val list = data
            if (loading && list == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 48.dp),
                    color = AccentOrange,
                    strokeWidth = 2.dp,
                )
            } else if (tab == 0) {
                val feedbacks = list?.feedbacks.orEmpty()
                if (feedbacks.isEmpty()) {
                    MessageEmptyHint(if (admin) "暂无用户反馈" else "暂无反馈，可在「帮助中心 → 意见反馈」提交")
                }
                feedbacks.forEach { item ->
                    FeedbackCard(
                        item = item,
                        showUsername = admin,
                        onReply = if (admin) {
                            {
                                replyTarget = item
                                replyDraft = item.reply.orEmpty()
                            }
                        } else null,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                val broadcasts = list?.broadcasts.orEmpty()
                if (broadcasts.isEmpty()) MessageEmptyHint("暂无公告")
                broadcasts.forEach { item ->
                    BroadcastCard(item = item, adminIcon = painterResource(Res.drawable.admin))
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // 管理员专属：公告页签底部提供发布入口
        if (admin && tab == 1) {
            SheetSubmitButton(text = "发布公告", enabled = true, onClick = { broadcastOpen = true })
        }
    }

    // 回复面板：写回指定工单，成功后刷新列表
    TravelBottomSheet(
        visible = replyTarget != null,
        onDismiss = { replyTarget = null },
        title = "回复 ${replyTarget?.ticketId.orEmpty()}",
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            MultilineInputField(
                value = replyDraft,
                onValueChange = { replyDraft = it },
                placeholder = "输入回复内容…",
                minHeight = 120.dp,
                maxHeight = 240.dp,
            )
        }
        SheetSubmitButton(
            enabled = replyDraft.isNotBlank(),
            text = "发送回复",
            onClick = {
                val target = replyTarget ?: return@SheetSubmitButton
                scope.launch {
                    when (val result = MessageApi.reply(session.token, target.ticketId, replyDraft.trim())) {
                        is MessageMutationResult.Done -> {
                            replyTarget = null
                            toast.show(result.message)
                            reload()
                        }
                        is MessageMutationResult.Failed -> toast.show(result.message)
                        is MessageMutationResult.Submitted -> toast.show(result.message)
                    }
                }
            },
        )
    }

    // 公告面板：标题 + 正文
    TravelBottomSheet(
        visible = broadcastOpen,
        onDismiss = { broadcastOpen = false },
        title = "发布公告",
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            MultilineInputField(
                value = broadcastTitle,
                onValueChange = { broadcastTitle = it },
                placeholder = "公告标题",
                minHeight = 44.dp,
                maxHeight = 80.dp,
            )
            MultilineInputField(
                value = broadcastBody,
                onValueChange = { broadcastBody = it },
                placeholder = "公告正文",
                minHeight = 120.dp,
                maxHeight = 240.dp,
            )
        }
        SheetSubmitButton(
            enabled = broadcastTitle.isNotBlank() && broadcastBody.isNotBlank(),
            text = "发布",
            onClick = {
                scope.launch {
                    when (val result = MessageApi.broadcast(session.token, broadcastTitle.trim(), broadcastBody.trim())) {
                        is MessageMutationResult.Done -> {
                            broadcastOpen = false
                            broadcastTitle = ""
                            broadcastBody = ""
                            toast.show(result.message)
                            reload()
                        }
                        is MessageMutationResult.Failed -> toast.show(result.message)
                        is MessageMutationResult.Submitted -> toast.show(result.message)
                    }
                }
            },
        )
    }

    TopToast(toast, modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}
