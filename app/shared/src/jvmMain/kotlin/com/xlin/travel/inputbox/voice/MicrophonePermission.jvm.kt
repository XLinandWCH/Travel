package com.xlin.travel.inputbox.voice

import androidx.compose.runtime.Composable

/** JVM（桌面）无麦克风接入，直接回调 false，语音输入面板据此提示不可用。 */
@Composable
actual fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): () -> Unit = {
    onResult(false)
}
