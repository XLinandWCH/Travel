package com.xlin.travel.miniprogram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.navigation.PageTopBar

/**
 * 小程序空壳页：只有顶栏（「< 小程序名」），内容区留空。
 *
 * 为什么留空：12 个小程序已改为「点一下 → 输入框插入 @名称 → AI 调用对应工具作答」，
 * 不再各自维护一套页面；这里保留入口壳，是为了以后需要独立页面时有落点，
 * 同时固定「顶部返回 + 标题」这一层导航观感。真正的展示交给 AI 回答与 A2UI 个性化界面。
 */
@Composable
fun MiniProgramDetailScreen(
    name: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = name, onBack = onBack)
    }
}
