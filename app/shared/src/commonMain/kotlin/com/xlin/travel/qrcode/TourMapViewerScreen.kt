package com.xlin.travel.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 线路地图全屏查看页（六条线路共用）：
 * 从线路页点击地图进入；
 * 双指捏合缩放（1~5 倍）、单指拖动平移、双击在放大与还原之间切换。
 * 固定暖色（[LifenColors]），不随黑白主题切换。
 */
@Composable
fun TourMapViewerScreen(
    mapImage: DrawableResource,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // 平移范围随缩放倍数变化：最多把图片边缘拖到容器中心，避免拖丢
    fun clampOffset(value: Offset, zoom: Float): Offset {
        val maxX = (containerSize.width * (zoom - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (containerSize.height * (zoom - 1f) / 2f).coerceAtLeast(0f)
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    val transformState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        val actualZoom = newScale / scale
        // 以双指中心为锚点缩放：把质心换算到相对容器中心的向量，保持指下的图不动
        val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
        val anchor = centroid - center
        val anchored = anchor + (offset - anchor) * actualZoom
        scale = newScale
        offset = if (scale > 1f) clampOffset(anchored + panChange, scale) else Offset.Zero
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LifenPageBrush)
            .safeContentPadding(),
    ) {
        LifenTopBar(title = "线路地图", onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    })
                }
                .transformable(state = transformState),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(mapImage),
                contentDescription = "线路地图",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }

        Text(
            text = "双指缩放 · 拖动查看 · 双击放大/还原",
            color = LifenInkSoft,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(12.dp),
        )
    }
}