package com.xlin.travel

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.xlin.travel.chat_screen.ChatState
import com.xlin.travel.chat_screen.MainChatContent
import com.xlin.travel.color.AppTheme
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.ThemePreference
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.SettingKeys
import com.xlin.travel.db.rememberChatDatabase
import com.xlin.travel.drawer.ChatDrawer
import com.xlin.travel.drawer.ChatHistoryState
import com.xlin.travel.inputbox.ChatInputState
import com.xlin.travel.navigation.AppBackHandler
import com.xlin.travel.search.SearchScreen
import com.xlin.travel.setting.ComingSoonScreen
import com.xlin.travel.setting.DocumentScreen
import com.xlin.travel.setting.HelpCenterScreen
import com.xlin.travel.setting.LegalDoc
import com.xlin.travel.setting.SettingsScreen
import com.xlin.travel.setting.ThemeColorScreen
import kotlinx.coroutines.launch

/** 顶层页面；ThemeColor / ComingSoon / HelpCenter / Document 是设置的子页面，返回时先回到设置列表 */
private enum class AppScreen {
    Chat,
    Settings,
    Search,
    ThemeColor,
    ComingSoon,
    HelpCenter,
    Document,
}

@Composable
fun App() {
    MaterialTheme {
        val database = rememberChatDatabase()
        val repository = remember(database) { ChatRepository(database) }
        val inputState = remember { ChatInputState() }
        val history = remember(repository) { ChatHistoryState(repository) }
        val chatState = remember(repository) {
            ChatState(repository) { id ->
                history.select(id)
                history.refresh()
            }
        }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var screen by remember { mutableStateOf(AppScreen.Chat) }
        var comingSoonTitle by remember { mutableStateOf("") }
        var legalDoc by remember { mutableStateOf(LegalDoc.Terms) }
        val systemIsDark = isSystemInDarkTheme()

        // 启动时从数据库恢复历史会话列表与主题偏好
        LaunchedEffect(repository) {
            history.refresh()
            repository.getSetting(SettingKeys.ThemePreference)?.let { saved ->
                AppTheme.preference = ThemePreference.entries.firstOrNull { it.name == saved }
                    ?: AppTheme.preference
            }
        }

        // 跟随系统：把系统深色状态同步给 AppTheme（System 偏好时生效）
        LaunchedEffect(systemIsDark) {
            AppTheme.systemIsDark = systemIsDark
        }

        // 系统返回键：子页面先回设置列表，搜索回聊天页，聊天页才交给系统退出应用；
        // 帮助中心页里的意见反馈面板是独立窗口，返回键由它自己消费
        AppBackHandler(enabled = screen != AppScreen.Chat) {
            screen = when (screen) {
                AppScreen.ThemeColor, AppScreen.ComingSoon,
                AppScreen.HelpCenter, AppScreen.Document -> AppScreen.Settings
                else -> AppScreen.Chat
            }
        }

        // 背景铺满整屏（绘制到系统栏之下），内容再各自避开系统栏，符合原生 edge-to-edge 观感
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatBackground),
        ) {
            when (screen) {
                AppScreen.Chat -> ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ChatDrawer(
                            history = history,
                            onNewChat = {
                                chatState.startNewChat()
                                history.newChat()
                            },
                            onSelectConversation = { id ->
                                scope.launch {
                                    chatState.openConversation(id)
                                    history.select(id)
                                }
                            },
                            onCloseDrawer = { scope.launch { drawerState.close() } },
                            onOpenSearch = {
                                scope.launch { drawerState.close() }
                                screen = AppScreen.Search
                            },
                            onOpenSettings = {
                                scope.launch { drawerState.close() }
                                screen = AppScreen.Settings
                            },
                        )
                    },
                ) {
                    MainChatContent(
                        inputState = inputState,
                        chatState = chatState,
                        onMenuClick = {
                            scope.launch {
                                history.refresh()
                                drawerState.open()
                            }
                        },
                        onSend = { text -> scope.launch { chatState.send(text) } },
                    )
                }

                AppScreen.Settings -> SettingsScreen(
                    onBack = { screen = AppScreen.Chat },
                    onOpenThemeColor = { screen = AppScreen.ThemeColor },
                    onOpenHelpCenter = { screen = AppScreen.HelpCenter },
                    onOpenDoc = { doc ->
                        legalDoc = doc
                        screen = AppScreen.Document
                    },
                    onOpenComingSoon = { title ->
                        comingSoonTitle = title
                        screen = AppScreen.ComingSoon
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

                AppScreen.ComingSoon -> ComingSoonScreen(
                    title = comingSoonTitle,
                    onBack = { screen = AppScreen.Settings },
                )

                AppScreen.HelpCenter -> HelpCenterScreen(
                    onBack = { screen = AppScreen.Settings },
                )

                AppScreen.Document -> DocumentScreen(
                    doc = legalDoc,
                    onBack = { screen = AppScreen.Settings },
                )

                AppScreen.Search -> SearchScreen(
                    history = history,
                    onBack = {
                        history.clearSearch()
                        screen = AppScreen.Chat
                    },
                    onSelectItem = { item ->
                        scope.launch {
                            chatState.openConversation(item.id)
                            history.select(item.id)
                        }
                        history.clearSearch()
                        screen = AppScreen.Chat
                    },
                )
            }
        }
    }
}
