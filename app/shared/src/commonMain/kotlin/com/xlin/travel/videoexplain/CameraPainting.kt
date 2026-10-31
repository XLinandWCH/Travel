package com.xlin.travel.videoexplain

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * 摄像头画面的 Canvas 绘制：城市剪影与取景网格。
 * 从 CameraStage 拆出，保证两个文件都在项目约定的 250 行以内。
 */

/** 三层建筑剪影 + 窗户灯点：近景更暗、远景更淡，形成景深 */
internal fun DrawScope.drawCityscape() {
    val w = size.width
    val h = size.height

    // 暮色光斑：画面右上角一团暖光
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x66FF8A50), Color.Transparent),
            center = Offset(w * 0.74f, h * 0.2f),
            radius = w * 0.42f,
        ),
        radius = w * 0.42f,
        center = Offset(w * 0.74f, h * 0.2f),
    )

    // 远景楼群（最淡）
    val far = listOf(0.02f to 0.50f, 0.18f to 0.44f, 0.34f to 0.52f, 0.52f to 0.46f, 0.70f to 0.50f, 0.86f to 0.43f)
    far.forEach { (left, top) ->
        drawRect(
            color = Color(0xFF4A3627).copy(alpha = 0.75f),
            topLeft = Offset(w * left, h * top),
            size = Size(w * 0.15f, h * (1f - top)),
        )
    }

    // 中景里份（红砖色，带过街楼缺口）
    drawRect(
        color = Color(0xFF6B3F2C),
        topLeft = Offset(0f, h * 0.60f),
        size = Size(w, h * 0.40f),
    )
    listOf(0.04f to 0.12f, 0.20f to 0.18f, 0.42f to 0.10f, 0.62f to 0.22f, 0.80f to 0.14f).forEach { (left, top) ->
        drawRect(
            color = Color(0xFF7C4A33),
            topLeft = Offset(w * left, h * top),
            size = Size(w * 0.17f, h * (0.60f - top)),
        )
    }

    // 窗户灯点：只在中景墙上点几盏，密度低才像夜景
    val windows = listOf(
        0.08f to 0.68f, 0.16f to 0.74f, 0.28f to 0.66f, 0.36f to 0.78f,
        0.52f to 0.70f, 0.60f to 0.64f, 0.72f to 0.76f, 0.84f to 0.68f, 0.92f to 0.80f,
    )
    windows.forEachIndexed { index, (left, top) ->
        drawRect(
            color = Color(0xFFFFC08A).copy(alpha = if (index % 3 == 0) 0.85f else 0.45f),
            topLeft = Offset(w * left, h * top),
            size = Size(8.dp.toPx(), 10.dp.toPx()),
        )
    }

    // 近景压暗，让字幕与按钮浮起来
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)),
        ),
        topLeft = Offset.Zero,
        size = Size(w, h),
    )
}

/** 取景网格与中央对焦框：细白线，压得很淡避免抢画面 */
internal fun DrawScope.drawViewfinder() {
    val w = size.width
    val h = size.height
    val line = Color.White.copy(alpha = 0.10f)
    val thin = 1.dp.toPx()

    // 九宫格：两条竖线 + 两条横线
    listOf(1f / 3f, 2f / 3f).forEach { ratio ->
        drawRect(line, Offset(w * ratio, 0f), Size(thin, h))
        drawRect(line, Offset(0f, h * ratio), Size(w, thin))
    }

    // 中央对焦框：四条边各留两段短角线，中间断开
    val arm = 22.dp.toPx()
    val stroke = 2.dp.toPx()
    val focus = Color.White.copy(alpha = 0.55f)
    val half = 26.dp.toPx()
    val left = w / 2f - half
    val top = h / 2f - half
    val right = w / 2f + half
    val bottom = h / 2f + half
    drawRect(focus, Offset(left, top), Size(arm, stroke))
    drawRect(focus, Offset(right - arm, top), Size(arm, stroke))
    drawRect(focus, Offset(left, bottom - stroke), Size(arm, stroke))
    drawRect(focus, Offset(right - arm, bottom - stroke), Size(arm, stroke))
    drawRect(focus, Offset(left, top), Size(stroke, arm))
    drawRect(focus, Offset(left, bottom - arm), Size(stroke, arm))
    drawRect(focus, Offset(right - stroke, top), Size(stroke, arm))
    drawRect(focus, Offset(right - stroke, bottom - arm), Size(stroke, arm))
}
