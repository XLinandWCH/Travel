package com.xlin.travel.agent.tool.lifen

/**
 * 票务与避坑写死数据：服务「@门票咨询」与「@避坑指南」。
 *
 * 票务条目与景点目录分开维护：路线里的免费点位只需一句“免费”，
 * 但票务工具要额外交代预约方式与闭馆日，所以单独列一份可执行口径。
 */

/** 一条票务：点位 + 价格 + 预约方式 + 补充说明 */
data class LifenTicket(
    val name: String,
    val price: String,
    val booking: String,
    val note: String,
) {
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "")
        if (key.isEmpty()) return true
        return listOf(name, price, booking, note).any { it.replace(" ", "").contains(key) }
    }
}

object LifenTickets {

    private val all = listOf(
        LifenTicket("唐华里·里份博物馆", "免费", "现场登记或馆方小程序预约", "周一闭馆；馆内恒温，适合午后避晒"),
        LifenTicket("咸安坊", "免费开放", "无需预约", "街区全天可进，部分店铺自有营业时间"),
        LifenTicket("江汉关博物馆", "免费", "需提前预约（身份证入馆）", "周一闭馆；整点前后人最多"),
        LifenTicket("武汉美术馆（汉口馆）", "免费", "馆方小程序预约", "周一闭馆，周末 11 点后排队"),
        LifenTicket("沿江大道江滩", "免费", "无需预约", "全天开放，夜间沿江步道照明一般"),
        LifenTicket("长江轮渡（武汉关—中华路）", "约 ¥1.5/人", "码头闸机扫码或投币", "约 10 分钟一班，末班以码头公告为准"),
        LifenTicket("夜游长江（游船）", "约 ¥120 起", "官方公众号选班次购票", "甲板位先到先得，勿买黄牛票"),
        LifenTicket("知音号（漂移剧场）", "约 ¥238 起", "官方渠道提前 1–3 天购票", "登船需安检，迟到不退不改"),
        LifenTicket("汉秀（水舞秀）", "约 ¥180 起", "官方渠道购票", "在武昌，需过江，注意场次时间"),
    )

    /** 门票咨询正文：按点位/价格关键词筛选，命不中退回全量 */
    fun answer(spotName: String): String {
        val key = spotName.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】票务信息")
            when {
                key.isEmpty() -> append("（全部点位与项目）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的票务，先给全部）")
                else -> append("（查询：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, ticket ->
                append(index + 1).append(". ").append(ticket.name).append("｜").append(ticket.price)
                    .append("｜").append(ticket.booking).append('\n')
                append("   ").append(ticket.note).append('\n')
            }
            append("省心组合：唐华里 + 咸安坊 + 江滩全部免费，只有夜游船票要花钱；")
            append("周一来的话，两座博物馆都闭馆，建议把行程换成“街区 + 江滩 + 轮渡”。")
            append('\n').append("优惠：学生、60 岁以上老人、现役军人按各场馆公示执行，需带证件。")
            append('\n').append(LifenScene.DATA_NOTE)
        }
    }
}

/** 一条避坑：场景 + 风险 + 做法 */
data class LifenPitfall(
    val scene: String,
    val risk: String,
    val advice: String,
) {
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "")
        if (key.isEmpty()) return true
        return listOf(scene, risk, advice).any { it.replace(" ", "").contains(key) }
    }
}

object LifenPitfalls {

    private val all = listOf(
        LifenPitfall(
            scene = "闭馆日",
            risk = "周一跑空：唐华里·里份博物馆、江汉关博物馆、武汉美术馆（汉口馆）都闭馆",
            advice = "行程前先确认当天星期；周一改走街区 + 江滩 + 轮渡，照样可以玩满半天",
        ),
        LifenPitfall(
            scene = "江汉关整点",
            risk = "整点报时前后钟楼下方人挤，拍照全是人头，台阶上易被推挤",
            advice = "提前 10 分钟到侧面机位，或干脆在江滩远拍；带小孩的避开钟楼正下方",
        ),
        LifenPitfall(
            scene = "咸安坊拍照",
            risk = "主街网红机位排队十几分钟，部分店铺“拍照需消费”",
            advice = "岔进安静的小里份拍空镜；进店先问是否收费，别默认免费",
        ),
        LifenPitfall(
            scene = "里份居民区",
            risk = "里份仍是居民生活区，擅自推门进院、大声喧哗容易起冲突",
            advice = "只走公共巷道，院内与楼道不进入；拍照避开住户门窗与晾晒",
        ),
        LifenPitfall(
            scene = "游船与演出",
            risk = "码头周边黄牛兜售“内部票”“包上甲板”，价格翻倍且无法验票",
            advice = "只在官方公众号 / 售票窗口买；登船前核对班次与身份证信息",
        ),
        LifenPitfall(
            scene = "江滩安全",
            risk = "涨水期围栏外湿滑、夜间江风大，夏季还有暴晒与蚊虫",
            advice = "不翻越围栏、不靠近水线；夜里备薄外套与驱蚊水，夏天带足饮水",
        ),
        LifenPitfall(
            scene = "交通拉客",
            risk = "江汉路口“三轮车 / 黑车”报价含糊，绕路加价",
            advice = "用网约车或地铁；确需短途先谈好总价，拒绝“到地方再说”",
        ),
        LifenPitfall(
            scene = "网红店消费",
            risk = "黎黄陂路咖啡馆周末排队 30 分钟起，部分加收服务费",
            advice = "工作日或上午去；点单前看价目表，确认是否含服务费",
        ),
        LifenPitfall(
            scene = "户部巷",
            risk = "游客密度高、同类小吃价格偏高，体验不如社区早点铺",
            advice = "想吃地道早点去黎黄陂路与胜利街；户部巷当作路过看看即可",
        ),
    )

    /** 避坑指南正文：按场景关键词筛选，命不中退回全量 */
    fun answer(scene: String): String {
        val key = scene.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】避坑指南")
            when {
                key.isEmpty() -> append("（按最容易踩的顺序排）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的条目，先给全部）")
                else -> append("（场景：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, pit ->
                append(index + 1).append(". ").append(pit.scene).append("：").append(pit.risk).append('\n')
                append("   → ").append(pit.advice).append('\n')
            }
            append("三条底线：门票只走官方渠道、不进居民家门、不翻江滩围栏。")
            append('\n').append(LifenScene.DATA_NOTE)
        }
    }
}
