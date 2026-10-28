package com.xlin.travel

import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import com.xlin.travel.chat_screen.MainChatContent
import com.xlin.travel.drawer.ChatDrawer
import com.xlin.travel.guest.GuestScreen
import com.xlin.travel.login.LoginScreen
import com.xlin.travel.search.SearchScreen
import kotlinx.coroutines.launch

/**
 * 聊天侧路由：主聊天（含历史抽屉）、未登入界面、登入页、消息搜索。
 * 与 [AppHost.SettingsRoutes] / [AppHost.MessageCenterRoutes] / [AppHost.MiniProgramRoutes] 互斥，
 * 同一时刻只有匹配当前 [AppHost.screen] 的那一个分支产出界面。
 */
@Composable
internal fun AppHost.ChatRoutes() {
    when (screen) {
        AppScreen.Chat -> ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ChatDrawer(
                    session = session,
                    history = history,
                    hasUnreadMessage = badge.hasUnread,
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
                    onOpenSearch = { openFromDrawer(AppScreen.Search) },
                    onOpenSettings = { openFromDrawer(AppScreen.Settings) },
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
                hasUnreadMessage = badge.hasUnread,
                // 输入框上方快捷条 / 面板点到某个小程序：记住名字再进入它的占位页
                onOpenMiniProgram = { name ->
                    currentProgram = name
                    screen = AppScreen.MiniProgramDetail
                },
                // 聊天顶栏「地图」按钮：进入定位功能演示页
                onOpenMap = { screen = AppScreen.Map },
                onMenuClick = {
                    scope.launch {
                        history.refresh()
                        drawerState.open()
                    }
                },
                onSend = { text ->
                    // 同步读取已选图片/文档的路径与缩略图，避免 onSent() 清空后再去协程里读到 null
                    val imagePath = inputState.pickedImagePath
                    val image = inputState.pickedImage
                    val documentPath = inputState.pickedDocumentPath
                    val documentName = inputState.pickedDocumentName
                    scope.launch { chatState.send(text, imagePath, image, documentPath, documentName) }
                },
            )
        }

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
            onSelectItem = { hit ->
                scope.launch {
                    chatState.openConversation(hit.conversationId)
                    history.select(hit.conversationId)
                }
                history.clearSearch()
                screen = AppScreen.Chat
            },
        )

        else -> Unit
    }
}
