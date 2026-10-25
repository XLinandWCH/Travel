package com.xlin.travel

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
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
import com.xlin.travel.guest.GuestScreen
import com.xlin.travel.inputbox.ChatInputState
import com.xlin.travel.login.LoginScreen
import com.xlin.travel.login.UserSession
import com.xlin.travel.navigation.AppBackHandler
import com.xlin.travel.search.SearchScreen
import com.xlin.travel.setting.AccountControlScreen
import com.xlin.travel.setting.ComingSoonScreen
import com.xlin.travel.setting.DocumentScreen
import com.xlin.travel.setting.HelpCenterScreen
import com.xlin.travel.setting.HistoryManageScreen
import com.xlin.travel.setting.LegalDoc
import com.xlin.travel.setting.SettingsScreen
import com.xlin.travel.setting.ThemeColorScreen
import kotlinx.coroutines.launch

/** 顶层页面；ThemeColor / ComingSoon / HelpCenter / Document 是设置的子页面，返回时先回到设置列表；
 * AccountControl 亦从设置进入；Guest 为未登入界面，Login 由未登入界面「登入」或退出登录进入。 */
private enum class AppScreen {
    Chat,
    Guest,
    Login,
    Settings,
    Search,
    ThemeColor,
    History,
    ComingSoon,
    HelpCenter,
    Document,
    AccountControl,
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
        val session = remember { UserSession() }
        var screen by remember { mutableStateOf(AppScreen.Guest) }
        // 启动时先不渲染任何业务页面，等数据库异步恢复完登录态再决定去向，
        // 避免已登录用户看到未登入界面一闪而过
        var appReady by remember { mutableStateOf(false) }
        var comingSoonTitle by remember { mutableStateOf("") }
        var legalDoc by remember { mutableStateOf(LegalDoc.Terms) }
        val systemIsDark = isSystemInDarkTheme()

        // 启动时从数据库恢复历史会话列表、主题偏好与登录态
        LaunchedEffect(repository) {
            history.refresh()
            repository.getSetting(SettingKeys.ThemePreference)?.let { saved ->
                AppTheme.preference = ThemePreference.entries.firstOrNull { it.name == saved }
                    ?: AppTheme.preference
            }
            // 恢复登录态：已登录进主聊天，否则停在未登入界面
            session.load(repository)
            screen = if (session.loggedIn) AppScreen.Chat else AppScreen.Guest
            appReady = true
        }

        // 跟随系统：把系统深色状态同步给 AppTheme（System 偏好时生效）
        LaunchedEffect(systemIsDark) {
            AppTheme.systemIsDark = systemIsDark
        }

        // 抽屉（历史记录区域）打开时：系统返回键先关闭抽屉，而不是把事件交给系统直接退出应用
        AppBackHandler(enabled = drawerState.currentValue == DrawerValue.Open) {
            scope.launch { drawerState.close() }
        }

        // 系统返回键：子页面先回设置列表，搜索回聊天页，登录回未登入界面；
        // 聊天页与未登入界面为根页，交给系统退出应用；帮助中心面板为独立窗口，返回键由它自己消费
        AppBackHandler(enabled = screen != AppScreen.Chat && screen != AppScreen.Guest) {
            screen = when (screen) {
                AppScreen.ThemeColor, AppScreen.History, AppScreen.ComingSoon,
                AppScreen.HelpCenter, AppScreen.Document,
                AppScreen.AccountControl -> AppScreen.Settings
                AppScreen.Login -> AppScreen.Guest
                else -> AppScreen.Chat
            }
        }

        // 背景铺满整屏（绘制到系统栏之下），内容再各自避开系统栏，符合原生 edge-to-edge 观感
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatBackground),
        ) {
            // 登录态尚未从数据库恢复完成：先铺一层中性背景，不渲染未登入 / 聊天页
            if (!appReady) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(28.dp),
                    color = com.xlin.travel.color.AccentOrange,
                    strokeWidth = 2.dp,
                )
                return@Box
            }

            when (screen) {
                AppScreen.Chat -> ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ChatDrawer(
                            session = session,
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
                            onDeleteConversation = { id ->
                                scope.launch {
                                    history.delete(id)
                                    // 删的是当前打开的会话时，重置为全新对话
                                    if (chatState.conversationId == id) chatState.startNewChat()
                                }
                            },
                            onRenameConversation = { id, newTitle ->
                                scope.launch { history.rename(id, newTitle) }
                            },
                        )
                    },
                ) {
                    MainChatContent(
                        inputState = inputState,
                        chatState = chatState,
                        repository = repository,
                        onMenuClick = {
                            scope.launch {
                                history.refresh()
                                drawerState.open()
                            }
                        },
                        onSend = { text ->
                            // 同步读取已选图片的路径与缩略图，避免 onSent() 清空后再去协程里读到 null
                            val imagePath = inputState.pickedImagePath
                            val image = inputState.pickedImage
                            scope.launch { chatState.send(text, imagePath, image) }
                        },
                    )
                }

                AppScreen.Settings -> SettingsScreen(
                    username = session.username,
                    maskedPhone = session.maskedPhone,
                    email = session.email,
                    onBack = { screen = AppScreen.Chat },
                    onLogout = {
                        scope.launch {
                            session.signOut(repository)
                            screen = AppScreen.Guest
                        }
                    },
                    onOpenThemeColor = { screen = AppScreen.ThemeColor },
                    onOpenHistory = { screen = AppScreen.History },
                    onOpenHelpCenter = { screen = AppScreen.HelpCenter },
                    onOpenDoc = { doc ->
                        legalDoc = doc
                        screen = AppScreen.Document
                    },
                    onOpenAccountControl = { screen = AppScreen.AccountControl },
                )

                AppScreen.AccountControl -> AccountControlScreen(
                    session = session,
                    repository = repository,
                    onBack = { screen = AppScreen.Settings },
                    onAccountDeleted = {
                        // 会话与本地数据已在弹窗内清空：重置当前聊天并刷新历史，回到最初的未登入界面
                        chatState.startNewChat()
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
                    onBack = { screen = AppScreen.Settings },
                )

                AppScreen.Document -> DocumentScreen(
                    doc = legalDoc,
                    onBack = { screen = AppScreen.Settings },
                )

                AppScreen.Guest -> GuestScreen(
                    inputState = inputState,
                    onLogin = { screen = AppScreen.Login },
                )

                AppScreen.Login -> LoginScreen(
                    onSkip = { screen = AppScreen.Guest },
                    onLoggedIn = { user, token ->
                        scope.launch {
                            session.signIn(user, token, repository)
                            screen = AppScreen.Chat
                        }
                    },
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
