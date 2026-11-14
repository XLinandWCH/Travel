package com.xlin.travel.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.chevron_right
import travel.app.shared.generated.resources.help

/**
 * 里份游览线路通用页（六条线路共用，模板即原"一小时快速游览线"）：
 * 顶部 4:3 地图完整显示、点击进全屏查看；
 * 下方景点按钮直接用介绍横幅图（图内自带名称与简介），点击进各自详情页。
 * 固定暖色（[LifenColors]），不随黑白主题切换。
 */
@Composable
fun RouteTourScreen(
    route: TourRoute,
    onBack: () -> Unit,
    onSpotClick: (TourStop) -> Unit = {},
    onMapClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LifenPageBrush)
            .safeContentPadding(),
    ) {
        LifenTopBar(title = route.title, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 地图：与原图同为 4:3，完整显示不裁剪；纸色描边像旧地图的裱框
            Image(
                painter = painterResource(route.mapImage),
                contentDescription = "${route.title}地图",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, LifenLine, RoundedCornerShape(16.dp))
                    .clickable(onClick = onMapClick),
            )
            Text(
                text = "点击地图可放大查看",
                color = LifenInkSoft,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            // 栏目标题：红砖短竖条 + 深棕墨字
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(LifenBrick),
                )
                Text(
                    text = "沿途看点",
                    color = LifenInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // 景点按钮：介绍横幅图本身就是按钮，宽度撑满、高度随原图比例
            route.stops.forEach { stop ->
                TourStopButton(
                    stop = stop,
                    onClick = { onSpotClick(stop) },
                )
            }
        }
    }
}

/**
 * 景点按钮：有介绍图的直接铺图（各图比例不一，按原图比例完整显示）；
 * 无图的（"关于"）用暖白卡片 + 图标 + 文字行。
 */
@Composable
private fun TourStopButton(
    stop: TourStop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = stop.image
    if (image != null) {
        Image(
            painter = painterResource(image),
            contentDescription = stop.displayName,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, LifenLine, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LifenCard)
                .border(1.dp, LifenLine, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.help),
                contentDescription = null,
                colorFilter = ColorFilter.tint(LifenBrick),
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stop.displayName,
                color = LifenInk,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = painterResource(Res.drawable.chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(LifenInkSoft),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}