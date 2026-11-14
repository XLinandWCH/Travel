package com.xlin.travel.map

/**
 * 一次定位的结果（MapModels.kt 里另有路线 / 站点等业务模型）。
 *
 * 定位优先走各端系统能力（Android LocationManager / iOS CoreLocation / 浏览器 Geolocation），
 * 后续接入高德 SDK 后由 SDK 定位（可拿到 GCJ-02 坐标与逆地名字段）替换 actual，
 * commonMain 的业务代码与 UI 不需要改动——这正是 MAP 分层的目的。
 *
 * [context]：仅 Android 需要（包内持有的平台上下文，挂起函数里拿不到 CompositionLocal），
 * 在 @Composable 侧把 LocalContext.current 传进来；其余端传 null 即可。
 *
 * 失败约定：直接抛异常，message 为可读中文（无权限 / 定位服务关闭 / 平台不支持 / 超时等），
 * 由调用方捕获展示；返回非空结果或抛出，不存在“返回 null”的中间态。
 */
suspend fun currentLocation(context: Any? = null): LocationInfo = locationProvider(context)

/** 平台定位实现（expect / actual），语义见 [currentLocation] */
internal expect suspend fun locationProvider(context: Any?): LocationInfo
