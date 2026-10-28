package com.xlin.travel

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.AppTheme
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.ThemePreference
import com.xlin.travel.db.SettingKeys
import com.xlin.travel.navigation.AppBackHandler
import kotlinx.coroutines.launch

/**
 * 应用装配入口：创建并持有应用级状态 [AppHost]，跑启动期的一次性 effects
 * （恢复历史 / 主题 / 登录态、未读徽标刷新、系统深浅色同步）与两个返回键处理，
 * 页面渲染按分组拆到同包的 AppChatRoutes / AppSettingsRoutes / AppMessageCenterRoutes /
 * AppMiniProgramRoutes，路由枚举与返回栈映射见 AppNavigation.kt。
 */
@Composable
fun App() {
    MaterialTheme {
        val host = rememberAppHost()
        val systemIsDark = isSystemInDarkTheme()

        // 启动时从数据库恢复历史会话列表、主题偏好与登录态
        LaunchedEffect(host.repository) {
            host.history.refresh()
            host.repository.getSetting(SettingKeys.ThemePreference)?.let { saved ->
                AppTheme.preference = ThemePreference.entries.firstOrNull { it.name == saved }
                    ?: AppTheme.preference
            }
            // 恢复登录态：已登录进主聊天，否则停在未登入界面
            host.session.load(host.repository)
            host.screen = if (host.session.loggedIn) AppScreen.Chat else AppScreen.Guest
            host.appReady = true
            if (host.session.loggedIn) host.badge.refresh(host.session.token)
        }

        // 回到聊天 / 设置页时刷新未读徽标（轻量拉取，驱动菜单 / 抽屉 / 设置行的红点）
        LaunchedEffect(host.screen, host.session.loggedIn) {
            if (host.session.loggedIn &&
                (host.screen == AppScreen.Chat || host.screen == AppScreen.Settings)
            ) {
                host.badge.refresh(host.session.token)
            }
        }

        // 跟随系统：把系统深色状态同步给 AppTheme（System 偏好时生效）
        LaunchedEffect(systemIsDark) {
            AppTheme.systemIsDark = systemIsDark
        }

        // 抽屉（历史记录区域）打开时：系统返回键先关闭抽屉，而不是把事件交给系统直接退出应用
        AppBackHandler(enabled = host.drawerState.currentValue == DrawerValue.Open) {
            host.scope.launch { host.drawerState.close() }
        }

        // 系统返回键：子页面按 [AppScreen.systemBackTarget] 逐级回退；
        // 聊天页与未登入界面为根页，交给系统退出应用；帮助中心面板为独立窗口，返回键由它自己消费
        AppBackHandler(enabled = host.screen != AppScreen.Chat && host.screen != AppScreen.Guest) {
            host.screen = host.screen.systemBackTarget()
        }

        // 背景铺满整屏（绘制到系统栏之下），内容再各自避开系统栏，符合原生 edge-to-edge 观感
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatBackground),
        ) {
            // 登录态尚未从数据库恢复完成：先铺一层中性背景，不渲染未登入 / 聊天页
            if (!host.appReady) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(28.dp),
                    color = AccentOrange,
                    strokeWidth = 2.dp,
                )
                return@Box
            }

            // 四组路由互斥：同一时刻只有匹配 host.screen 的那个分支产出界面
            host.ChatRoutes()
            host.SettingsRoutes()
            host.MessageCenterRoutes()
            host.MiniProgramRoutes()
        }
    }
}
