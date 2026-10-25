package com.xlin.travel.inputbox.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android 麦克风权限：已授权立即回调 true，否则用 RequestPermission 拉起系统授权框。
 * RECORD_AUDIO 需在 AndroidManifest 声明；launcher 结果在主线程回调，可安全更新 Compose 状态。
 */
@Composable
actual fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> onResult(granted) }
    return {
        val already = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (already) onResult(true) else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
