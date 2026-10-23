package com.xlin.travel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xlin.travel.chat_screen.ChatScreen
import com.xlin.travel.chat_screen.ChatState
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.MenuButtonBackground
import com.xlin.travel.color.TopBarEdgeFadeBrush
import com.xlin.travel.drawer.ChatDrawer
import com.xlin.travel.drawer.ChatHistoryState
import com.xlin.travel.inputbox.ChatInputBox
import com.xlin.travel.inputbox.ChatInputState
import com.xlin.travel.navigation.AppBackHandler
import com.xlin.travel.search.SearchScreen
import com.xlin.travel.setting.SettingsScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.menu

/** 顶层页面 */
private enum class AppScreen {
    Chat,
    Settings,
    Search,
}

@Composable
fun App() {
    MaterialTheme {
        val inputState = remember { ChatInputState() }
        val chatState = remember { ChatState() }
        val history = remember { ChatHistoryState() }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var screen by remember { mutableStateOf(AppScreen.Chat) }

        // 系统返回键：子页面先回到聊天页，聊天页才交给系统退出应用
        AppBackHandler(enabled = screen != AppScreen.Chat) {
            screen = AppScreen.Chat
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
                    ChatContent(
                        inputState = inputState,
                        chatState = chatState,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSend = { text -> scope.launch { chatState.send(text) } },
                    )
                }

                AppScreen.Settings -> SettingsScreen(
                    onBack = { screen = AppScreen.Chat },
                )

                AppScreen.Search -> SearchScreen(
                    history = history,
                    onBack = {
                        history.clearSearch()
                        screen = AppScreen.Chat
                    },
                    onSelectItem = { item ->
                        history.select(item.id)
                        history.clearSearch()
                        screen = AppScreen.Chat
                    },
                )
            }
        }
    }
}

/** 聊天页：消息区铺满，顶栏半透明悬浮其上 */
@Composable
private fun ChatContent(
    inputState: ChatInputState,
    chatState: ChatState,
    onMenuClick: () -> Unit,
    onSend: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()   // 内容避开状态栏 / 导航栏，不再与其重叠
            .imePadding(),          // 键盘弹出时输入框上移
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // 聊天消息铺满整个区域（顶部留出浮层高度，由 ChatScreen 的内容内边距处理）
            ChatScreen(
                state = chatState,
                modifier = Modifier.fillMaxSize(),
            )

            // 顶部区域半透明，避免整条 fillMaxWidth 的实色条大面积遮挡聊天内容
            MainTopBar(
                onMenuClick = onMenuClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TopBarEdgeFadeBrush),
            )
        }

        // 输入框
        ChatInputBox(
            state = inputState,
            onSend = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** 主内容顶栏：左侧菜单按钮，用于打开抽屉 */
@Composable
private fun MainTopBar(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MenuButtonBackground)
                .clickable(onClick = onMenuClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.menu),
                contentDescription = "打开菜单",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
