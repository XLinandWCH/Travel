// jsMain/kotlin/com/xlin/travel/navigation/AppBackHandler.js.kt
package com.xlin.travel.navigation

import androidx.compose.runtime.Composable

/** 浏览器无系统返回键语义（页面内自带返回按钮），与 iOS / JVM 相同处理 */
@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op
}
