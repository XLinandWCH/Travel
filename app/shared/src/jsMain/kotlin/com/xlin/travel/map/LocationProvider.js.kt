package com.xlin.travel.map

import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 服务端 map 表约定的平台标识 */
actual val mapPlatformKey: String = "WEB"

/**
 * Web 定位权限：浏览器 Geolocation 没有独立的可查询权限 API，授权框在首次定位时才弹，
 * 这里直接回调 true，真正的成功 / 拒绝由 [locationProvider] 的 Promise 结果决定。
 */
@Composable
actual fun rememberLocationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit = {
    onResult(true)
}

/** navigator.geolocation 单次定位返回的位置坐标结构 */
private external interface Coordinates {
    val latitude: Double
    val longitude: Double
    val accuracy: Double
}

/** navigator.geolocation 定位成功回调参数 */
private external interface GeolocationPosition {
    val coords: Coordinates
    val timestamp: Double
}

/** navigator.geolocation 定位失败回调参数 */
private external interface GeolocationError {
    val code: Int
    val message: String
}

/** getCurrentPosition 的 options 参数 */
private external interface GeolocationOptions {
    var enableHighAccuracy: Boolean
    var timeout: Int
    var maximumAge: Int
}

/**
 * Web 定位：浏览器 Geolocation API（getCurrentPosition）。
 * 高德 JS API 本身不含无 UI 的纯定位能力，定位统一交给浏览器；
 * 后续接入高德 JS SDK 只替换 MapView 渲染部分，定位结果结构不变。
 */
internal actual suspend fun locationProvider(context: Any?): LocationInfo =
    suspendCancellableCoroutine { cont ->
        val geolocation = js("navigator.geolocation").unsafeCast<WebGeolocation>()
        if (!geolocation.isAvailable()) {
            cont.resumeWithException(RuntimeException("当前浏览器不支持地理定位"))
            return@suspendCancellableCoroutine
        }
        val options = js("({ enableHighAccuracy: true, timeout: 15000, maximumAge: 0 })")
            .unsafeCast<GeolocationOptions>()
        geolocation.getCurrentPosition(
            onSuccess = { position ->
                cont.resume(
                    LocationInfo(
                        point = GeoPoint(position.coords.latitude, position.coords.longitude),
                        accuracyMeters = position.coords.accuracy,
                        timeMillis = position.timestamp.toLong(),
                    ),
                )
            },
            onError = { error ->
                val reason = when (error.code) {
                    1 -> "用户拒绝了定位请求"
                    2 -> "当前位置不可用，请检查设备定位服务"
                    3 -> "定位超时，请重试"
                    else -> error.message.ifBlank { "定位失败" }
                }
                cont.resumeWithException(RuntimeException(reason))
            },
            options = options,
        )
    }

/** 只声明用到的 navigator.geolocation 成员，避免依赖 Kotlin/JS 的完整 DOM 绑定 */
private external interface WebGeolocation {
    fun isAvailable(): Boolean
    fun getCurrentPosition(
        onSuccess: (GeolocationPosition) -> Unit,
        onError: (GeolocationError) -> Unit,
        options: GeolocationOptions,
    )
}
