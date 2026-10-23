package com.xlin.travel.inputbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

/**
 * 输入框内用到的矢量图标（自绘部分）
 * 说明：添加 / 麦克风 / 发送 已改用 composeResources 里的 drawable（见 ChatInputBox），
 * 这里仅保留未提供 drawable 的少量图标（关闭✕、图片占位、下拉箭头）
 * 使用 Canvas 直接绘制，避免引入额外图片资源
 */

/** 关闭（✕）图标 */
fun closePath(stroke: Float = 2f): Path = Path().apply {
    val s = stroke / 2
    moveTo(6f, 6f - s); lineTo(12f, 12f - s); lineTo(18f, 6f - s)
    lineTo(18f + s, 6f); lineTo(12f + s, 12f); lineTo(18f + s, 18f)
    lineTo(18f, 18f + s); lineTo(12f, 12f + s); lineTo(6f, 18f + s)
    lineTo(6f - s, 18f); lineTo(12f - s, 12f); lineTo(6f - s, 6f); close()
}

/** 下拉三角箭头 */
fun arrowDownPath(): Path = Path().apply {
    moveTo(5f, 9f); lineTo(12f, 16f); lineTo(19f, 9f); close()
}

/** 图片占位图标（山形 + 日，容器自身充当外框） */
fun imagePlaceholderPath(): Path = Path().apply {
    // 山形
    moveTo(4f, 18f); lineTo(10f, 11f); lineTo(14f, 15f); lineTo(17f, 12f); lineTo(20f, 18f); close()
    // 太阳
    addOval(androidx.compose.ui.geometry.Rect(15.5f, 6f, 18.5f, 9f))
}

/**
 * 通用矢量图标渲染：将 24x24 viewBox 等比缩放到 [size]
 */
@androidx.compose.runtime.Composable
fun VectorIcon(
    path: Path,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        translate(this.size.width / 2f, this.size.height / 2f) {
            scale(s, s) {
                translate(-12f, -12f) {
                    drawPath(path, color = tint, style = Fill)
                }
            }
        }
    }
}
