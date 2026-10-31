package com.xlin.travel.agent.tool.lifen

/**
 * 美食与演出写死数据：分别服务「@美食攻略」与「@表演时间」两个小程序工具。
 *
 * 店名、价格、时段均为演示期固定文案（不联网、不查点评），
 * 回答里统一带上「以现场公示为准」，避免用户把演示价当成实时价。
 */

/** 一条美食：店/摊 + 位置 + 价格 + 招牌 + 营业时段 + 点单提醒 */
data class LifenFood(
    val name: String,
    val where: String,
    val price: String,
    val signature: String,
    val hours: String,
    val tip: String,
) {
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "")
        if (key.isEmpty()) return true
        return listOf(name, where, signature, tip).any { it.replace(" ", "").contains(key) }
    }

    fun line(index: Int): String = buildString {
        append(index).append(". ").append(name).append("｜").append(where).append("｜").append(price).append('\n')
        append("   招牌：").append(signature).append('\n')
        append("   时段：").append(hours).append("；").append(tip).append('\n')
    }
}

object LifenFoods {

    private val all = listOf(
        LifenFood(
            name = "热干面",
            where = "蔡林记（江汉路步行街店）或黎黄陂路早点摊",
            price = "约 ¥8–12",
            signature = "芝麻酱拌碱水面，配一杯蛋酒解腻",
            hours = "06:00–21:00",
            tip = "拌之前加一勺卤水更顺口；早高峰排队 10 分钟起",
        ),
        LifenFood(
            name = "三鲜豆皮",
            where = "老通城（吉庆街附近）",
            price = "约 ¥10–15",
            signature = "糯米 + 肉丁 + 香菇，外皮煎到焦香",
            hours = "07:00–20:00",
            tip = "要现煎的，出锅 5 分钟内最好吃，凉了发硬",
        ),
        LifenFood(
            name = "排骨藕汤",
            where = "沿江大道一带的湖北菜馆",
            price = "约 ¥38–58/份",
            signature = "粉藕炖到起沙，汤色奶白",
            hours = "11:00–14:00 / 17:00–21:00",
            tip = "两人点小份就够，配一份炒青菜解腻",
        ),
        LifenFood(
            name = "糊汤粉",
            where = "户部巷老摊 / 汉口老社区早点铺",
            price = "约 ¥8–12",
            signature = "胡椒味重的鱼糊汤配细米粉，加油条蘸着吃",
            hours = "06:30–11:00（只做早市）",
            tip = "下午去基本扑空，想吃得赶早",
        ),
        LifenFood(
            name = "面窝",
            where = "街边早点摊（黎黄陂路、胜利街口都有）",
            price = "约 ¥3–5/个",
            signature = "中间薄脆、边上一圈厚软，米浆现炸",
            hours = "06:30–10:30",
            tip = "现炸才脆，袋装闷一会儿就返软",
        ),
        LifenFood(
            name = "糯米包油条",
            where = "黎黄陂路早点铺",
            price = "约 ¥6–8",
            signature = "甜（白糖芝麻）咸（榨菜肉松）两味",
            hours = "06:30–10:30",
            tip = "边走边吃最合适，配豆浆刚好",
        ),
        LifenFood(
            name = "鸭脖 / 鸭锁骨",
            where = "精武、周黑鸭（江汉路沿线多家）",
            price = "约 ¥20–35/份",
            signature = "甜辣卤味，越嚼越香",
            hours = "09:00–22:00",
            tip = "真空装可带走当伴手礼，现卤的当天吃完",
        ),
        LifenFood(
            name = "吉庆街夜宵烧烤",
            where = "吉庆街（大智路方向，步行约 15 分钟）",
            price = "人均 ¥50–80",
            signature = "烤虾球、烤藕片、凉拌毛豆配冰啤酒",
            hours = "17:30–次日 02:00",
            tip = "22 点后最热闹；夏天备驱蚊水，先问价再点",
        ),
    )

    /** 美食攻略正文：按口味/场景关键词筛选，命不中退回全量 */
    fun answer(preference: String): String {
        val key = preference.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】美食攻略")
            when {
                key.isEmpty() -> append("（从早到晚一条街吃过去）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的店，先给全部推荐）")
                else -> append("（口味/场景：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, food -> append(food.line(index + 1)) }
            append("路线建议：早上在黎黄陂路吃面窝 + 糊汤粉，中午藕汤配青菜，")
            append("晚上逛完江滩去吉庆街吃夜宵。").append('\n')
            append("提醒：户部巷游客多、价格偏高，本地人更常去吉庆街与社区早点铺。").append('\n')
            append(LifenScene.DATA_NOTE)
        }
    }
}

/** 一场演出/活动：名称 + 地点 + 场次 + 时长 + 票价 + 提醒 */
data class LifenShow(
    val name: String,
    val venue: String,
    val sessions: String,
    val duration: String,
    val price: String,
    val tip: String,
) {
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "")
        if (key.isEmpty()) return true
        return listOf(name, venue, sessions, tip).any { it.replace(" ", "").contains(key) }
    }
}

object LifenShows {

    private val all = listOf(
        LifenShow(
            name = "长江灯光秀",
            venue = "沿江大道两岸楼宇 / 汉口江滩",
            sessions = "每晚 19:00–22:00 滚动播放，节假日加场",
            duration = "约 15 分钟一轮",
            price = "免费",
            tip = "江滩三阳广场到武汉关一带最正；江风大，备件薄外套",
        ),
        LifenShow(
            name = "江汉关钟声",
            venue = "江汉关大楼（沿江大道）",
            sessions = "整点报时（10:00–22:00）",
            duration = "约 1 分钟",
            price = "免费",
            tip = "提前 10 分钟到，钟声与江面同框最值得",
        ),
        LifenShow(
            name = "夜游长江（游船）",
            venue = "武汉关码头 / 中华路码头",
            sessions = "19:00–21:30 多班，单程约 60–90 分钟",
            duration = "约 60–90 分钟",
            price = "约 ¥120 起",
            tip = "官方公众号购票选班次；甲板位先到先得，别买黄牛票",
        ),
        LifenShow(
            name = "知音号（漂移剧场）",
            venue = "汉口江滩知音号码头",
            sessions = "通常 19:30 / 21:00 两场",
            duration = "约 100 分钟",
            price = "约 ¥238 起",
            tip = "需提前 1–3 天购票；可穿复古装扮，登船要安检",
        ),
        LifenShow(
            name = "汉秀（水舞秀）",
            venue = "武昌汉秀剧场（需过江）",
            sessions = "通常 19:30 一场",
            duration = "约 90 分钟",
            price = "约 ¥180 起",
            tip = "过江预留 1 小时交通；与江滩夜景同日只能二选一",
        ),
    )

    /** 表演时间正文：按日期/时段关键词筛选，命不中退回全量 */
    fun answer(date: String): String {
        val key = date.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】演出与活动场次")
            when {
                key.isEmpty() -> append("（按傍晚到夜里的顺序排）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的场次，先给全部）")
                else -> append("（查询：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, show ->
                append(index + 1).append(". ").append(show.name).append("｜").append(show.venue).append('\n')
                append("   场次：").append(show.sessions).append("（").append(show.duration).append("）\n")
                append("   票价：").append(show.price).append("；").append(show.tip).append('\n')
            }
            append("串联建议：19:00 江滩看灯光秀 → 20:00 江汉关听整点钟声 → 20:30 武汉关码头上船夜游。")
            append('\n').append("演出场次随季节调整，出发前请再核对官方当日公告。")
        }
    }
}
