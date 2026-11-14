package com.xlin.travel.map

import androidx.compose.runtime.Composable

/**
 * 定位权限（expect / actual），与 [com.xlin.travel.inputbox.voice.rememberMicPermission] 同一套约定：
 * 返回一个 lambda，调用它即检查 / 申请权限；已授权立即回调 true，否则拉起系统授权框并回调用户选择。
 *
 * - Android：ActivityResultContracts.RequestPermission（ACCESS_FINE_LOCATION）
 * - iOS：CLLocationManager 授权状态（首次定位时系统自动弹框，这里查询当前态）
 * - Web：浏览器 Geolocation 无独立权限查询，直接回调 true（真正授权在定位时由浏览器弹出）
 * - JVM（桌面）：无定位能力，回调 false
 */
@Composable
expect fun rememberLocationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit
