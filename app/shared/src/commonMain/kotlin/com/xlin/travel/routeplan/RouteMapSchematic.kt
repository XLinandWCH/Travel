package com.xlin.travel.routeplan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.MapPlaceholderBackground
import com.xlin.travel.color.RouteTimelineConnector

/**
 * 路线示意图：不依赖任何地图服务 / 密钥，用 Canvas 按各途经点的经纬度相对位置
 * 把「一条线」画出来——橙色折线串联全部站点，站点画圆点，选中站点加白色高亮环。
 *
 * 它是真实高德地图不可用时（未登录 / 未配置 Web服务 Key / 断网 / 超额）的降级视图，
 * 保证「路线规划」永远有路线可看、点击站点也有视觉反馈；坐标在城市尺度下做线性映射即可，
 * 不做墨卡托投影，只求相对方位与顺序正确，够表达「先去哪、再去哪」。
 */
@Composable
internal fun RouteMapSchematic(
    spot: RouteSpot,
    focusedStop: Int?,
    modifier: Modifier = Modifier,
) {
    val stops = spot.stops
    val lons = stops.map { it.lng }
    val lats = stops.map { it.lat }
    val minLon = lons.min(); val maxLon = lons.max()
    val minLat = lats.min(); val maxLat = lats.max()

    Box(modifier = modifier.fillMaxSize().background(MapPlaceholderBackground)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = 30.dp.toPx()
            val usableW = (size.width - pad * 2).coerceAtLeast(1f)
            val usableH = (size.height - pad * 2).coerceAtLeast(1f)
            // 经纬度跨度为 0（单点/全部同点）时避免除零，落到中心
            val spanLon = (maxLon - minLon).takeIf { it > 1e-9 } ?: 1.0
            val spanLat = (maxLat - minLat).takeIf { it > 1e-9 } ?: 1.0

            fun project(i: Int): Offset {
                val fx = ((stops[i].lng - minLon) / spanLon).toFloat()
                val fy = ((stops[i].lat - minLat) / spanLat).toFloat()
                return Offset(pad + fx * usableW, size.height - pad - fy * usableH)
            }

            val points = stops.indices.map { project(it) }

            // 折线：强调橙串联相邻站点（粗线，压在圆点之下）
            for (i in 0 until points.lastIndex) {
                drawLine(
                    color = AccentOrange.copy(alpha = 0.85f),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // 站点圆点：外圈底 + 实心橙；选中站再套一圈白色高亮环
            points.forEachIndexed { i, p ->
                val selected = focusedStop == i
                val r = (if (selected) 11.dp else 9.dp).toPx()
                drawCircle(color = RouteTimelineConnector, radius = r + 3.dp.toPx(), center = p)
                drawCircle(color = if (selected) AccentOrange else Color(0xFF8A8F98), radius = r, center = p)
                if (selected) {
                    drawCircle(color = Color.White, radius = r + 4.dp.toPx(), center = p, style = Stroke(width = 2.5.dp.toPx()))
                }
            }
        }
    }
}
