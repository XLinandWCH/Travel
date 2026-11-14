package com.xlin.travel.routeplan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 路线规划地图的平台渲染入口。
 *
 * [platformHasNativeRouteMap] 为 true 的平台（Android）用原生高德 SDK 渲染可交互真地图；
 * 其余平台（iOS / JS / JVM）为 false，调用方走 [RouteMapSchematic] 示意图或静态地图降级。
 * 这样「路线规划」在各端都有的看，Android 端优先保障真地图。
 */
expect val platformHasNativeRouteMap: Boolean

/**
 * 平台原生路线地图：以 [spot] 的途经点序列画出折线与站点标记，[focusedStop] 非空时相机聚焦该站点。
 *
 * [apiKey] / [securityCode] 为服务端 map 表下发的「Android 平台」高德 SDK 密钥与安全码，
 * 仅原生实现需要（渲染前初始化 SDK）；非原生 actual 忽略这两个参数。
 * 数据坐标为 GCJ-02，与高德一致，直接绘制无需纠偏。
 */
@Composable
expect fun RoutePlanPlatformMap(
    spot: RouteSpot,
    focusedStop: Int?,
    apiKey: String,
    securityCode: String,
    modifier: Modifier = Modifier,
)
