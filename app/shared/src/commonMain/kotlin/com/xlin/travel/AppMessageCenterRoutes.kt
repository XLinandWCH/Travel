package com.xlin.travel

import androidx.compose.runtime.Composable
import com.xlin.travel.setting.BroadcastComposeScreen
import com.xlin.travel.setting.BroadcastDetailScreen
import com.xlin.travel.setting.FeedbackComposeScreen
import com.xlin.travel.setting.FeedbackDetailScreen
import com.xlin.travel.setting.MessageCenterScreen

/** 消息中心侧路由：工单列表 / 新建反馈 / 反馈详情 / 公告发布 / 公告详情 */
@Composable
internal fun AppHost.MessageCenterRoutes() {
    when (screen) {
        AppScreen.MessageCenter -> MessageCenterList()

        AppScreen.FeedbackDetail -> FeedbackDetailScreen(
            session = session,
            ticketId = detailTicketId,
            onBack = { screen = AppScreen.MessageCenter },
        )

        AppScreen.FeedbackCompose -> FeedbackComposeScreen(
            session = session,
            onBack = { screen = AppScreen.MessageCenter },
            onCreated = { ticket ->
                // 提交成功带回工单号则进入该工单详情；异常空值回列表兼容
                if (ticket.isNotBlank()) {
                    detailTicketId = ticket
                    screen = AppScreen.FeedbackDetail
                } else {
                    screen = AppScreen.MessageCenter
                }
            },
        )

        AppScreen.BroadcastCompose -> BroadcastComposeScreen(
            session = session,
            onBack = { screen = AppScreen.MessageCenter },
            onPublished = { screen = AppScreen.MessageCenter },
        )

        AppScreen.BroadcastDetail -> {
            val target = selectedBroadcast
            if (target != null) {
                BroadcastDetailScreen(
                    item = target,
                    onBack = { screen = AppScreen.MessageCenter },
                )
            } else {
                // 无选中公告（例如进程恢复）：渲染消息中心兼容，避免在组合期修改状态
                MessageCenterList()
            }
        }

        else -> Unit
    }
}

/** 消息中心列表：从列表进入各子页时记住工单号 / 公告内容 */
@Composable
private fun AppHost.MessageCenterList() {
    MessageCenterScreen(
        session = session,
        onBack = { screen = AppScreen.Settings },
        onOpenTicket = { id ->
            detailTicketId = id
            screen = AppScreen.FeedbackDetail
        },
        onComposeFeedback = { screen = AppScreen.FeedbackCompose },
        onComposeBroadcast = { screen = AppScreen.BroadcastCompose },
        onOpenBroadcast = { item ->
            selectedBroadcast = item
            screen = AppScreen.BroadcastDetail
        },
        badge = badge,
    )
}
