package com.xlin.travel.map

import androidx.compose.runtime.Composable

/** Web 定位（navigator.geolocation）无需预置上下文 */
@Composable
actual fun mapPlatformContext(): Any? = null

actual fun initMapContext(context: Any?) {
    // 空操作：浏览器 API 不依赖平台上下文
}
