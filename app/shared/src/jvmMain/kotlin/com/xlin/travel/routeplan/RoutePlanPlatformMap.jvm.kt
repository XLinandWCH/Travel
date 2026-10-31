package com.xlin.travel.routeplan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 桌面（JVM）端路线地图实现：无原生高德地图，直接走 [RouteMapSchematic] 示意图。
 * [apiKey] / [securityCode] 为 Android 平台密钥，此端忽略。
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
