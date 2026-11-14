package com.xlin.travel.routeplan

import kotlin.math.roundToInt

/**
 * 路线规划数据层：目前写死第一个 demo 景点「武汉·里份」。
 *
 * [RouteSpot] 是小程序的核心数据模型：景点 = 简介 + 途经点序列 + 分段行程（三种出行方式）。
 * 界面（routeplan 包）与 AI 工具（agent.tool.RoutePlanTool）共用同一份数据与格式化函数，
 * 保证「看到的路线」和「AI 说出的路线」一致；后续接入云端景点库时只需替换本文件的构造来源。
 *
 * 坐标为 GCJ-02（高德坐标系），高德静态地图与路径规划接口直接可用。
 */

/** 出行方式：label 用于界面胶囊，perMinute 是每种方式的时速（米/分钟，估算用时用） */
enum class TravelMode(val label: String, val perMinute: Int) {
    Walk("步行", 80),
    Ride("骑行", 200),
    Drive("驾车", 400),
}

/** 路线途经点：名称 / 到达时间 / 建议游玩时长 / 坐标 / 一句话亮点 / 门票 / 贴士 / 周边推荐 */
data class RouteStop(
    val name: String,
    val arrival: String,
    val stayMinutes: Int,
    val lat: Double,
    val lng: Double,
    val highlight: String,
    val ticket: String,
    val tip: String,
    val nearby: String,
)

/** 相邻两点之间的一段路：距离固定，用时随出行方式变化（[TravelMode.perMinute] 估算） */
data class RouteLeg(
    val fromName: String,
    val toName: String,
    val distanceMeters: Int,
) {
    /** 该方式下的用时文案，例如「约 13 分钟」「约 1 小时 5 分」 */
    fun durationFor(mode: TravelMode): String =
        formatMinutes((distanceMeters / mode.perMinute).coerceAtLeast(1))
}

/** 一条完整路线：景点简介 + 途经点 + 分段路 + 贴士标签 */
data class RouteSpot(
    val name: String,
    val city: String,
    val intro: String,
    val tags: String,
    val recommend: String,
    val rating: String,
    val stops: List<RouteStop>,
    val legs: List<RouteLeg>,
) {
    val totalDistanceMeters: Int get() = legs.sumOf { it.distanceMeters }

    /** 纯游玩时长（不含路上），用于「约 X 小时」的总览 */
    val sightseeingMinutes: Int get() = stops.sumOf { it.stayMinutes }

    /** 含路程的总用时（游玩 + 该方式通勤） */
    fun totalMinutes(mode: TravelMode): Int =
        sightseeingMinutes + legs.sumOf { it.distanceMeters / mode.perMinute }

    companion object {
        /** demo：汉口里份（黎黄陂路—咸安坊一带）傍晚CityWalk，5 点 4 段 */
        val WuhanLifen = RouteSpot(
            name = "武汉 · 里份",
            city = "武汉",
            intro = "「里份」是武汉特有的巷弄民居，北方叫胡同、南方弄堂，武汉把院落写进砖石里。" +
                "从唐华里出发，穿租界旧街到江汉关，最后在江边收尾，是感受老汉口与新汉口交织的经典半日线。",
            tags = "历史建筑 · 街头美食 · 夜游",
            recommend = "建议 14:30–21:00",
            rating = "4.7 分",
            stops = listOf(
                RouteStop(
                    name = "唐华里·里份博物馆",
                    arrival = "14:30", stayMinutes = 60,
                    lat = 30.5797, lng = 114.2995,
                    highlight = "全国首座以「里份」为主题的博物馆，先看百年汉口住区如何生长",
                    ticket = "免费（周一闭馆）",
                    tip = "入口在黎黄陂路路口，馆内恒温，适合行程开头避午后晒",
                    nearby = "黎黄陂路街头博物馆 · 巴公房子",
                ),
                RouteStop(
                    name = "咸安坊",
                    arrival = "15:50", stayMinutes = 50,
                    lat = 30.5749, lng = 114.2944,
                    highlight = "石库门建筑群改造街区，老拱门与清水砖墙最出片",
                    ticket = "免费开放",
                    tip = "主街人多，岔进安静的小里份拍空镜效果更好",
                    nearby = "武汉美术馆（汉口馆）",
                ),
                RouteStop(
                    name = "江汉关大楼",
                    arrival = "16:50", stayMinutes = 40,
                    lat = 30.5635, lng = 114.2913,
                    highlight = "1924 年的海关钟楼，整点钟声是汉口的时间名片",
                    ticket = "外观免费；博物馆收费（周一闭馆）",
                    tip = "碰整点前 10 分钟到，钟声与江边同框最值得",
                    nearby = "沿江大道 · 武汉关码头",
                ),
                RouteStop(
                    name = "沿江大道江滩",
                    arrival = "17:40", stayMinutes = 50,
                    lat = 30.5608, lng = 114.3008,
                    highlight = "万里长江第一滩，芦苇与轮渡码头，日落方向顺光",
                    ticket = "免费",
                    tip = "10–11 月芦花开；夏季注意江畔围栏与涨水提示",
                    nearby = "轮渡「武汉关—中华路」，1.5 元过江",
                ),
                RouteStop(
                    name = "中华路码头 · 夜游长江",
                    arrival = "19:30", stayMinutes = 90,
                    lat = 30.5465, lng = 114.3057,
                    highlight = "船上看两岸灯光秀与大桥夜景，给一天一个亮尾",
                    ticket = "夜游航线约 ¥120 起",
                    tip = "提前在官方公众号购票选班次，甲板风大备件薄外套",
                    nearby = "户部巷（夜宵街，24 点后渐静）",
                ),
            ),
            legs = listOf(
                RouteLeg("唐华里·里份博物馆", "咸安坊", 1000),
                RouteLeg("咸安坊", "江汉关大楼", 500),
                RouteLeg("江汉关大楼", "沿江大道江滩", 400),
                RouteLeg("沿江大道江滩", "中华路码头 · 夜游长江", 3500),
            ),
        )

        /** 目录页与工具兜底用的景点列表 */
        val catalog: List<RouteSpot> = listOf(WuhanLifen)
    }
}

