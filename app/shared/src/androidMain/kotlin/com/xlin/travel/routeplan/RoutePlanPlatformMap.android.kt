package com.xlin.travel.routeplan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Android 端路线地图实现。
 *
 * 原计划用高德 SDK 渲染可交互真地图（密钥与安全码已由服务端 map 表下发），
 * 原生视图尚未接入，先统一回落到 [RouteMapSchematic]：不依赖密钥，
 * 未登录 / 断网 / 未配置 Key 时也能看到橙色折线串起的途经点顺序。
 * 接入真地图后把 [platformHasNativeRouteMap] 改成 true，调用方会自动切换。
 */
actual val platformHasNativeRouteMap: Boolean = false

@Composable
actual fun RoutePlanPlatformMap(
    spot: RouteSpot,
    focusedStop: Int?,
    apiKey: String,
    securityCode: String,
    modifier: Modifier,
) {
    RouteMapSchematic(spot = spot, focusedStop = focusedStop, modifier = modifier)
}
