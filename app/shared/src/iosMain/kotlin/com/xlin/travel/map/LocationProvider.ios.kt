package com.xlin.travel.map

import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLLocationManagerAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.CLLocationManagerAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.CLLocationManagerAuthorizationStatusDenied
import platform.CoreLocation.CLLocationManagerAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

/** 服务端 map 表约定的平台标识 */
actual val mapPlatformKey: String = "IOS"

/**
 * iOS 定位权限：查询 CLLocationManager 授权状态。
 * 未决定（.notDetermined）时先发起 requestWhenInUseAuthorization 再回调 true——
 * 真正的系统弹框也可能延到首次 [currentLocation] 启动定位时出现（两种时序都属正常）。
 * 被拒绝 / 受限时回调 false，引导用户去系统设置开启。
 * 需在 iosApp/Info.plist 声明 NSLocationWhenInUseUsageDescription，否则申请会崩溃。
 */
@Composable
actual fun rememberLocationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit = {
    when (CLLocationManager.authorizationStatus()) {
        CLLocationManagerAuthorizationStatusDenied,
        CLLocationManagerAuthorizationStatusRestricted,
        -> onResult(false)
        CLLocationManagerAuthorizationStatusAuthorizedWhenInUse,
        CLLocationManagerAuthorizationStatusAuthorizedAlways,
        -> onResult(true)
        else -> {
            CLLocationManager().requestWhenInUseAuthorization()
            onResult(true)
        }
    }
}

/** requestLocation 单次结果的一次性回调代理（manager 持有到回调完成，闭包捕获保活） */
private class LocationDelegate(
    val manager: CLLocationManager,
    onFix: (latitude: Double, longitude: Double, accuracy: Double, epochMillis: Double) -> Unit,
    onError: (String) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        onFix(
            loc.coordinate.latitude,
            loc.coordinate.longitude,
            loc.horizontalAccuracy,
            // Apple 系统时间（2001 起点）秒 → epoch 毫秒
            (loc.timestamp.timeIntervalSince1970 + 973_097_400.0) * 1000.0,
        )
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: platform.Foundation.NSError?) {
        // kCLErrorDenied（用户当场点了不允许）单独文案，其余透出 localizedDescription
        val denied = didFailWithError?.domain == "kCLErrorDomain" && didFailWithError.code == 1L
        onError(
            if (denied) "用户拒绝了定位授权，请在 设置→隐私→定位服务 中允许本应用" else
                "定位失败：${didFailWithError?.localizedDescription ?: "未知错误"}",
        )
    }
}

/**
 * iOS 定位：CoreLocation 的 requestLocation 单次定位（iOS 11+）。
 * 后续接入高德 iOS SDK 后，此 actual 换成高德定位（可拿 GCJ-02 坐标与地址）。
 */
internal actual suspend fun locationProvider(context: Any?): LocationInfo = suspendCancellableCoroutine { cont ->
    // CLLocationManager 要求在主线程创建 / 启动，否则 delegate 回调不送达；
    // continuation 跨线程 resume 安全
    dispatch_async(dispatch_get_main_queue()) {
        val manager = CLLocationManager()
        val delegate = LocationDelegate(
            manager,
            onFix = { lat, lng, accuracy, epochMillis ->
                cont.resume(
                    LocationInfo(
                        point = GeoPoint(lat, lng),
                        accuracyMeters = accuracy,
                        timeMillis = epochMillis.toLong(),
                    ),
                )
            },
            onError = { message -> cont.resumeWithException(RuntimeException(message)) },
        )
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
        // requestLocation 只回调一次结果（成功 / 失败），适合单次定位演示
        manager.requestLocation()
        cont.invokeOnCancellation { manager.stopUpdatingLocation() }
    }
}
