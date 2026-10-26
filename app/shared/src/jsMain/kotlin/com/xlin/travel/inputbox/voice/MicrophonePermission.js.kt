// jsMain/kotlin/com/xlin/travel/inputbox/voice/MicrophonePermission.js.kt
package com.xlin.travel.inputbox.voice

import androidx.compose.runtime.Composable

/** Web 端不申请麦克风权限（非安全上下文不可用），直接回调 false */
@Composable
actual fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): () -> Unit = {
    onResult(false)
}
