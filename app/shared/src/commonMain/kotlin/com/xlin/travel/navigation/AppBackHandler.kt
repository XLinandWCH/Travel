package com.xlin.travel.navigation

import androidx.compose.runtime.Composable

/**
 * 跨平台系统返回键处理。
 * Android 使用官方推荐的 BackHandler；iOS / JVM 无系统返回键，页面内自带返回按钮。
 *
 * @param enabled 是否拦截返回事件
 * @param onBack 拦截后的处理逻辑
 */
@Composable
expect fun AppBackHandler(enabled: Boolean, onBack: () -> Unit)
