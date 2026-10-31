package com.xlin.travel.videoexplain.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 桌面端没有摄像头预览能力，用取景占位（JVM 只是跑 Web 版的前置环境） */
@Composable
actual fun CameraPreview(cameraOn: Boolean, modifier: Modifier) {
    CameraFallback(modifier = modifier)
}
