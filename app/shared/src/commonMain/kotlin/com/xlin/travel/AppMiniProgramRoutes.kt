package com.xlin.travel

import androidx.compose.runtime.Composable
import com.xlin.travel.miniprogram.MiniProgramDetailScreen

/**
 * 小程序侧路由：聊天页的快捷条 / 面板点到某个小程序后，记住名字并进入它自己的「开发中」占位页，
 * 占位页的返回按钮回到聊天页。
 */
@Composable
internal fun AppHost.MiniProgramRoutes() {
    when (screen) {
        AppScreen.MiniProgramDetail -> MiniProgramDetailScreen(
            name = currentProgram,
            onBack = { screen = AppScreen.Chat },
        )

        else -> Unit
    }
}
