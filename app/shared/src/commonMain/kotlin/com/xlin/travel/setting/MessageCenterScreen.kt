package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.admin

/**
 * 消息中心子页面（设置 → 应用 进入）：
 * - 普通用户：「用户反馈」看自己提交的反馈（点条目进入详情回复 / 查看管理员回复）、「公告」看管理员公告（点条目进入详情查看）；
 * - 管理员（users.type=admin）：「用户反馈」点工单进入详情像聊天一样回复、「公告」长按条目可删除。
 * 右下角悬浮「+」按钮是新建入口：用户反馈页签点击进入反馈撰写页（普通用户与管理员都可），
 * 公告页签仅管理员可见点击进入公告撰写页（支持 Markdown，含预览 / 发布）。数据来自服务端 /api/feedback（token 鉴权）。
 */
@Composable
fun MessageCenterScreen(
    session: UserSession,
    onBack: () -> Unit,
    onOpenTicket: (String) -> Unit,
    onComposeFeedback: () -> Unit,
    onComposeBroadcast: () -> Unit,
    onOpenBroadcast: (BroadcastDto) -> Unit,
    badge: MessageBadgeState,
    modifier: Modifier = Modifier,
) {
    val toast = remember { ChatToastState() }
    val scope = rememberCoroutineScope()
    val admin = session.isAdmin

    var tab by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<MessageListResponse?>(null) }
    var loading by remember { mutableStateOf(true) }

    // 公告长按删除：menuBroadcastId 标记哪张卡片展开菜单，pendingDelete 触发确认弹窗
    var menuBroadcastId by remember { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<BroadcastDto?>(null) }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        MessageEmptyHint(if (admin) "暂无用户反馈" else "暂无反馈，点右下角「+」提交")
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
                        BroadcastCard(
                            item = item,
                            adminIcon = painterResource(Res.drawable.admin),
                            canDelete = admin,
                            menuExpanded = menuBroadcastId == item.id,
                            onClick = { onOpenBroadcast(item) },
                            onLongClick = { menuBroadcastId = item.id },
                            onDismissMenu = { menuBroadcastId = null },
                            onDelete = {
                                menuBroadcastId = null
                                pendingDelete = item
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        // 右下角悬浮「+」：反馈页签始终可见；公告页签仅管理员可见（普通用户无发布权限）
        val showFab = tab == 0 || admin
        if (showFab) {
            MessageAddFab(
                onClick = { if (tab == 0) onComposeFeedback() else onComposeBroadcast() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            )
        }

        // 删除公告确认弹窗：确认后才调用接口，成功后刷新列表
        pendingDelete?.let { target ->
            ConfirmDeleteBroadcastDialog(
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    pendingDelete = null
                    scope.launch {
                        when (val result = MessageApi.deleteBroadcast(session.token, target.id)) {
                            is MessageMutationResult.Done -> {
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

        // 顶部居中提示：铺满整页 Box 并最后绘制，避让状态栏由内部胶囊自带 padding 处理
        TopToast(toast, modifier = Modifier.fillMaxSize())
    }
}
