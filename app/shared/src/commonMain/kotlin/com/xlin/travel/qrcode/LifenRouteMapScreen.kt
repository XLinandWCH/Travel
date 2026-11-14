package com.xlin.travel.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.route_food_tour
import travel.app.shared.generated.resources.route_historic_building
import travel.app.shared.generated.resources.route_parent_child
import travel.app.shared.generated.resources.route_photo_spot
import travel.app.shared.generated.resources.route_quick_tour
import travel.app.shared.generated.resources.route_street_life

/** 图片叠加层上的文字固定用暖白，不随主题反转（衬底始终是深色渐变） */
private val OnImageText = Color(0xFFF7F1E4)

/**
 * 路线类别枚举：每个类别对应一张封面图与一条线路内容（[TourRoute]），
 * 六个类别全部可点击进入各自的线路页。
 */
enum class RouteCategory(
    val title: String,
    val subtitle: String,
    val image: DrawableResource,
    val route: TourRoute,
) {
    QUICK_TOUR("一小时快速游览线", "咸安坊 · 景明大楼 · 黎黄陂路", Res.drawable.route_quick_tour, LifenTourRoutes.quickTour),
    STREET_LIFE("市井烟火体验游", "走进老汉口的市井日常", Res.drawable.route_street_life, LifenTourRoutes.streetLife),
    PHOTO_SPOT("网红拍照打卡游", "热门机位，一拍即出片", Res.drawable.route_photo_spot, LifenTourRoutes.photoSpot),
    PARENT_CHILD("亲子趣味探索游", "带娃边玩边学", Res.drawable.route_parent_child, LifenTourRoutes.parentChild),
    HISTORIC_BUILDING("历史建筑深度游", "读懂百年建筑的故事", Res.drawable.route_historic_building, LifenTourRoutes.historicBuilding),
    FOOD_TOUR("美食探店专线", "地道汉味，边逛边吃", Res.drawable.route_food_tour, LifenTourRoutes.foodTour),
}

/**
 * 里份路线图页面：
 * 扫码后跳转的页面，六个大类做成图片封面卡片，全部可点击进入对应线路页。
 * 固定暖色（[LifenColors]），不随黑白主题切换。
 */
@Composable
fun LifenRouteMapScreen(
    onBack: () -> Unit,
    onRouteClick: (RouteCategory) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LifenPageBrush)
            .safeContentPadding(),
    ) {
        LifenTopBar(title = "里份路线图", onBack = onBack)

        Text(
            text = "选一条路线，走进汉口老里份",
            color = LifenInkSoft,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // 六个大类图片卡片网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(RouteCategory.entries.size) { index ->
                RouteCategoryCard(
                    category = RouteCategory.entries[index],
                    onClick = { onRouteClick(RouteCategory.entries[index]) },
                )
            }
        }
    }
}

/**
 * 路线类别封面卡片：整卡铺封面图，底部深棕渐变衬底上叠标题与一句话简介。
 */
@Composable
private fun RouteCategoryCard(
    category: RouteCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(category.image),
            contentDescription = category.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // 底部深棕渐变衬底（带棕味而非纯黑），保证浅色封面图上的暖白字可读
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xE63A281C)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = category.title,
                color = OnImageText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(
                text = category.subtitle,
                color = OnImageText.copy(alpha = 0.75f),
                fontSize = 11.sp,
                maxLines = 2,
            )
        }
    }
}