package com.xlin.travel.agent.tool.lifen

/**
 * 周边配套写死数据：服务「@周边查询」。
 *
 * 只收录步行 20 分钟内真正用得上的点位（交通 / 停车 / 吃喝 / 如厕 / 寄存 / 医疗），
 * 距离以咸安坊或黎黄陂路为参照点估算，回答里会明确写“约”。
 */
data class LifenNearbyItem(
    val category: String,
    val name: String,
    val distance: String,
    val note: String,
) {
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "")
        if (key.isEmpty()) return true
        return listOf(category, name, note).any { it.replace(" ", "").contains(key) }
    }
}

object LifenNearby {

    private val all = listOf(
        LifenNearbyItem("地铁", "江汉路站（2 / 6 号线）", "距咸安坊约 600 米", "C 口出站沿江汉路步行街向北，步行 8 分钟"),
        LifenNearbyItem("地铁", "大智路站（1 / 6 号线）", "距黎黄陂路约 500 米", "A 口出站步行 7 分钟，适合从吉庆街方向进场"),
        LifenNearbyItem("停车", "咸安坊地下停车场", "约 300 米", "车位少，节假日 10 点后常满，建议早到或停沿江大道"),
        LifenNearbyItem("停车", "沿江大道公共停车场", "约 800 米", "按时计费，离江滩最近，看完夜景取车方便"),
        LifenNearbyItem("餐饮", "吉庆街夜宵街", "约 1.2 公里", "17:30 后开市，烧烤与民间艺人集中在街中段"),
        LifenNearbyItem("餐饮", "户部巷小吃街", "过江后中华路码头旁", "游客密度高、价格偏高，赶时间可以不去"),
        LifenNearbyItem("便利店", "罗森 / Today（黎黄陂路店）", "约 150 米", "24 小时营业，可买水、雨具与创可贴"),
        LifenNearbyItem("药店", "老百姓大药房（胜利街）", "约 400 米", "08:00–22:00，常用药与晕车药都有"),
        LifenNearbyItem("厕所", "咸安坊公共卫生间", "约 200 米", "入口在街区东侧，免费，节假日排队"),
        LifenNearbyItem("厕所", "江滩公园公厕", "约 400 米", "沿江步道内侧，夜间照明一般"),
        LifenNearbyItem("寄存", "江汉路站智能寄存柜", "约 600 米", "按小时计费，大件行李需到服务点人工寄存"),
        LifenNearbyItem("服务", "游客服务点（黎黄陂路路口）", "约 50 米", "可借充电宝、雨伞（押金），可领纸质地图"),
        LifenNearbyItem("医疗", "武汉市中心医院（南京路院区）", "约 1.5 公里", "急诊 24 小时，打车 8 分钟"),
    )

    /** 周边查询正文：按类别/名称筛选，命不中退回全量 */
    fun answer(category: String): String {
        val key = category.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】周边查询")
            when {
                key.isEmpty() -> append("（步行 20 分钟内可用的配套）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的点位，先给全部）")
                else -> append("（查询：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, item ->
                append(index + 1).append(". [").append(item.category).append("] ").append(item.name)
                    .append("｜").append(item.distance).append('\n')
                append("   ").append(item.note).append('\n')
            }
            append("参照点：咸安坊 / 黎黄陂路路口；游客服务点可问路、借雨具、要纸质地图。")
            append('\n').append(LifenScene.DATA_NOTE)
        }
    }
}
