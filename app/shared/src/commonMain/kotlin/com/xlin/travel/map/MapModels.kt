package com.xlin.travel.map

/**
 * 地图业务模型（纯数据，跨平台共享）：路线点、站点、定位结果。
 *
 * 这里是 MAP 功能的「业务层」——只描述数据与业务规则，不出现任何平台 SDK 类型；
 * 平台差异（高德 Android/iOS/JS SDK、系统定位）全部收敛到 LocationProvider 的 expect/actual 与后续各端封装。
 */

/** 经纬度坐标（WGS-84 语义；高德返回的 GCJ-02 也先按该结构承载，转换工具后续补） */
data class GeoPoint(
    val lat: Double,
    val lng: Double,
) {
    /** 高德坐标串约定 "lng,lat"（途经点 / 起终点在 REST 与 JS API 里都是这个格式） */
    fun toAmapString(): String = "$lng,$lat"
}

/** 一次定位的结果：坐标 + 精度（米）+ 采集时间（epoch 毫秒） */
data class LocationInfo(
    val point: GeoPoint,
    val accuracyMeters: Double,
    val timeMillis: Long,
)

/** 路线上的一个点：地铁站 / 景点 / 任意位置（当前只放定位与命名，路径规划后续在此基础上扩展） */
data class RoutePoint(
    val name: String,
    val location: GeoPoint,
)

/** 站点：公交站 / 地铁站等（含所属线路名，供线路查询与换乘展示） */
data class Station(
    val name: String,
    val lineName: String,
    val location: GeoPoint,
)

/** 一条出行路线：起点 → 途经站点 → 终点；[distanceMeters] / [durationSeconds] 规划结果回填，未规划时为 null */
data class TravelRoute(
    val origin: RoutePoint,
    val destination: RoutePoint,
    val stations: List<Station> = emptyList(),
    val distanceMeters: Int? = null,
    val durationSeconds: Int? = null,
)
