package com.xlin.travel.qrcode

import org.jetbrains.compose.resources.DrawableResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.food_ice_room
import travel.app.shared.generated.resources.food_lanling_jiqing
import travel.app.shared.generated.resources.food_map
import travel.app.shared.generated.resources.food_shanhaiguan
import travel.app.shared.generated.resources.historic_bagong
import travel.app.shared.generated.resources.historic_lihuangpi
import travel.app.shared.generated.resources.historic_map
import travel.app.shared.generated.resources.historic_xiananfang
import travel.app.shared.generated.resources.jingming_building
import travel.app.shared.generated.resources.lihuangpi_road
import travel.app.shared.generated.resources.parent_child_dongting81
import travel.app.shared.generated.resources.parent_child_erchang
import travel.app.shared.generated.resources.parent_child_map
import travel.app.shared.generated.resources.parent_child_stamp
import travel.app.shared.generated.resources.photo_spot_bagong
import travel.app.shared.generated.resources.photo_spot_map
import travel.app.shared.generated.resources.photo_spot_red_wall
import travel.app.shared.generated.resources.photo_spot_tongxingli
import travel.app.shared.generated.resources.quick_tour_map
import travel.app.shared.generated.resources.street_life_ice_room
import travel.app.shared.generated.resources.street_life_map
import travel.app.shared.generated.resources.street_life_tiansheng
import travel.app.shared.generated.resources.street_life_xiananfang
import travel.app.shared.generated.resources.xiananfang

/**
 * 线路上的一个景点：介绍横幅图（图内自带名称与简介）；
 * image 为 null 时（如"关于"）用图标行卡片。
 * 点击景点进入的详情页当前统一显示"开发中.."，后续按景点补充真实内容。
 */
data class TourStop(
    val displayName: String,
    val image: DrawableResource?,
)

/**
 * 一条里份游览线路：4:3 地图 + 若干景点，
 * 六条线路共用 RouteTourScreen 同一套模板渲染。
 */
data class TourRoute(
    val title: String,
    val mapImage: DrawableResource,
    val stops: List<TourStop>,
)

/** 六条线路的内容表：素材来自 参考文件/ 目录，已按资源名导入 drawable */
object LifenTourRoutes {

    val quickTour = TourRoute(
        title = "一小时快速游览线",
        mapImage = Res.drawable.quick_tour_map,
        stops = listOf(
            TourStop("咸安坊", Res.drawable.xiananfang),
            TourStop("景明大楼", Res.drawable.jingming_building),
            TourStop("黎黄陂路", Res.drawable.lihuangpi_road),
            TourStop("关于", null),
        ),
    )

    val streetLife = TourRoute(
        title = "市井烟火体验游",
        mapImage = Res.drawable.street_life_map,
        stops = listOf(
            TourStop("咸安坊居民区", Res.drawable.street_life_xiananfang),
            TourStop("天声街菜场", Res.drawable.street_life_tiansheng),
            TourStop("老冰室", Res.drawable.street_life_ice_room),
        ),
    )

    val photoSpot = TourRoute(
        title = "网红拍照打卡游",
        mapImage = Res.drawable.photo_spot_map,
        stops = listOf(
            TourStop("咸安坊红墙", Res.drawable.photo_spot_red_wall),
            TourStop("巴公房子", Res.drawable.photo_spot_bagong),
            TourStop("同兴里", Res.drawable.photo_spot_tongxingli),
        ),
    )

    val parentChild = TourRoute(
        title = "亲子趣味探索游",
        mapImage = Res.drawable.parent_child_map,
        stops = listOf(
            TourStop("咸安坊集章", Res.drawable.parent_child_stamp),
            TourStop("洞庭街81号", Res.drawable.parent_child_dongting81),
            TourStop("汉口二厂怀旧乐园", Res.drawable.parent_child_erchang),
        ),
    )

    val historicBuilding = TourRoute(
        title = "历史建筑深度游",
        mapImage = Res.drawable.historic_map,
        stops = listOf(
            TourStop("咸安坊", Res.drawable.historic_xiananfang),
            TourStop("巴公房子", Res.drawable.historic_bagong),
            TourStop("黎黄陂路", Res.drawable.historic_lihuangpi),
        ),
    )

    val foodTour = TourRoute(
        title = "美食探店专线",
        mapImage = Res.drawable.food_map,
        stops = listOf(
            TourStop("山海关路", Res.drawable.food_shanhaiguan),
            TourStop("老冰室", Res.drawable.food_ice_room),
            TourStop("兰陵古路与吉庆街", Res.drawable.food_lanling_jiqing),
        ),
    )
}