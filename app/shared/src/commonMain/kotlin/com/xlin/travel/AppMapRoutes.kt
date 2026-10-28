package com.xlin.travel

import androidx.compose.runtime.Composable
import com.xlin.travel.map.MapScreen

/**
 * 地图侧路由：聊天页顶栏「地图」按钮进入定位演示页，返回按钮回到聊天页。
 * 后续地图功能（路径规划 / 站点查询）展开时，页面间跳转也收在这个分支里。
 */
@Composable
internal fun AppHost.MapRoutes() {
    when (screen) {
        AppScreen.Map -> MapScreen(
            repository = repository,
            onBack = { screen = AppScreen.Chat },
        )

        else -> Unit
    }
}
