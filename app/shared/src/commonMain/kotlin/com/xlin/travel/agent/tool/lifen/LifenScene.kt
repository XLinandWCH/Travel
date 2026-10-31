package com.xlin.travel.agent.tool.lifen

import com.xlin.travel.routeplan.RouteSpot

/**
 * 武汉 · 里份 景区基线数据（12 个小程序工具共用的写死数据源）。
 *
 * 景点主目录直接复用 route_plan 的 [RouteSpot.catalog]（同一份数据、同一套文案口径），
 * 再补几处顺路点位，避免「AI 推荐」与「路线规划」两处内容互相对不上。
 * 演示期不查云端：接入云端景点库后只需替换 [LifenSpots.all] 的构造来源，
 * 各工具的输出格式与提示词都不用改。
 */

/** 景区背景与公共服务信息：所有工具作答时都沿用这一份口径，不让模型自由发挥 */
object LifenScene {
    const val NAME = "武汉 · 里份"
    const val CITY = "武汉"
    const val AREA = "汉口江岸区：黎黄陂路—咸安坊—江汉关—沿江大道"
    const val INTRO = "「里份」是武汉特有的巷弄民居：北方叫胡同、南方叫弄堂，武汉把院落写进砖石里。" +
        "从黎黄陂路的唐华里出发，穿过租界旧街与咸安坊，到江汉关、江滩收尾，" +
        "是老汉口与新汉口交织的一条半日线。"
    const val BEST_TIME = "14:30–21:00（下午进场，江滩看日落，夜里坐船收尾）"
    const val TAGS = "历史建筑 · 街巷美食 · 江滩夜景"
    const val SERVICE_CENTER = "黎黄陂路路口游客服务点（唐华里·里份博物馆旁）"
    const val DATA_NOTE = "注：演示期内容写死在本地，票价与开放时间请以现场公示为准。"
}

/**
 * 景点条目：推荐清单、景点讲解、票务工具共用同一份字段。
 *
 * [tags] 是给「偏好筛选」用的关键词串（拍照/免费/夜游/亲子…），留空时按名称与看点匹配。
 */
data class LifenSpot(
    val name: String,
    val stayMinutes: Int,
    val ticket: String,
    val highlight: String,
    val tip: String,
    val nearby: String,
    val tags: String = "",
) {
    /** 宽松匹配：关键词与名称/标签/看点互相包含即命中，「咸安坊拍照」这类连写也能查到 */
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "")
        if (key.isEmpty()) return true
        if (keyword.contains(name)) return true
        return listOf(name, tags, highlight, tip, nearby).any { it.replace(" ", "").contains(key) }
    }
}

/** 景点目录：route_plan 的 5 个站点 + 顺路补充点位 */
object LifenSpots {

    /** 不在主路线里、但走两步就能到的免费点位 */
    private val extra = listOf(
        LifenSpot(
            name = "黎黄陂路街头博物馆",
            stayMinutes = 30,
            ticket = "免费开放",
            highlight = "一条街就是展柜：老租界建筑立面配梧桐树影，随手一张就是汉口味",
            tip = "沿路往江边方向走，早上 9 点前最清爽；周末下午人流密集",
            nearby = "巴公房子 · 唐华里·里份博物馆",
            tags = "拍照 建筑 免费 步行 梧桐 情侣",
        ),
        LifenSpot(
            name = "巴公房子",
            stayMinutes = 20,
            ticket = "免费（外观打卡）",
            highlight = "1910 年代的俄式公寓楼，红砖转角像一艘停在街口的船",
            tip = "对角路口才能拍全三角形平面；楼内是居民住宅，请勿进入",
            nearby = "黎黄陂路 · 合作路",
            tags = "拍照 建筑 免费 历史 冷知识",
        ),
        LifenSpot(
            name = "武汉美术馆（汉口馆）",
            stayMinutes = 60,
            ticket = "免费预约（周一闭馆）",
            highlight = "原金城银行大楼改造，中庭光线好，常年有湖北近现代美术展",
            tip = "带身份证件、提前在小程序预约；周末 11 点后进馆要排队",
            nearby = "咸安坊 · 中山大道",
            tags = "博物馆 免费 避暑 亲子 室内",
        ),
        LifenSpot(
            name = "平和打包厂",
            stayMinutes = 40,
            ticket = "免费（部分展馆单独收费）",
            highlight = "英式工业厂房改的文创园，红砖楼梯与铁艺窗是合照热门位",
            tip = "工作日上午几乎没人；部分区域是办公区，跟着指引走",
            nearby = "青岛路 · 咸安坊",
            tags = "拍照 工业风 免费 文创 建筑",
        ),
        LifenSpot(
            name = "吉庆街",
            stayMinutes = 90,
            ticket = "免费（消费自理）",
            highlight = "老汉口的夜宵与民间艺人聚集地，晚上比白天热闹",
            tip = "17:30 后开市；先看价目再点单，街头表演打赏随意",
            nearby = "大智路 · 武汉美术馆（汉口馆）",
            tags = "夜宵 美食 夜间 烟火气 免费",
        ),
    )

    /** 全部点位：前 5 个与 route_plan 完全同源，顺序即推荐顺序 */
    val all: List<LifenSpot> = buildList {
        RouteSpot.catalog.flatMap { spot -> spot.stops }.forEach { stop ->
            add(
                LifenSpot(
                    name = stop.name,
                    stayMinutes = stop.stayMinutes,
                    ticket = stop.ticket,
                    highlight = stop.highlight,
                    tip = stop.tip,
                    nearby = stop.nearby,
                )
            )
        }
        addAll(extra)
    }

    /** 名称宽松匹配，供讲解、票务等单点工具使用 */
    fun find(keyword: String): LifenSpot? = all.firstOrNull { it.matches(keyword) }

    /** 兜底提示里的可用点位清单 */
    fun names(): String = all.joinToString("、") { it.name }

    /** 景点推荐正文：命中偏好就筛选，命不中就退回全量并说明原因 */
    fun recommend(preference: String): String {
        val key = preference.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】景点推荐")
            when {
                key.isEmpty() -> append("（全部 ").append(all.size).append(" 个点位）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的点位，先给全部点位）")
                else -> append("（偏好：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, spot ->
                append(index + 1).append(". ").append(spot.name)
                    .append("｜建议 ").append(spot.stayMinutes).append(" 分钟｜").append(spot.ticket)
                    .append('\n')
                append("   看点：").append(spot.highlight).append('\n')
                append("   贴士：").append(spot.tip).append('\n')
                append("   顺路：").append(spot.nearby).append('\n')
            }
            append("区域：").append(LifenScene.AREA).append("；建议时段：").append(LifenScene.BEST_TIME)
            append('\n').append(LifenScene.DATA_NOTE)
        }
    }
}
