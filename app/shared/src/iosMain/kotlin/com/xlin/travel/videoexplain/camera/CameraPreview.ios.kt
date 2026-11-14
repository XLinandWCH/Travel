package com.xlin.travel.videoexplain.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS 暂不接真机相机：AVCaptureSession + AVCaptureVideoPreviewLayer 需要
 * UIKitView 桥接与摄像头权限描述（Info.plist NSCameraUsageDescription），
 * 本机（Windows）无法编译验证，先用取景占位。
 * 需要时这里换成 UIKitView 包一层 AVCaptureVideoPreviewLayer 即可。
 */
@Composable
actual fun CameraPreview(cameraOn: Boolean, modifier: Modifier) {
    CameraFallback(modifier = modifier)
}
