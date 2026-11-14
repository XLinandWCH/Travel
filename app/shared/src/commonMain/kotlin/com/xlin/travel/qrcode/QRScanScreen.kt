package com.xlin.travel.qrcode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.videoexplain.camera.CameraPreview

/** 摄像头画面上的提示文字用暖白 */
private val OnCameraText = Color(0xFFF7F1E4)

/**
 * 二维码扫描页面（里份旅游线入口，固定暖色）：
 * 打开摄像头预览，显示扫描框和扫描线动画，双击屏幕触发扫描成功，跳转到里份路线图页面。
 */
@Composable
fun QRScanScreen(
    onBack: () -> Unit,
    onQRScanned: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cameraOn by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LifenPageBrush)
            .safeContentPadding(),
    ) {
        LifenTopBar(title = "扫码游里份", onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        // 双击屏幕触发扫描成功
                        onQRScanned()
                    })
                },
        ) {
            // 摄像头预览画面
            if (cameraOn) {
                CameraPreview(cameraOn = true, modifier = Modifier.fillMaxSize())
            }

            // 扫描框和扫描线叠加层
            QRScanOverlay(
                onStartClick = { cameraOn = true },
                cameraOn = cameraOn,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 扫码叠加层：未启动时显示暖色引导与开始按钮；启动后显示红砖色扫描框与扫描线动画。
 */
@Composable
private fun QRScanOverlay(
    onStartClick: () -> Unit,
    cameraOn: Boolean,
    modifier: Modifier = Modifier,
) {
    val scanLineY = remember { Animatable(0f) }

    LaunchedEffect(cameraOn) {
        if (cameraOn) {
            // 扫描线从上到下循环动画
            while (true) {
                scanLineY.animateTo(1f, tween(1500, easing = LinearEasing))
                scanLineY.snapTo(0f)
            }
        } else {
            scanLineY.snapTo(0f)
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        if (!cameraOn) {
            // 未启动摄像头时显示引导与开始按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 48.dp),
            ) {
                Text(
                    text = "扫描二维码",
                    fontSize = 20.sp,
                    color = LifenInk,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "扫描里份入口的二维码，打开里份路线图",
                    fontSize = 13.sp,
                    color = LifenInkSoft,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onStartClick,
                    colors = ButtonDefaults.buttonColors(containerColor = LifenBrick),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(
                        text = "开始扫码",
                        color = LifenCard,
                        fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "双击屏幕可完成扫描",
                    fontSize = 12.sp,
                    color = LifenInkSoft,
                )
            }
        } else {
            // 摄像头开启时显示扫描框和扫描线
            Canvas(modifier = Modifier.fillMaxSize()) {
                val size = this.size
                val scanBoxSize = size.minDimension * 0.6f
                val scanBoxTop = (size.height - scanBoxSize) / 2
                val scanBoxLeft = (size.width - scanBoxSize) / 2
                val scanBox = Rect(
                    scanBoxLeft, scanBoxTop,
                    scanBoxLeft + scanBoxSize, scanBoxTop + scanBoxSize
                )

                // 半透明深棕遮罩（带棕味，不用纯黑）
                drawRect(Color(0xFF2B1B10).copy(alpha = 0.35f))

                // 四个角的红砖色边框
                val cornerLength = 30f
                val strokeWidth = 4f

                drawLine(LifenBrick, scanBox.topLeft, scanBox.topLeft + Offset(cornerLength, 0f), strokeWidth)
                drawLine(LifenBrick, scanBox.topLeft, scanBox.topLeft + Offset(0f, cornerLength), strokeWidth)

                drawLine(LifenBrick, scanBox.topRight - Offset(cornerLength, 0f), scanBox.topRight, strokeWidth)
                drawLine(LifenBrick, scanBox.topRight, scanBox.topRight + Offset(0f, cornerLength), strokeWidth)

                drawLine(LifenBrick, scanBox.bottomLeft, scanBox.bottomLeft + Offset(cornerLength, 0f), strokeWidth)
                drawLine(LifenBrick, scanBox.bottomLeft, scanBox.bottomLeft + Offset(0f, -cornerLength), strokeWidth)

                drawLine(LifenBrick, scanBox.bottomRight - Offset(cornerLength, 0f), scanBox.bottomRight, strokeWidth)
                drawLine(LifenBrick, scanBox.bottomRight, scanBox.bottomRight + Offset(0f, -cornerLength), strokeWidth)

                // 扫描线
                val lineY = scanBox.top + scanLineY.value * scanBox.size.height
                drawLine(
                    LifenBrick,
                    Offset(scanBox.left, lineY),
                    Offset(scanBox.right, lineY),
                    strokeWidth = 3f
                )
            }

            // 提示文字
            Text(
                text = "双击屏幕完成扫描",
                color = OnCameraText,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            )
        }
    }
}
