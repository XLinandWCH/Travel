package com.xlin.travel.navigation

import androidx.compose.runtime.Composable

/** JVM 桌面端没有系统返回键，页面内使用返回按钮 */
@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op
}
