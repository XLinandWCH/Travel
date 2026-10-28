package com.xlin.travel.map

import androidx.compose.runtime.Composable

/** 服务端 map 表约定的平台标识（桌面不向服务端拉密钥，保留常量便于多端调试） */
actual val mapPlatformKey: String = "JVM"

/** JVM（桌面）无定位硬件接入，直接抛可读异常，地图演示页据此提示不可用 */
internal actual suspend fun locationProvider(context: Any?): LocationInfo {
    error("桌面端暂不支持定位功能，请在 Android / iOS / Web 端使用")
}

/** JVM（桌面）无定位能力，直接回调 false */
@Composable
actual fun rememberLocationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit = {
    onResult(false)
}
