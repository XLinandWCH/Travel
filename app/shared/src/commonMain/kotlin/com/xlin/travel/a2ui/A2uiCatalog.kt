package com.xlin.travel.a2ui

/**
 * 组件目录 + 排版契约：这是「AI 选组件」时唯一的说明书。
 *
 * [specs] 是给人和模型看的组件清单；[contract] 会被压缩成一小段中文拼进每轮提示词，
 * 告诉模型「有哪些 type、每个 type 填什么、一屏最多放几块」。
 * 渲染器只认 [canonicalType] 归一化之后的名字，模型写了别名也能落到正确组件上。
 */
object A2uiCatalog {

    /** 一个组件的用法说明（type + 用途 + 关键字段） */
    data class Spec(val type: String, val usage: String, val fields: String)

    /** 19 个组件：覆盖「头图 / 图片 / 正文 / 列表 / 数据 / 提示 / 操作」七类信息 */
    val specs: List<Spec> = listOf(
        Spec(A2uiType.Header, "页头：大标题 + 副标题 + 一句说明", "title, subtitle, text"),
        Spec(A2uiType.Hero, "主视觉卡：一屏只放一个，放最上面", "title, subtitle, icon, items(关键词)"),
        Spec(A2uiType.Image, "图片卡：全宽图 + 图注，一屏最多一张", "image(map/scenic/food/route), title, text"),
        Spec(A2uiType.Paragraph, "普通段落：一段完整的话", "title, text"),
        Spec(A2uiType.Bullets, "要点列表：短句 + 一句话解释", "items[{title,text,icon,meta}]"),
        Spec(A2uiType.CardList, "卡片列表：每个点位 / 店铺一张描边卡", "items[{title,subtitle,text,meta,badge,icon}]"),
        Spec(A2uiType.GridCards, "双列小卡：4–6 个短条目，比 cardList 更紧凑", "items[{title,meta,icon}]"),
        Spec(A2uiType.KeyValue, "键值表：门票、时长、地址等成对信息", "items[{title,text}]"),
        Spec(A2uiType.Chips, "标签组：免费、室内、夜景这类关键词", "items[{title}]"),
        Spec(A2uiType.Steps, "步骤条：按顺序做的一二三四", "items[{title,text}]"),
        Spec(A2uiType.Timeline, "时间线：带时间的行程 / 场次", "items[{meta,title,text}]"),
        Spec(A2uiType.StatRow, "指标横排：2–3 个数字对比", "stats[{label,value,unit}]"),
        Spec(A2uiType.Notice, "提示条：需要提醒时才用，info 为中性", "level(info|warn|danger), title, text"),
        Spec(A2uiType.Quote, "引用块：一句讲解词 / 金句", "text, subtitle(出处)"),
        Spec(A2uiType.Divider, "细分割线", "—"),
        Spec(A2uiType.Progress, "进度条：拥挤度、完成度", "title, value, max, subtitle"),
        Spec(A2uiType.Rating, "评分条：评分 / 推荐指数", "title, value, max, text"),
        Spec(A2uiType.ActionBar, "操作按钮：点击后把 prompt 发回聊天，固定底部", "actions[{label,prompt,style}]"),
        Spec(A2uiType.Spacer, "留白", "value(8–48 像素)"),
    )

    /** 别名归一化：模型常写的近义词都落到标准组件上，写错则退化为段落 */
    fun canonicalType(raw: String): String = when (raw.trim().lowercase()) {
        "title", "heading", "headerblock" -> A2uiType.Header
        "cover", "banner", "maincard" -> A2uiType.Hero
        "image", "img", "photo", "picture", "map" -> A2uiType.Image
        "text", "body", "p" -> A2uiType.Paragraph
        "list", "bullet", "bulletlist", "points" -> A2uiType.Bullets
        "cards", "cardlist", "card" -> A2uiType.CardList
        "grid", "gridcards", "gridcard", "tiles" -> A2uiType.GridCards
        "kv", "keyvalue", "table", "info" -> A2uiType.KeyValue
        "tags", "tag", "chips", "chip" -> A2uiType.Chips
        "step", "steps", "howto" -> A2uiType.Steps
        "timeline", "schedule", "agenda" -> A2uiType.Timeline
        "stats", "statrow", "metrics" -> A2uiType.StatRow
        "tip", "alert", "warning", "notice" -> A2uiType.Notice
        "quote", "blockquote" -> A2uiType.Quote
        "line", "divider", "hr" -> A2uiType.Divider
        "progress", "bar", "crowd" -> A2uiType.Progress
        "rating", "score", "stars" -> A2uiType.Rating
        "actions", "actionbar", "buttons" -> A2uiType.ActionBar
        "spacer", "space", "gap" -> A2uiType.Spacer
        else -> A2uiType.Paragraph
    }

    /**
     * 拼进系统提示的排版契约（约 450 字，尽量短）。
     *
     * 除了「有哪些 type」，这里额外压两条硬约束：
     * 1. 正文不许出现 JSON / 字段名 / 组件名，界面条目也不在正文复述——否则聊天里会出现
     *    「正文 + 一段 token」这种最难看的拼接；
     * 2. 颜色、强调色由客户端统一分配，模型不要为了「丰富」硬塞 notice / progress / rating。
     */
    fun contract(): String = buildString {
        append("[个性化界面契约] 正文只写一次结论与建议；写完后调用一次 a2ui_render 排出一屏界面。")
        append("调用之后立即结束本轮：不要再写正文、总结或邀请语，否则入口下方会再长出一段重复正文。")
        append("正文里绝不能出现 JSON、字段名、组件名，也不要复述界面里的条目——这些都会原样显示给用户。\n")
        append("结构：surface{title(≤12字), subtitle(地点·时长·适合人群), blocks[]}；一屏 4–7 块，")
        append("按「概览 → 地图 → 内容 → 提示 → 操作」的顺序排，每块就是一个模块。\n")
        append("可用 type：header、hero、image、paragraph、bullets、cardList、gridCards、keyValue、chips、steps、")
        append("timeline、statRow、notice、quote、divider、progress、rating、actionBar、spacer。\n")
        append("模块分工：1) 首块 header 或 hero 交代「这是什么」；2) 涉及地点时放 1 块 image(image=map, title 写图注)；")
        append("3) 内容块 2–3 个：cardList（点位/店铺）、gridCards（4–6 个短条目）、timeline（带时间的行程）、")
        append("steps（有先后顺序）、keyValue（门票/时长/地址）、statRow（2–3 个数字）、bullets、chips；")
        append("4) 真有必要才放 notice（info 为中性，warn/danger 仅用于真实风险）；5) 最后一块必须是 actionBar。\n")
        append("约束：列表 3–6 条，title ≤10 字，text 一句话（≤30 字），时间/距离/价格放 meta；")
        append("actionBar 2–3 个按钮，第一个 style=primary，prompt 写成用户点击后会发回聊天的完整问句；")
        append("数据只能来自本轮工具结果，价格与场次不得编造；颜色与强调色由客户端控制，不要为凑数硬塞组件。")
    }
}
