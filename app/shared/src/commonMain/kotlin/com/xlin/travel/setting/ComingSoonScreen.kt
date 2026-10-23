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
 * 通用“XXX即将上线”占位子界面：
 * 带 Chevron Right 但尚未开发的条目（账号控制 / 帮助中心 / 服务条款 / 隐私政策）统一进入此页。
 */
@Composable
fun ComingSoonScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = title, onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$title 即将上线",
                color = TextGray500,
                fontSize = 14.sp,
            )
        }
    }
}
