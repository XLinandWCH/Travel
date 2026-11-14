package com.xlin.travel.agent.tool.lifen

/**
 * 交通与客流写死数据：服务「@交通出行」与「@客流分析」。
 *
 * 交通以“从哪来、怎么进场、怎么收尾离场”三段写；客流是一张分时段拥挤度表，
 * 让模型能直接回答“人多不多 / 几点来合适”，而不是笼统说“建议错峰”。
 */

/** 一条交通方案：方式 + 路线 + 用时 + 花费 + 提醒 */
data class LifenTransit(
    val mode: String,
    val route: String,
    val duration: String,
    val cost: String,
    val tip: String,
) {
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "")
        if (key.isEmpty()) return true
        return listOf(mode, route, duration, cost, tip).any { it.replace(" ", "").contains(key) }
    }
}

object LifenTraffic {

    private val all = listOf(
        LifenTransit(
            mode = "地铁",
            route = "2 / 6 号线「江汉路站」C 口 → 江汉路步行街 → 咸安坊（约 600 米）",
            duration = "出站后步行 8 分钟",
            cost = "按里程计费",
            tip = "从汉口火车站方向来最快；晚高峰车厢很挤，错开 17:30–19:00",
        ),
        LifenTransit(
            mode = "地铁",
            route = "1 / 6 号线「大智路站」A 口 → 黎黄陂路（约 500 米）",
            duration = "出站后步行 7 分钟",
            cost = "按里程计费",
            tip = "适合先去吉庆街吃夜宵、再往江边走的下半场路线",
        ),
        LifenTransit(
            mode = "轮渡",
            route = "武昌「中华路码头」⇄ 汉口「武汉关码头」",
            duration = "航程约 10 分钟，约 10 分钟一班",
            cost = "约 ¥1.5/人",
            tip = "最便宜的过江方式，还能顺路看江景；末班时间以码头公告为准",
        ),
        LifenTransit(
            mode = "自驾",
            route = "导航「咸安坊地下停车场」或「沿江大道公共停车场」",
            duration = "市区内约 20–40 分钟",
            cost = "按时计费，节假日上浮",
            tip = "咸安坊车位少，10 点后常满；江汉路步行街全段禁行，别往街里开",
        ),
        LifenTransit(
            mode = "打车",
            route = "上/下车点建议定「沿江大道武汉关」或「胜利街黎黄陂路路口」",
            duration = "市区内约 0.5 小时",
            cost = "起步价 + 里程",
            tip = "步行街与咸安坊主街不能临时停车，定位到路口再走进去",
        ),
        LifenTransit(
            mode = "步行",
            route = "唐华里 → 咸安坊 → 江汉关 → 江滩 → 中华路码头（全程约 5.4 公里）",
            duration = "含游玩约 5 小时，纯走路约 1 小时 10 分",
            cost = "免费",
            tip = "平路多、树荫少，夏天备水和遮阳；鞋底软一点更舒服",
        ),
        LifenTransit(
            mode = "共享单车",
            route = "沿江大道江滩段有指定停放点",
            duration = "江滩到中华路码头约 15 分钟",
            cost = "按时计费",
            tip = "步行街与里份巷道禁骑，进街区前先在停放点还车",
        ),
    )

    /** 交通出行正文：按方式/路线关键词筛选，命不中退回全量 */
    fun answer(from: String): String {
        val key = from.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】交通出行")
            when {
                key.isEmpty() -> append("（进场—串联—离场三段）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的方案，先给全部）")
                else -> append("（查询：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, transit ->
                append(index + 1).append(". ").append(transit.mode).append("｜").append(transit.route).append('\n')
                append("   用时：").append(transit.duration).append("；花费：").append(transit.cost).append('\n')
                append("   提醒：").append(transit.tip).append('\n')
            }
            append("收尾建议：江滩看完夜景后，从武汉关码头坐轮渡回武昌，或沿江大道打车离场，")
            append("比挤江汉路地铁省时间。").append('\n')
            append(LifenScene.DATA_NOTE)
        }
    }
}

/** 一条客流时段：时间 + 拥挤度 + 说明 */
data class LifenFlowSlot(
    val time: String,
    val level: String,
    val note: String,
) {
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "").replace("：", ":")
        if (key.isEmpty()) return true
        return listOf(time, level, note).any { it.replace(" ", "").contains(key) } || level in keyword
    }
}

object LifenFlow {

    private val all = listOf(
        LifenFlowSlot("08:00–10:00", "低", "街区刚开门，早点摊最舒服，适合拍照和吃早餐"),
        LifenFlowSlot("10:00–12:00", "中", "旅行团陆续进场，唐华里与咸安坊主街开始排队"),
        LifenFlowSlot("12:00–14:00", "中高", "午饭高峰，藕汤馆与网红店都要等位"),
        LifenFlowSlot("14:00–16:00", "中", "午后最晒，博物馆与美术馆是最佳避暑选择"),
        LifenFlowSlot("16:00–18:00", "高", "进景区看日落的人流叠加，江滩观景带最挤"),
        LifenFlowSlot("18:00–20:00", "高峰", "灯光秀 + 夜游船叠加，码头与江滩入口排队明显"),
        LifenFlowSlot("20:00–22:00", "中", "第一波看完灯光秀的人离场，江滩反而好走"),
        LifenFlowSlot("22:00 后", "低", "街区渐静，只剩吉庆街夜宵还热闹"),
    )

    /** 客流分析正文：按时段关键词筛选，命不中退回全天曲线 */
    fun answer(time: String): String {
        val key = time.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】客流分析")
            when {
                key.isEmpty() -> append("（工作日全天曲线，周末整体上浮一档）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的时段，先给全天曲线）")
                else -> append("（查询：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, slot ->
                append(index + 1).append(". ").append(slot.time).append("｜").append(slot.level)
                    .append("｜").append(slot.note).append('\n')
            }
            append("错峰建议：工作日 09:00–11:00 和 20:00 之后最舒服；")
            append("周末尽量 09:00 前进场，把江滩与游船放到 20:00 以后。").append('\n')
            append("拥挤度最高的是咸安坊主街与江滩观景带，想拍空镜就岔进小里份。").append('\n')
            append(LifenScene.DATA_NOTE)
        }
    }
}
