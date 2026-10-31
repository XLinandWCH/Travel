package com.xlin.travel.videoexplain.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Web 版没有可用的相机桥接（需要 getUserMedia + <video> 元素），用取景占位 */
@Composable
actual fun CameraPreview(cameraOn: Boolean, modifier: Modifier) {
    CameraFallback(modifier = modifier)
}
