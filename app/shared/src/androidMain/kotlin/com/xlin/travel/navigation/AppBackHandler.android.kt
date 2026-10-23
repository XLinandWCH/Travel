package com.xlin.travel.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** Android：交给官方 BackHandler 处理，支持预测性返回 */
@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
