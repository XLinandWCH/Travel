package com.xlin.travel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlin.travel.chat_screen.ChatScreen
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.inputbox.ChatInputBox
import com.xlin.travel.inputbox.ChatInputState

@Composable
fun App() {
    MaterialTheme {
        val inputState = remember { ChatInputState() }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatBackground)
                .safeContentPadding()
                .imePadding(),
        ) {
            // 上：聊天界面
            ChatScreen(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            // 下：输入框
            ChatInputBox(
                state = inputState,
                onSend = { /* TODO 接入发送逻辑 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
