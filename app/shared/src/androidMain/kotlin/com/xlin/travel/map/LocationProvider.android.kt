package com.xlin.travel.map

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** 服务端 map 表约定的平台标识 */
actual val mapPlatformKey: String = "ANDROID"

/** 单次定位的等待上限，超时仍无回调则返回缓存 / 失败 */
private const val SINGLE_FIX_TIMEOUT_MS = 10_000L

/**
 * Android 定位权限：已授权立即回调 true，否则用 RequestPermission 拉起系统授权框。
 * ACCESS_FINE_LOCATION 需在 AndroidManifest 声明（已加）；定位服务（GPS）开关不在此检查，
 * 由 [locationProvider] 在拿不到位置时提示。
 */
@Composable
actual fun rememberLocationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> onResult(granted) }
    return {
        val already = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (already) onResult(true) else launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

/**
 * Android 定位：系统 LocationManager（不引 Google Play 服务，国产 ROM 也能用）。
 *
 * 策略：先取 GPS / 网络 providers 的 lastKnownLocation（立即返回缓存点，演示够用）；
 * 全部为空时发起单次定位挂起等待，拿不到就报明确错误。
 * 后续接入高德 Android SDK 后，此 actual 换成 AMapLocationClient（可拿 GCJ-02 坐标与地址）。
 *
 * 权限：ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION 需运行时授权（AndroidManifest 已声明），
 * 未授权直接抛可读异常，由页面展示引导。
 */
@Suppress("MissingPermission")   // 已用 checkSelfPermission 先行检查
internal actual suspend fun locationProvider(context: Any?): LocationInfo {
    // 优先用显式传入的 Context，其次回退到 initMapContext 保存的实例
    val app = (context as? Context) ?: androidMapContext
        ?: error("定位失败：未获得平台上下文")
    val granted = app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED ||
        app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    if (!granted) error("没有定位权限，请在系统设置中允许本应用获取位置")

    val manager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    val last = providers
        .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    val location = last ?: manager.requestSingleFix(providers)
        ?: error("暂未获取到位置，请确认系统已开启定位服务后重试")

    return LocationInfo(
        point = GeoPoint(location.latitude, location.longitude),
        accuracyMeters = location.accuracy.toDouble(),
        timeMillis = location.time,
    )
}

/** 无缓存点时的单次定位：向首个可用 provider 注册更新，收到第一帧即返；全部关闭或超时返回 null */
private suspend fun LocationManager.requestSingleFix(providers: List<String>): Location? =
    suspendCancellableCoroutine { cont ->
        val callback = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (cont.isActive) {
                    removeUpdates(this)
                    cont.resume(location)
                }
            }

            override fun onProviderDisabled(provider: String) {
                // 只有当所有目标 provider 都关闭时才判失败，否则继续等其它回调
                val anyEnabled = providers.any {
                    runCatching { isProviderEnabled(it) }.getOrDefault(false)
                }
                if (!anyEnabled && cont.isActive) {
                    removeUpdates(this)
                    cont.resume(null)
                }
            }
        }
        val requested = providers.any { provider ->
            // 显式传主 Looper：挂起函数不一定在主线程恢复，无 Looper 线程直接注册会抛异常
            runCatching {
                requestLocationUpdates(
                    provider, 0L, 0f, callback,
                    android.os.Looper.getMainLooper(),
                )
            }.isSuccess
        }
        if (!requested) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        // 超时兜底：室内无信号等场景 10 秒后主动收场，避免永久挂起
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeout = Runnable {
            if (cont.isActive) {
                runCatching { removeUpdates(callback) }
                cont.resume(null)
            }
        }
        handler.postDelayed(timeout, SINGLE_FIX_TIMEOUT_MS)
        cont.invokeOnCancellation {
            handler.removeCallbacks(timeout)
            runCatching { removeUpdates(callback) }
        }
    }
