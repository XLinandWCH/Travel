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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xlin.travel.chat_screen.ChatScreen
import com.xlin.travel.chat_screen.ChatState
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.MenuButtonBackground
import com.xlin.travel.drawer.ChatDrawer
import com.xlin.travel.drawer.ChatHistoryState
import com.xlin.travel.drawer.SearchDialog
import com.xlin.travel.inputbox.ChatInputBox
import com.xlin.travel.inputbox.ChatInputState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.menu

@Composable
fun App() {
    MaterialTheme {
        val inputState = remember { ChatInputState() }
        val chatState = remember { ChatState() }
        val history = remember { ChatHistoryState() }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // 背景铺满整屏（绘制到系统栏之下），内容再各自避开系统栏，符合原生 edge-to-edge 观感
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatBackground),
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ChatDrawer(
                        history = history,
                        onCloseDrawer = { scope.launch { drawerState.close() } },
                    )
                },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding()   // 内容避开状态栏 / 导航栏，不再与其重叠
                        .imePadding(),          // 键盘弹出时输入框上移
                ) {
                    // 顶栏：菜单按钮
                    MainTopBar(onMenuClick = { scope.launch { drawerState.open() } })

                    // 聊天界面：用户消息与 AI 流式回复
                    ChatScreen(
                        state = chatState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )

                    // 输入框
                    ChatInputBox(
                        state = inputState,
                        onSend = { text -> scope.launch { chatState.send(text) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            // 搜索弹窗：覆盖在最上层
            SearchDialog(history)
        }
    }
}

/** 主内容顶栏：左侧菜单按钮，用于打开抽屉 */
@Composable
private fun MainTopBar(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
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
