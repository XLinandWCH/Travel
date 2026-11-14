package com.xlin.travel.routeplan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.MapPlaceholderBackground
import com.xlin.travel.color.RouteChipSelectedBackground
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.TextGray400
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.color.WhiteBtnBackground
import com.xlin.travel.color.WhiteBtnIcon
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.navigation.PageTopBar
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.mini_route

/**
 * 「路线规划」小程序主屏：地图在上、路线信息在下，重点突出「一条线」而不是功能陈列。
 *
 * 布局自上而下：顶栏 → 地图卡片（真实高德静态地图，不可用时降级路线示意图）→ 景点简介
 * → 出行方式胶囊 → 行程时间线 → AI 定制入口。整屏内容纵向可滚动；
 * 顶栏经 safeContentPadding 避开系统状态栏（与其余子页面同一套做法）。
 * demo 数据写死在 [RouteSpot.WuhanLifen]。
 */
@Composable
fun RoutePlanScreen(
    repository: ChatRepository?,
    onBack: () -> Unit,
    onAiCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spot = RouteSpot.WuhanLifen
    var mode by remember { mutableStateOf(TravelMode.Walk) }
    var focusedStop by remember { mutableStateOf<Int?>(null) }
    var detailOpen by remember { mutableStateOf<Int?>(null) }
    var webKey by remember { mutableStateOf<String?>(null) }

    // 静态地图属高德 Web服务，固定取 map 表 WEB 行密钥（设备平台的 SDK Key 调不了 restapi）
    LaunchedEffect(repository) {
        webKey = ensureWebMapKey(repository)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),   // 内容避开状态栏 / 导航栏，顶栏不再压到系统时间上
    ) {
        PageTopBar(title = "路线规划", onBack = onBack, modifier = Modifier.padding(horizontal = 12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            RouteMapView(
                spot = spot,
                webKey = webKey,
                focusedStop = focusedStop,
                onExitFocus = { focusedStop = null },
            )

            Spacer(Modifier.height(16.dp))
            SpotHeader(spot = spot)

            Spacer(Modifier.height(14.dp))
            ModeSelector(
                selected = mode,
                totalMinutes = spot.totalMinutes(mode),
                onSelect = { mode = it },
            )

            Spacer(Modifier.height(14.dp))
            RoutePlanItinerary(
                spot = spot,
                mode = mode,
                focusedStop = focusedStop,
                detailOpen = detailOpen,
                onFocusStop = { focusedStop = it },
                onToggleDetail = { detailOpen = it },
            )

            Spacer(Modifier.height(18.dp))
            AiCustomizeButton(onAiCustomize)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * 地图卡片：三态自洽，不再出现「永远转圈」。
 * - 加载中：spinner（密钥已就绪、正在拉图）；
 * - 成功：高德静态地图整幅铺满，聚焦某站时右上角浮现「全览」小胶囊；
 * - 失败 / 无密钥：降级 [RouteMapSchematic] 示意图 + 底部原因条，点击站点仍有高亮反馈。
 */
@Composable
internal fun RouteMapView(
    spot: RouteSpot,
    webKey: String?,
    focusedStop: Int?,
    onExitFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val url = webKey?.takeIf { it.isNotBlank() }?.let { buildRouteMapUrl(spot, it, focusedStop) }.orEmpty()
    var result by remember(url) { mutableStateOf<StaticMapResult?>(null) }
    LaunchedEffect(url) {
        result = if (url.isBlank()) null else fetchStaticMap(url)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MapPlaceholderBackground),
    ) {
        val ready = (result as? StaticMapResult.Ready)?.bitmap
        when {
            url.isBlank() -> {
                // 无密钥（游客 / 服务端未配置 / 拉取失败）：示意图兜底，不空转
                RouteMapSchematic(spot = spot, focusedStop = focusedStop)
                MapReasonBar(result?.let { (it as? StaticMapResult.Failed)?.reason }
                    ?: "未获取到地图密钥——登录后由服务端下发「Web服务」Key 即可显示真实地图")
            }
            ready != null -> {
                Image(
                    bitmap = ready,
                    contentDescription = "${spot.name} 路线图",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize(),
                )
                if (focusedStop != null) {
                    Text(
                        text = "全览",
                        color = TextWhite,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SettingCardBackground.copy(alpha = 0.92f))
                            .clickable(onClick = onExitFocus)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            result is StaticMapResult.Failed -> {
                RouteMapSchematic(spot = spot, focusedStop = focusedStop)
                MapReasonBar((result as StaticMapResult.Failed).reason)
            }
            else -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = AccentOrange,
                strokeWidth = 2.dp,
            )
        }
    }
}

/** 示意图底部的一行原因说明：半透明衬底，说清「现在是示意、为什么不是真地图」；只能在地图卡片的 Box 内调用 */
@Composable
private fun BoxScope.MapReasonBar(reason: String) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(SettingCardBackground.copy(alpha = 0.88f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = reason,
            color = TextGray500,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            maxLines = 2,
        )
    }
}

/** 景点简介：名称 + 标签行 + 两行内的短文，信息克制、不铺开 */
@Composable
private fun SpotHeader(spot: RouteSpot) {
    Column {
        Text(
            text = spot.name,
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${spot.tags} · ${spot.recommend} · ★ ${spot.rating}",
            color = AccentOrange,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = spot.intro,
            color = TextGray400,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
    }
}

/** 出行方式横排胶囊：选中态强调橙描底；右侧同步给出该方式的全程总用时 */
@Composable
private fun ModeSelector(
    selected: TravelMode,
    totalMinutes: Int,
    onSelect: (TravelMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TravelMode.entries.forEach { m ->
                ModeChip(m.label, m == selected) { onSelect(m) }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (totalMinutes >= 60) "全程约 ${totalMinutes / 60} 小时" else "全程约 $totalMinutes 分钟",
            color = TextGray500,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) AccentOrange else TextGray400,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) RouteChipSelectedBackground else SettingCardBackground)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
    )
}

/** 底部唯一行动点：回聊天并真实发问，AI 会调 route_plan 读路线后向用户提定制问题 */
@Composable
private fun AiCustomizeButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WhiteBtnBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.mini_route),
            contentDescription = null,
            colorFilter = ColorFilter.tint(WhiteBtnIcon),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "让 AI 帮我定制这条路线",
            color = WhiteBtnIcon,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
