package com.xlin.travel.chat_screen

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xlin.travel.color.MenuButtonBackground
import com.xlin.travel.color.TopBarEdgeFadeBrush
import com.xlin.travel.inputbox.ChatInputBox
import com.xlin.travel.inputbox.ChatInputState
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.menu

/** 聊天页：消息区铺满，顶栏半透明悬浮其上，底部固定输入框 */
@Composable
internal fun MainChatContent(
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
