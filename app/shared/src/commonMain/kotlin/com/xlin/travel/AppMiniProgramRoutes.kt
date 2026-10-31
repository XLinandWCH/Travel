package com.xlin.travel

import androidx.compose.runtime.Composable
import com.xlin.travel.a2ui.A2uiSurfaceScreen
import com.xlin.travel.chat_screen.SurfaceStage
import com.xlin.travel.miniprogram.MiniProgramDetailScreen
import kotlinx.coroutines.launch

/**
 * 小程序与 A2UI 侧路由：
 * - MiniProgramDetail：小程序空壳页（只保留顶栏），内容由聊天里的 AI 工具回答承担；
 * - A2ui：AI 编排的个性化界面，actionBar 的按钮不是跳页，而是把 prompt 发回聊天继续对话。
 */
@Composable
internal fun AppHost.MiniProgramRoutes() {
    when (screen) {
        AppScreen.MiniProgramDetail -> MiniProgramDetailScreen(
            name = currentProgram,
            onBack = { screen = AppScreen.Chat },
        )

        AppScreen.A2ui -> A2uiSurfaceScreen(
            // 读消息本体：生成中的新块到达时，页面自动重组，不用回到聊天再点一次
            surface = currentSurfaceMessage?.surface,
            building = currentSurfaceMessage?.surfaceStage == SurfaceStage.Building,
            onBack = { screen = AppScreen.Chat },
            onAction = { prompt ->
                // 回聊天并真实发问：界面里的「换个更慢的版本」这类按钮，最终都变成一句话
                screen = AppScreen.Chat
                scope.launch { chatState.send(prompt) }
            },
        )

        else -> Unit
    }
}
