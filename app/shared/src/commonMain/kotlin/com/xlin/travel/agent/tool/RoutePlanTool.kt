package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.routeplan.RouteSpot
import com.xlin.travel.routeplan.TravelMode
import com.xlin.travel.routeplan.buildItineraryText
import kotlinx.serialization.Serializable

/**
 * 路线规划工具：小程序「路线规划」的核心能力暴露给 AI。
 *
 * 界面（routeplan 包）与工具共用同一份 [RouteSpot] 数据与
 * [buildItineraryText] 格式化口径——用户在小程序里看到的路线，
 * 和模型通过本工具读到的路线完全一致（含坐标与高德静态地图 URL，
 * 模型可据此向用户复述行程或解释站点取舍）。
 *
 * 使用场景（对应小程序底部「让 AI 帮我定制这条路线」入口）：
 * 用户提出改路线 / 加站点 / 换出行方式时，模型先调用本工具拿到基线行程再作答。
 * 当前景点库写死为武汉·里份；后续云端景点库接入后，未命中景点的兜底文案同步更新。
 */
object RoutePlanTool : Tool<RoutePlanTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "route_plan",
    description = "获取景点的完整游览路线：途经点顺序、到达时间、各点游玩时长与门票/贴士、"
        + "相邻点之间的距离与出行用时，以及可展示的高德静态地图信息。"
        + "当用户点击「@路线规划」小程序、询问某景点怎么玩、路线怎么排，或要求定制行程时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("景点名称，例如“武汉里份”；留空则返回全部可用景点目录")
        val spotName: String = "",
        @property:LLMDescription("通勤方式：walk（步行）/ ride（骑行）/ drive（驾车），默认 walk")
        val travelMode: String = "walk",
    )

    override suspend fun execute(args: Args): String {
        val keyword = args.spotName.trim()
        if (keyword.isEmpty()) {
            return "（route_plan 目前收录的景点：" +
                RouteSpot.catalog.joinToString("、") { "${it.name}（${it.city}）" } + "）"
        }
        // 宽松匹配：去空格后互相包含即命中，"里份"/"武汉里份"/"武汉 · 里份" 都能查到
        val compact = keyword.replace(" ", "").replace("·", "")
        val spot = RouteSpot.catalog.firstOrNull { candidate ->
            val name = candidate.name.replace(" ", "").replace("·", "")
            name.contains(compact) || compact.contains(name)
        } ?: return "（暂无「$keyword」的路线数据，目前提供：" +
            RouteSpot.catalog.joinToString("、") { it.name } + "；可提示用户先在路线规划小程序中选择。）"

        val mode = TravelMode.entries.firstOrNull { it.name.lowercase() == args.travelMode.lowercase() }
            ?: TravelMode.Walk
        return buildItineraryText(spot, mode)
    }
}
