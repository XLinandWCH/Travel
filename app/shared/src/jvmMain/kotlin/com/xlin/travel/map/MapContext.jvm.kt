package com.xlin.travel.map

import androidx.compose.runtime.Composable

/** JVM（桌面）无定位能力，无需上下文 */
@Composable
actual fun mapPlatformContext(): Any? = null

actual fun initMapContext(context: Any?) {
    // 空操作
}
