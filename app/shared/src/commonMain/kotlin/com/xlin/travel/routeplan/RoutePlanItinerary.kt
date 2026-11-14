package com.xlin.travel.routeplan

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.MiniPanelDivider
import com.xlin.travel.color.ReasoningTextColor
import com.xlin.travel.color.RouteChipSelectedBackground
import com.xlin.travel.color.RouteStopSelectedBackground
import com.xlin.travel.color.RouteTimelineConnector
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.SettingChevron
import com.xlin.travel.color.TextGray400
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.color.WhiteBtnIcon
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.chevron_right

/**
 * 行程时间线卡片：标题行本身是开关（点击展开/收起全部站点，展开箭头旋 90°），
 * 收起时只留一行总览——功能可以藏起来，不把 5 个点一次性铺满屏幕。
 *
 * 每个站点行左侧是序号圆点与竖连线（与地图字母标注同一套编号），点击站点 = 选中并展开详情，
 * 同时通过 [onFocusStop] 让上方地图聚焦到该点；站点之间的「步行 1.0公里 · 约13分钟」小字
 * 随出行方式 [mode] 即时重算，是路线规划区别于纯景点介绍的核心信息。
 */
@Composable
internal fun RoutePlanItinerary(
    spot: RouteSpot,
    mode: TravelMode,
    focusedStop: Int?,
    detailOpen: Int?,
    onFocusStop: (Int?) -> Unit,
    onToggleDetail: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(if (expanded) 90f else 0f, label = "itineraryArrow")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SettingCardBackground)
            .animateContentSize(),
    ) {
        // 标题行：展开状态常驻，内容折叠
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "行程时间线",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${spot.stops.size} 站 · ${formatDistance(spot.totalDistanceMeters)}",
                color = TextGray500,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = painterResource(Res.drawable.chevron_right),
                contentDescription = if (expanded) "收起行程" else "展开行程",
                colorFilter = ColorFilter.tint(SettingChevron),
                modifier = Modifier
                    .size(14.dp)
                    .rotate(arrowRotation),
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                spot.stops.forEachIndexed { i, stop ->
                    ItineraryStopRow(
                        index = i,
                        stop = stop,
                        selected = focusedStop == i,
                        detailVisible = detailOpen == i,
                        trailingLine = i < spot.stops.lastIndex,
                        onClick = {
                            onFocusStop(if (focusedStop == i) null else i)
                            onToggleDetail(if (detailOpen == i) null else i)
                        },
                    )
                    // 站点间的路：小字居中，用时随方式变化
                    spot.legs.getOrNull(i)?.let { leg ->
                        LegRow(mode = mode, leg = leg)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 单个站点行：左列圆点+竖线，右列 时间/名称/时长；选中时名称染强调橙，详情即时展开 */
@Composable
private fun ItineraryStopRow(
    index: Int,
    stop: RouteStop,
    selected: Boolean,
    detailVisible: Boolean,
    trailingLine: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) RouteStopSelectedBackground else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
    ) {
        // 左列：上连线 / 圆点 / 下连线，圆点内序号与地图字母标注一致
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(22.dp)) {
            Box(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) AccentOrange else RouteChipSelectedBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    color = if (selected) WhiteBtnIcon else AccentOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            // 最后一站后不再画连线
            if (trailingLine) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(10.dp)
                        .background(RouteTimelineConnector),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.arrival,
                color = TextGray500,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stop.name,
                color = if (selected) AccentOrange else TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "游玩 ${stop.stayMinutes} 分钟",
                color = TextGray400,
                fontSize = 12.sp,
            )
            if (detailVisible) {
                Spacer(Modifier.height(8.dp))
                StopDetails(stop)
            }
        }
    }
}

/** 站点详情：亮点 / 门票 / 贴士 / 周边，四行小字，只在点开该站时出现 */
@Composable
private fun StopDetails(stop: RouteStop) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MiniPanelDivider.copy(alpha = 0.35f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DetailLine("亮点", stop.highlight)
        DetailLine("门票", stop.ticket)
        DetailLine("贴士", stop.tip)
        DetailLine("周边", stop.nearby)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row {
        Text(
            text = label,
            color = AccentOrange,
            fontSize = 12.sp,
            modifier = Modifier.width(34.dp),
        )
        Text(
            text = value,
            color = ReasoningTextColor,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 站间路的小字行：与圆点列同宽对齐，格式「步行 · 1.0公里 · 约13分钟」 */
@Composable
private fun LegRow(mode: TravelMode, leg: RouteLeg) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(22.dp))
        Text(
            text = "${mode.label} · ${formatDistance(leg.distanceMeters)} · ${leg.durationFor(mode)}",
            color = TextGray500,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}
