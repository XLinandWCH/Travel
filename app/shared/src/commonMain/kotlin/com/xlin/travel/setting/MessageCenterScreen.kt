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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xlin.travel.chat_screen.ChatToastState
import com.xlin.travel.chat_screen.TopToast
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.login.UserSession
import com.xlin.travel.navigation.PageTopBar
import com.xlin.travel.sheet.MultilineInputField
import com.xlin.travel.sheet.SheetSubmitButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.admin

/**
 * 消息中心子页面（设置 → 应用 进入）：
 * - 普通用户：「我的反馈」看自己提交的反馈（点条目进入详情回复 / 查看管理员回复）、「公告」看管理员公告；
 * - 管理员（users.type=admin）：「用户反馈」点某工单进入详情像聊天一样回复、「公告」页签内联发布。
 * 不再使用任何底部抽屉（drawer）；回复统一在工单详情页完成。数据来自服务端 /api/feedback（token 鉴权）。
 */
@Composable
fun MessageCenterScreen(
    session: UserSession,
    onBack: () -> Unit,
    onOpenTicket: (String) -> Unit,
    badge: MessageBadgeState,
    modifier: Modifier = Modifier,
) {
    val toast = remember { ChatToastState() }
    val scope = rememberCoroutineScope()
    val admin = session.isAdmin

    var tab by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<MessageListResponse?>(null) }
    var loading by remember { mutableStateOf(true) }

    // 管理员公告内联编辑（不使用抽屉）：是否展开编辑 + 标题 / 正文
    var composing by remember { mutableStateOf(false) }
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
    LaunchedEffect(session.token, admin) {
        reload()
        // 进入消息中心即视为已读：推送水位线并清除全局红点
        badge.markReadAndClear(session.token)
    }

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
                        onClick = { onOpenTicket(item.ticketId) },
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
                // 管理员专属：公告页签底部内联编辑区（不使用抽屉）
                if (admin) {
                    if (composing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SettingCardBackground)
                                .padding(16.dp),
                        ) {
                            MultilineInputField(
                                value = broadcastTitle,
                                onValueChange = { broadcastTitle = it },
                                placeholder = "公告标题",
                                minHeight = 44.dp,
                                maxHeight = 80.dp,
                            )
                            Spacer(Modifier.height(10.dp))
                            MultilineInputField(
                                value = broadcastBody,
                                onValueChange = { broadcastBody = it },
                                placeholder = "公告正文",
                                minHeight = 120.dp,
                                maxHeight = 240.dp,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        SheetSubmitButton(
                            enabled = broadcastTitle.isNotBlank() && broadcastBody.isNotBlank(),
                            text = "发布公告",
                            onClick = {
                                scope.launch {
                                    when (val result = MessageApi.broadcast(
                                        session.token, broadcastTitle.trim(), broadcastBody.trim()
                                    )) {
                                        is MessageMutationResult.Done -> {
                                            composing = false
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
                        SheetSubmitButton(
                            enabled = true,
                            text = "取消",
                            onClick = {
                                composing = false
                                broadcastTitle = ""
                                broadcastBody = ""
                            },
                        )
                    } else {
                        SheetSubmitButton(text = "发布公告", enabled = true, onClick = { composing = true })
                    }
                }
            }
        }
    }

    TopToast(toast, modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}
