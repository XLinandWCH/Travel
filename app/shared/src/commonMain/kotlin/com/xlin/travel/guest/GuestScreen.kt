package com.xlin.travel.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.TextWhite
import com.xlin.travel.inputbox.ChatInputBox
import com.xlin.travel.inputbox.ChatInputState

/**
 * 未登入界面，1:1 对应 UI设计/未登入界面UI.html：
 * - 右上角「登入」按钮（强调橙圆角），点击进入登录页；
 * - 居中仅保留 Welcome 大字；
 * - 底部使用项目自己的输入框 [ChatInputBox]（参考稿的输入框不采用）。
 *
 * 游客发送消息时 [onSend] 会引导去登录，与点击「登入」一致。
 */
@Composable
fun GuestScreen(
    inputState: ChatInputState,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding()   // 内容避开状态栏 / 导航栏
            .imePadding(),          // 键盘弹出时输入框上移
    ) {
        // 顶部右上角：登入
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            SignInButton(onLogin)
        }

        // 中间：仅保留 Welcome
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Welcome",
                color = TextWhite,
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 60.dp), // 视觉略微偏上
            )
        }

        // 底部：项目自有输入框；游客发送 → 引导登录
        ChatInputBox(
            state = inputState,
            onSend = { onLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }
}

/** 「登入」按钮：强调橙底白字，圆角 12，带轻微投影 */
@Composable
private fun SignInButton(onClick: () -> Unit) {
    Text(
        text = "登入",
        color = TextWhite,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(AccentOrange)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}
