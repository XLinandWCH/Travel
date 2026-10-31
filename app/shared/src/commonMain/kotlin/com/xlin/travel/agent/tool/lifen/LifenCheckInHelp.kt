package com.xlin.travel.agent.tool.lifen

/**
 * 打卡与求助写死数据：服务「@打卡分享」与「@求助咨询」。
 *
 * 打卡给的是「机位 + 时间 + 构图」这种能直接照做的信息；
 * 求助给的是「去哪找谁 + 电话」，把全国通用热线的 12301 / 12345 与 110 / 120 放在最前。
 */

/** 一个机位：点位 + 最佳时间 + 站位/构图 + 提醒 + 话题标签 */
data class LifenCheckInSpot(
    val name: String,
    val bestTime: String,
    val angle: String,
    val tip: String,
    val hashtag: String,
) {
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "")
        if (key.isEmpty()) return true
        return listOf(name, angle, tip, hashtag).any { it.replace(" ", "").contains(key) }
    }
}

object LifenCheckIn {

    private val all = listOf(
        LifenCheckInSpot(
            name = "咸安坊拱门",
            bestTime = "09:00 前或 16:30 后",
            angle = "站在拱门内侧往外拍，用门框做前景，人站在三分线偏左",
            tip = "主街正门排队最长，往街区深处走还有两个同样的拱门",
            hashtag = "#汉口里份 #咸安坊",
        ),
        LifenCheckInSpot(
            name = "巴公房子转角",
            bestTime = "上午顺光 / 傍晚亮灯后",
            angle = "对角路口拍三角形平面，把梧桐树纳入画面左侧",
            tip = "用 0.5 倍广角才装得下整栋；车流多，别站到路中间",
            hashtag = "#巴公房子 #老建筑",
        ),
        LifenCheckInSpot(
            name = "江汉关钟楼",
            bestTime = "整点前 10 分钟",
            angle = "沿江大道斜 45° 拍，钟楼 + 车流长曝光；或从江滩回望",
            tip = "人像建议背对夕阳、面向钟楼，脸不会黑",
            hashtag = "#江汉关 #武汉钟声",
        ),
        LifenCheckInSpot(
            name = "江滩芦苇带",
            bestTime = "日落前 30 分钟（10–11 月芦花开）",
            angle = "低机位让芦苇占前景，人物放右侧留白给夕阳",
            tip = "顺光拍剪影最好看；夏季涨水期注意围栏与湿滑",
            hashtag = "#武汉江滩 #日落",
        ),
        LifenCheckInSpot(
            name = "黎黄陂路梧桐街景",
            bestTime = "08:00–09:30",
            angle = "沿街纵深构图，让梧桐树影落在柏油路上",
            tip = "早上人少车少，能拍到干净的街景；周末下午全是人",
            hashtag = "#黎黄陂路 #CityWalk",
        ),
        LifenCheckInSpot(
            name = "平和打包厂红砖楼梯",
            bestTime = "工作日上午",
            angle = "仰拍楼梯转角，红砖墙做背景，人站在台阶中段",
            tip = "部分区域是办公区，跟指引走、控制音量",
            hashtag = "#平和打包厂 #工业风",
        ),
    )

    /** 打卡分享正文：按风格/机位关键词筛选，命不中退回全量 */
    fun answer(style: String): String {
        val key = style.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】打卡机位")
            when {
                key.isEmpty() -> append("（按动线顺序排，边逛边拍）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的机位，先给全部）")
                else -> append("（风格：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, spot ->
                append(index + 1).append(". ").append(spot.name).append("｜最佳时间：").append(spot.bestTime).append('\n')
                append("   机位：").append(spot.angle).append('\n')
                append("   提醒：").append(spot.tip).append("（").append(spot.hashtag).append("）\n")
            }
            append("分享模板：先放一张空镜 + 一张人像，配文写清时间与机位，别人更容易照着走。")
            append('\n').append("拍居民门窗、晾晒衣物请先征得同意；商用拍摄需向管理方报备。")
        }
    }
}

/** 一条求助指引：场景 + 找谁 + 提示 */
data class LifenHelpItem(
    val scene: String,
    val channel: String,
    val tip: String,
) {
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "")
        if (key.isEmpty()) return true
        return listOf(scene, channel, tip).any { it.replace(" ", "").contains(key) }
    }
}

object LifenHelp {

    private val all = listOf(
        LifenHelpItem("紧急情况", "110 / 120 / 119", "说清所在点位（如“咸安坊正门”“江汉关钟楼下”），原地等待"),
        LifenHelpItem("普通咨询 / 投诉", "12345 政务服务热线；文旅投诉 12301", "记录时间、地点、商家名称与金额，便于后续处理"),
        LifenHelpItem("现场问路 / 借物", LifenScene.SERVICE_CENTER, "可借充电宝、雨伞（押金），可领纸质地图"),
        LifenHelpItem("人员走失", "游客服务点广播 + 110", "先报走失者衣着特征与最后出现点位，别自行远距离寻找"),
        LifenHelpItem("身体不适 / 中暑", "游客服务点 + 120；就近医院 武汉市中心医院（南京路院区）", "先移到阴凉处补水，江滩段可请巡逻人员协助"),
        LifenHelpItem("物品遗失", "游客服务点失物登记 + 110", "说明遗失时间与点位；游船、轮渡遗失可联系码头值班室"),
        LifenHelpItem("无障碍 / 母婴", "游客服务点", "可借轮椅（需押证件），母婴室在服务点内侧"),
        LifenHelpItem("消费纠纷", "12315 市场监管 + 12345", "保留小票或支付记录，先与商家协商再升级投诉"),
    )

    /** 求助咨询正文：按问题关键词筛选，命不中退回全量 */
    fun answer(issue: String): String {
        val key = issue.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】求助咨询")
            when {
                key.isEmpty() -> append("（先紧急、后常规）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的条目，先给全部）")
                else -> append("（问题：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, help ->
                append(index + 1).append(". ").append(help.scene).append(" → ").append(help.channel).append('\n')
                append("   ").append(help.tip).append('\n')
            }
            append("提醒：涉及人身安全先打 110 / 120，再联系服务点；不要在江边、马路上停留处理。")
        }
    }
}