/** 米 → 人类可读距离：<1km 显示米，否则保留一位小数的公里 */
internal fun formatDistance(meters: Int): String =
    if (meters < 1000) "${meters}米" else "${formatKm(meters)}公里"

/** 米 → 一位小数公里数（不带单位），拼接总距离时用 */
internal fun formatKm(meters: Int): String {
    val km = meters / 1000.0
    val tenth = (km * 10).roundToInt()
    return if (tenth % 10 == 0) "${tenth / 10}" else "${tenth / 10}.${tenth % 10}"
}

/** 分钟 → 「约 X 分钟 / 约 X 小时 Y 分 / 约 X 小时」 */
internal fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "约 $minutes 分钟"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "约 $h 小时" else "约 $h 小时 $m 分"
}

/**
 * 行程 → LLM 友好的纯文本（AI 工具返回值与将来落库共用此口径）。
 * 包含坐标，方便模型接续调用地图类工具或向用户复述位置。
 */
internal fun buildItineraryText(spot: RouteSpot, mode: TravelMode): String = buildString {
    append("【").append(spot.name).append("】路线（默认出行：").append(mode.label).append("）\n")
    spot.stops.forEachIndexed { i, s ->
        append(i + 1).append(". ").append(s.arrival).append(" · ").append(s.name)
        append("（").append(s.lat).append(',').append(s.lng).append("）")
        append(" 游玩").append(s.stayMinutes).append("分钟\n")
        append("   亮点：").append(s.highlight)
        .append("；门票：").append(s.ticket)
        .append("；贴士：").append(s.tip)
        .append("；周边：").append(s.nearby).append('\n')
        spot.legs.getOrNull(i)?.let { leg ->
            append("   → ").append(leg.toName).append("：")
            append(formatDistance(leg.distanceMeters)).append('，').append(leg.durationFor(mode)).append('\n')
        }
    }
    append("全程约 ").append(formatKm(spot.totalDistanceMeters)).append(" 公里，")
    TravelMode.entries.joinTo(this) { "${it.label}${spot.totalMinutes(it) / 60}小时${spot.totalMinutes(it) % 60}分" }
    append("；贴士标签：").append(spot.tags)
}
