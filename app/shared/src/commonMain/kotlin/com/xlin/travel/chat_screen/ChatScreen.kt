package com.xlin.travel.chat_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.InputTextColor

/**
 * 聊天界面占位，后续在此实现消息列表等功能
 */
@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "聊天界面开发中",
            color = InputTextColor,
            fontSize = 18.sp,
        )
    }
}
