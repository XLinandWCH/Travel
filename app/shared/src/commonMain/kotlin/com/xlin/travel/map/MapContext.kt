package com.xlin.travel.map

import androidx.compose.runtime.Composable

/**
 * 地图模块的平台上下文注入（expect / actual）。
 *
 * 挂起函数里取不到 CompositionLocal（commonMain 也没有 LocalContext），
 * 因此在 @Composable 侧通过 [mapPlatformContext] 捕获平台上下文，再用 [initMapContext] 注入：
 * - Android：保存 Context 供 [currentLocation] 使用
 * - iOS / Web / JVM：定位不需要预置上下文，取 null / 注入为空操作
 */
expect fun initMapContext(context: Any?)

/** 当前平台的定位上下文（Android 为 Context，其余端 null）；只能在 @Composable 中调用 */
@Composable
expect fun mapPlatformContext(): Any?
