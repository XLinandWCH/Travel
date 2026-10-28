package com.xlin.travel.map

import androidx.compose.runtime.Composable

/** iOS 定位（CoreLocation）无需预置上下文 */
@Composable
actual fun mapPlatformContext(): Any? = null

actual fun initMapContext(context: Any?) {
    // 空操作：CoreLocation 不依赖平台上下文
}
