package com.xlin.travel

import androidx.compose.runtime.Composable
import com.xlin.travel.a2ui.A2uiSurfaceScreen
import com.xlin.travel.a2ui.SurfaceGalleryScreen
import com.xlin.travel.chat_screen.ChatMessage
import com.xlin.travel.chat_screen.MessageRole
import com.xlin.travel.chat_screen.SurfaceStage
import com.xlin.travel.miniprogram.MiniProgramDetailScreen
import kotlinx.coroutines.launch

/**
 * 小程序与 A2UI 侧路由：
 * - MiniProgramDetail：小程序空壳页（只保留顶栏），内容由聊天里的 AI 工具回答承担；
 * - A2ui：AI 编排的个性化界面，actionBar 的按钮不是跳页，而是把 prompt 发回聊天继续对话；
 * - Surfaces：个性化窗口展示，近 1~7 天生成过的界面按时间汇总，点开回到只读的同一屏。
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
            // 返回目标随入口变化：聊天页点进来回聊天，历史页点进来回历史
            onBack = { screen = surfaceBackTarget },
            onAction = { prompt ->
                // 回聊天并真实发问：界面里的「换个更慢的版本」这类按钮，最终都变成一句话
                screen = AppScreen.Chat
                scope.launch { chatState.send(prompt) }
            },
        )

        AppScreen.Surfaces -> SurfaceGalleryScreen(
            repository = repository,
            onBack = { screen = AppScreen.Chat },
            // 当前对话里已有界面的条数：变化时历史页自动重查一次（界面刚写库就应该能看见）
            liveCount = chatState.messages.count { it.surface != null },
            onOpenSurface = { surface ->
                // 历史里的界面已经定稿：包成一条只读消息交给同一个渲染页，不再走生成态
                currentSurfaceMessage = ChatMessage(id = -1L, role = MessageRole.Assistant).apply {
                    this.surface = surface
                    this.surfaceStage = SurfaceStage.Ready
                }
                surfaceBackTarget = AppScreen.Surfaces
                screen = AppScreen.A2ui
            },
        )

        else -> Unit
    }
}
