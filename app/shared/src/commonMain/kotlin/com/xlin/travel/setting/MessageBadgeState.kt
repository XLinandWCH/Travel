package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 消息中心未读徽标状态（进程内单例，由 App 创建并持有）。
 *
 * 服务端以 users.msg_last_read 为水位线给出「相关新消息数量」：管理员看新工单 + 新公告，
 * 普通用户看自己工单的最新回复 + 新公告。进入消息中心（或工单详情）时调用 [markReadAndClear]
 * 把水位线推到当前时间并本地清零，实现「查看后取消红点」。
 */
@Stable
class MessageBadgeState {

    var hasUnread by mutableStateOf(false)
        private set

    /** 拉取一次未读数量并刷新 [hasUnread]；token 为空（未登录）时保持无徽标 */
    suspend fun refresh(token: String) {
        if (token.isBlank()) {
            hasUnread = false
            return
        }
        hasUnread = MessageApi.unread(token) > 0
    }

    /** 标记已读并立即本地清零 */
    suspend fun markReadAndClear(token: String) {
        if (token.isBlank()) return
        MessageApi.markRead(token)
        hasUnread = false
    }

    /** 退出登录时复位 */
    fun reset() {
        hasUnread = false
    }
}

/**
 * 圆点徽标：小红点，叠加在图标右上角（或任意锚点）。
 * [visible] 为 false 时不渲染，避免占位。
 */
@Composable
fun UnreadBadgeDot(
    visible: Boolean,
    modifier: Modifier = Modifier,
    dotSize: androidx.compose.ui.unit.Dp = 9.dp,
) {
    if (!visible) return
    Box(
        modifier = modifier
            .size(dotSize)
            .clip(CircleShape)
            .background(BadgeRed),
    )
}

/** 徽标红：两套主题一致的提示红 */
private val BadgeRed = Color(0xFFFF3B30)
