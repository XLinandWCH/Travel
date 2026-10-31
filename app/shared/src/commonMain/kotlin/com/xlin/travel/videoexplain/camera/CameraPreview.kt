package com.xlin.travel.videoexplain.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 摄像头画面（expect / actual）。
 *
 * - Android：CameraX 真机预览（PreviewView + 生命周期绑定 + 运行时权限申请），
 *   画面按中心裁剪铺满；没有权限 / 没有相机 / 绑定失败时回落到 [CameraFallback]
 *   并在画面上给一句原因；
 * - iOS / JVM / Web：暂用 [CameraFallback]（Canvas 拼的取景画面）。
 *
 * @param cameraOn 是否显示画面；false 时 Android 会解绑相机，避免摄像头一直亮着
 */
@Composable
expect fun CameraPreview(cameraOn: Boolean, modifier: Modifier = Modifier)
