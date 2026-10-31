package com.xlin.travel.agent.tool.kb

/**
 * 知识库的一章：[id] 既是文件名前缀也是模型填的章节号；
 * [keywords] 只用于把用户问题路由到章节（不进上下文），[summary] 出现在目录里。
 */
internal class LifenChapter(
    val id: String,
    val title: String,
    val file: String,
    val summary: String,
    val keywords: List<String>,
)

/**
 * 武汉里份知识库的章节表：与 composeResources/files/lifen 下的 markdown 一一对应。
 *
 * 为什么写死在代码里：知识库是随包发布的说明书，不是用户数据；
 * 章节表只维护「哪一章讲什么」，正文仍放在资源文件里，改内容不用改 Kotlin。
 */
internal object LifenKnowledgeCatalog {

    val chapters: List<LifenChapter> = listOf(
        LifenChapter("00", "目录与导读", "00-index.md", "知识库结构、各章要点与常见问法", listOf("目录", "导读", "索引", "总览")),
        LifenChapter("01", "里份概述", "01-overview.md", "里份的定义、分类、分布与价值", listOf("什么是里份", "定义", "分类", "里弄", "胡同", "数量", "分布")),
        LifenChapter("02", "历史沿革", "02-history.md", "1861 年开埠以来的七个发展阶段", listOf("历史", "开埠", "1861", "租界", "发展", "阶段", "年代")),
        LifenChapter("03", "建筑特色", "03-architecture.md", "石库门、天井、老虎窗与里份建筑智慧", listOf("建筑", "石库门", "天井", "老虎窗", "亭子间", "红砖", "结构", "钢窗蜡板")),
        LifenChapter("04", "咸安坊", "04-xian-an-fang.md", "咸安坊的历史、名人与修缮现状", listOf("咸安坊", "卢作孚", "陈伯华", "同仁里")),
        LifenChapter("05", "同兴里", "05-tong-xing-li.md", "私家花园改造而来的市井里份", listOf("同兴里", "私家花园", "1928", "中产")),
        LifenChapter("06", "泰兴里", "06-tai-xing-li.md", "1907 年建成的中档里份与文艺改造", listOf("泰兴里", "1907", "叶澄衷", "四明银行", "店铺")),
        LifenChapter("07", "延庆里", "07-yan-qing-li.md", "D 级危房到城市新地标的改造历程", listOf("延庆里", "1933", "危房", "改造", "活化")),
        LifenChapter("08", "其他重要里份", "08-other-lifen.md", "汉润里、永康里、楚宝片、保元里、巴公房子", listOf("汉润里", "永康里", "楚宝", "保元里", "巴公房子", "其他里份")),
        LifenChapter("09", "游玩攻略", "09-travel-guide.md", "最佳时间、游览时长、必做事项与注意事项", listOf("游玩", "攻略", "最佳", "季节", "门票", "时长", "体验", "开放")),
        LifenChapter("10", "推荐路线", "10-routes.md", "精华一日游、深度两日游、摄影与亲子专线", listOf("路线", "一日游", "两日游", "摄影", "亲子", "美食", "周末", "安排")),
        LifenChapter("11", "交通指南", "11-transport.md", "地铁、公交、停车与步行衔接", listOf("交通", "地铁", "公交", "停车", "步行", "江汉路", "黎黄陂路", "怎么去")),
        LifenChapter("12", "美食推荐", "12-food.md", "里份周边过早、小吃与餐厅", listOf("美食", "热干面", "小吃", "餐厅", "豆皮", "过早", "吃")),
        LifenChapter("13", "摄影指南", "13-photography.md", "机位、光线、镜头与拍照打卡点", listOf("摄影", "拍照", "打卡", "机位", "光线", "镜头")),
        LifenChapter("14", "名人故居", "14-celebrities.md", "里份里的名人旧居与故事", listOf("名人", "故居", "旧居", "故事")),
        LifenChapter("15", "文化价值", "15-culture.md", "里份的文化意义与城市记忆", listOf("文化", "价值", "意义", "生活方式", "记忆")),
        LifenChapter("16", "市井生活", "16-daily-life.md", "居民日常与里份烟火气", listOf("市井", "生活", "居民", "烟火", "日常")),
        LifenChapter("17", "保护现状", "17-preservation.md", "保护政策、修缮进度与挂牌情况", listOf("保护", "现状", "修缮", "政策", "挂牌")),
        LifenChapter("18", "改造案例", "18-renovation.md", "城市更新案例与设计手法", listOf("改造", "案例", "更新", "设计")),
        LifenChapter("19", "商业开发", "19-commerce.md", "业态、招商、运营与民宿", listOf("商业", "开发", "业态", "招商", "运营", "民宿")),
        LifenChapter("20", "实用信息", "20-practical.md", "开放时间、地址、电话等实用信息", listOf("实用", "信息", "开放时间", "电话", "地址", "厕所")),
        LifenChapter("21", "活动表演", "21-events.md", "展览、演出、市集与节庆活动", listOf("活动", "表演", "展览", "演出", "市集", "节庆")),
    )

