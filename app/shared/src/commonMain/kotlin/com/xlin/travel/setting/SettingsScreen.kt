package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.TextGray500
import com.xlin.travel.navigation.PageTopBar

/**
 * 设置页面：左上角返回按钮，具体设置项后续在此基础上扩展。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = "设置", onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "设置项即将上线",
                color = TextGray500,
                fontSize = 14.sp,
            )
        }
    }
}
