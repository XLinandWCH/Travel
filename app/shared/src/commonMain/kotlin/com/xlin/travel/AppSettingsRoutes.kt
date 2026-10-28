package com.xlin.travel

import androidx.compose.runtime.Composable
import com.xlin.travel.color.AppTheme
import com.xlin.travel.db.SettingKeys
import com.xlin.travel.setting.AccountControlScreen
import com.xlin.travel.setting.ComingSoonScreen
import com.xlin.travel.setting.DocumentScreen
import com.xlin.travel.setting.HelpCenterScreen
import com.xlin.travel.setting.HistoryManageScreen
import com.xlin.travel.setting.SettingsScreen
import com.xlin.travel.setting.ThemeColorScreen
import kotlinx.coroutines.launch

/** 设置侧路由：设置列表与其子页面（主题颜色 / 历史管理 / 账号控制 / 帮助中心 / 法务文档 / 即将上线） */
@Composable
internal fun AppHost.SettingsRoutes() {
    when (screen) {
        AppScreen.Settings -> SettingsScreen(
            username = session.username,
            maskedPhone = session.maskedPhone,
            email = session.email,
            onBack = { screen = AppScreen.Chat },
            onLogout = {
                scope.launch {
                    session.signOut(repository)
                    badge.reset()
                    screen = AppScreen.Guest
                }
            },
            onOpenThemeColor = { screen = AppScreen.ThemeColor },
            onOpenMessageCenter = { screen = AppScreen.MessageCenter },
            onOpenHistory = { screen = AppScreen.History },
            onOpenHelpCenter = { screen = AppScreen.HelpCenter },
            onOpenDoc = { doc ->
                legalDoc = doc
                screen = AppScreen.Document
            },
            onOpenAccountControl = { screen = AppScreen.AccountControl },
            hasUnreadMessage = badge.hasUnread,
        )

        AppScreen.AccountControl -> AccountControlScreen(
            session = session,
            repository = repository,
            onBack = { screen = AppScreen.Settings },
            onAccountDeleted = {
                // 会话与本地数据已在弹窗内清空：重置当前聊天并刷新历史，回到最初的未登入界面
                chatState.startNewChat()
                badge.reset()
                scope.launch { history.refresh() }
                screen = AppScreen.Guest
            },
        )

        AppScreen.ThemeColor -> ThemeColorScreen(
            onSelect = { option ->
                // 立即全局生效，同时写入数据库，下次启动保持
                AppTheme.preference = option
                scope.launch { repository.setSetting(SettingKeys.ThemePreference, option.name) }
            },
            onBack = { screen = AppScreen.Settings },
        )

        AppScreen.History -> HistoryManageScreen(
            repository = repository,
            onHistoryCleared = {
                // 历史已从数据库清空：复位当前聊天并刷新抽屉列表，页面留在历史管理
                chatState.startNewChat()
                history.newChat()
                scope.launch { history.refresh() }
            },
            onBack = { screen = AppScreen.Settings },
        )

        AppScreen.ComingSoon -> ComingSoonScreen(
            title = comingSoonTitle,
            onBack = { screen = AppScreen.Settings },
        )

        AppScreen.HelpCenter -> HelpCenterScreen(
            session = session,
            onBack = { screen = AppScreen.Settings },
        )

        AppScreen.Document -> DocumentScreen(
            doc = legalDoc,
            onBack = { screen = AppScreen.Settings },
        )

        else -> Unit
    }
}