    /** 目录：先给结构再给用法，模型据此决定翻哪一章，避免整库塞进上下文 */
    fun toc(): String = buildString {
        append("[武汉里份知识库 · 目录]\n")
        append("共 ${chapters.size} 章（离线写死，非实时数据）。先看目录选章节，再带 chapter + query 取具体段落。\n")
        chapters.forEach { append("${it.id} ${it.title}：${it.summary}\n") }
    }

    /** 按章节号（00 / 04 / 04-咸安坊）或标题解析；认不出返回 null */
    fun byIdOrTitle(hint: String): LifenChapter? {
        if (hint.isBlank()) return null
        val trimmed = hint.trim()
        val number = trimmed.take(2).takeIf { it.all { c -> c.isDigit() } }
        number?.let { id -> chapters.firstOrNull { it.id == id }?.let { return it } }
        return chapters.firstOrNull { it.title.isNotBlank() && (trimmed.contains(it.title) || it.title.contains(trimmed)) }
            ?: chapters.firstOrNull { chapter -> chapter.keywords.any { trimmed.contains(it) } }
    }

    /** 只有关键词时猜最相关的一章：标题 / 关键词命中权重高于简介 */
    fun bestMatch(query: String): LifenChapter? {
        if (query.isBlank()) return null
        val tokens = LifenKnowledgeTokens.of(query)
        if (tokens.isEmpty()) return null
        return chapters
            .map { chapter ->
                val score = tokens.sumOf { token ->
                    countOf(chapter.title, token) * 6 +
                        chapter.keywords.sumOf { countOf(it, token) } * 4 +
                        countOf(chapter.summary, token) * 2
                }
                chapter to score
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun countOf(text: String, token: String): Int {
        if (token.isEmpty()) return 0
        var count = 0
        var index = text.indexOf(token)
        while (index >= 0) {
            count++
            index = text.indexOf(token, index + token.length)
        }
        return count
    }
}

/**
 * 中文检索的分词：空格与标点切词 + 汉字二元组。
 * 用户常连写成「咸安坊门票多少」，整串匹配不到任何词，二元组才能命中「咸安坊」「门票」。
 */
internal object LifenKnowledgeTokens {

    fun of(query: String): List<String> {
        val words = query
            .split(' ', '　', '，', ',', '、', '。', '.', '？', '?', '！', '!', '：', ':', '的')
            .map { it.trim() }
            .filter { it.length >= 2 }
        val cjk = query.filter { it in '\u4e00'..'\u9fff' }
        val grams = if (cjk.length >= 2) (0..cjk.length - 2).map { cjk.substring(it, it + 2) } else emptyList()
        return (words + grams).distinct().take(16)
    }
}
