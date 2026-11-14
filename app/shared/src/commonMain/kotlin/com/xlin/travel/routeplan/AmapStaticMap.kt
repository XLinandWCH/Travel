package com.xlin.travel.routeplan

import com.xlin.travel.db.ChatRepository
import com.xlin.travel.login.createHttpClient
import com.xlin.travel.map.MapKeysStore
import io.ktor.client.call.body
import io.ktor.client.request.get
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 高德静态地图渲染（路线规划小程序的地图卡片）。
 *
 * 静态地图属于高德【Web服务】API，必须用 Web服务类型的 Key：服务端 map 表按平台分行，
 * ANDROID / IOS 行是 SDK 密钥（绑定包名/SHA1，调 restapi 会报 USERKEY_PLAT_NOMATCH），
 * 因此这里固定取 WEB 行（MapKeysStore.ensure(repository, "WEB")，仍不硬编码密钥）。
 *
 * URL 按官方文档拼装：全览不传 location/zoom，由高德按「所有标注/折线的几何中心」自动取景；
 * 折线走 paths（weight,color,transparency,fillcolor,fillTransparency:点串，transparency 0~1），
 * 标注走 markers（size,color,label:坐标，A~E 与时间线序号同一套编号），颜色即全局强调橙。
 *
 * 出图失败不伪装成「一直加载中」：Loading / Ready / Failed(可读原因) 三态，
 * 失败时由调用方（RouteMapView）降级为自绘路线示意图。
 */

/** 强调橙的高德 0xRRGGBB 表示（与 AppColors.AccentOrange #FF5722 一致） */
private const val AMAP_ACCENT = "0xFF5722"

/** 逻辑出图尺寸：scale=2 时实收 960*600（高清且不超 1024 上限） */
private const val STATIC_MAP_SIZE = "480*300"

/**
 * 拼装静态地图 URL。
 * - focusIndex == null：全览，橙色折线串联全部途经点 + A/B/C… 字母标注，几何中心自动取景；
 * - focusIndex >= 0：聚焦单个途经点，large 标注居中、zoom 16，其余要素收起。
 */
fun buildRouteMapUrl(spot: RouteSpot, apiKey: String, focusIndex: Int?): String {
    val labels = "ABCDEFGHIJK"
    return if (focusIndex == null) {
        val markers = spot.stops.mapIndexed { i, s ->
            "mid,$AMAP_ACCENT,${labels[i]}:${coord(s.lng)},${coord(s.lat)}"
        }.joinToString("|")
        val pathPoints = spot.stops.joinToString(";") { "${coord(it.lng)},${coord(it.lat)}" }
        "https://restapi.amap.com/v3/staticmap?size=$STATIC_MAP_SIZE&scale=2" +
            "&markers=$markers&paths=8,$AMAP_ACCENT,0.95,,,:$pathPoints&key=$apiKey"
    } else {
        val stop = spot.stops[focusIndex]
        "https://restapi.amap.com/v3/staticmap?location=${coord(stop.lng)},${coord(stop.lat)}" +
            "&zoom=16&size=$STATIC_MAP_SIZE&scale=2" +
            "&markers=large,$AMAP_ACCENT,${labels[focusIndex]}:${coord(stop.lng)},${coord(stop.lat)}&key=$apiKey"
    }
}

/** 高德要求经纬度小数点后不超过 6 位 */
private fun coord(value: Double): String =
    value.toString().trimEnd('0').trimEnd('.').ifBlank { "0" }

/** 取 WEB 行密钥；游客 / 拉取失败返回 null（调用方降级示意图） */
suspend fun ensureWebMapKey(repository: ChatRepository?): String? {
    if (repository == null) return null
    return runCatching { MapKeysStore.ensure(repository, platform = "WEB") }
        .getOrNull()?.apiKey?.takeIf { it.isNotBlank() }
}

/** 地图请求结果：成功位图 / 可读失败原因 */
sealed interface StaticMapResult {
    data class Ready(val bitmap: ImageBitmap) : StaticMapResult
    data class Failed(val reason: String) : StaticMapResult
}

/**
 * 请求静态地图。高德出错时返回的是小体积 JSON（status=0 + info），
 * 这里把它翻译成用户能看懂的原因（密钥类型不符 / 无效 Key / 超额 / 断网），绝不静默吞掉。
 */
internal suspend fun fetchStaticMap(url: String): StaticMapResult {
    if (url.isBlank()) return StaticMapResult.Failed("未获取到地图密钥")
    return try {
        val bytes: ByteArray = staticMapClient.get(url).body()
        val text = bytes.decodeToString()
        // 正常出图是 PNG 二进制（不会以 { 开头）；以 { 开头说明是高德的错误 JSON
        if (bytes.size < 1_000 && text.trimStart().startsWith("{")) {
            StaticMapResult.Failed(describeAmapError(text))
        } else {
            decodeMapImageBytes(bytes)?.let { StaticMapResult.Ready(it) }
                ?: StaticMapResult.Failed("地图图片解析失败")
        }
    } catch (t: Throwable) {
        StaticMapResult.Failed("连不上高德地图服务，请检查网络")
    }
}

/** 高德错误码 → 可读原因（前缀匹配 info 字段即可，不必完整解析 JSON） */
private fun describeAmapError(json: String): String {
    val info = Regex("\"info\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1).orEmpty()
    return when {
        info.startsWith("USERKEY_PLAT_NOMATCH") ->
            "地图 Key 平台类型不符：静态地图需要「Web服务」类型的 Key"
        info.startsWith("INVALID_USER_KEY") -> "地图 Key 无效（服务端 map 表 WEB 行未配置）"
        info.contains("OVER_QUOTA") || info.contains("OVER_LIMIT") -> "今日地图请求已达上限，明天再看吧"
        info.isNotBlank() -> "地图服务出错（$info）"
        else -> "地图服务返回了异常内容"
    }
}

/** 单引擎复用：文件级 lazy，避免每次进页重建 HttpClient */
private val staticMapClient by lazy { createHttpClient() }
