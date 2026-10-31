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

    /** 17 个组件：覆盖「头图 / 正文 / 列表 / 数据 / 提示 / 操作」六类信息 */
    val specs: List<Spec> = listOf(
        Spec(A2uiType.Header, "页头：大标题 + 副标题 + 一句说明", "title, subtitle, text"),
        Spec(A2uiType.Hero, "主视觉卡：一屏只放一个，放最上面", "title, subtitle, icon, items(徽标)"),
        Spec(A2uiType.Paragraph, "普通段落：一段完整的话", "title, text"),
        Spec(A2uiType.Bullets, "要点列表：短句 + 一句话解释", "items[{title,text,icon,meta}]"),
        Spec(A2uiType.CardList, "卡片列表：每个点位 / 店铺一张卡", "items[{title,subtitle,text,meta,badge,icon}]"),
        Spec(A2uiType.KeyValue, "键值表：门票、时长、地址等成对信息", "items[{title,text}]"),
        Spec(A2uiType.Chips, "标签组：免费、室内、夜景这类关键词", "items[{title}]"),
        Spec(A2uiType.Steps, "步骤条：按顺序做的一二三四", "items[{title,text}]"),
        Spec(A2uiType.Timeline, "时间线：带时间的行程 / 场次", "items[{meta,title,text}]"),
        Spec(A2uiType.StatRow, "指标横排：2–4 个数字对比", "stats[{label,value,unit}]"),
        Spec(A2uiType.Notice, "提示条：注意事项、避坑提醒", "level(info|warn|danger), title, text"),
        Spec(A2uiType.Quote, "引用块：一句讲解词 / 金句", "text, subtitle(出处)"),
        Spec(A2uiType.Divider, "细分割线", "—"),
        Spec(A2uiType.Progress, "进度条：拥挤度、完成度", "title, value, max, subtitle"),
        Spec(A2uiType.Rating, "评分条：评分 / 推荐指数", "title, value, max, text"),
        Spec(A2uiType.ActionBar, "操作按钮：点击后把 prompt 发回聊天", "actions[{label,prompt,style}]"),
        Spec(A2uiType.Spacer, "留白", "value(8–48 像素)"),
    )

    /** 别名归一化：模型常写的近义词都落到 17 个标准组件上，写错则退化为段落 */
    fun canonicalType(raw: String): String = when (raw.trim().lowercase()) {
        "title", "heading", "headerblock" -> A2uiType.Header
        "cover", "banner", "maincard" -> A2uiType.Hero
        "text", "body", "p" -> A2uiType.Paragraph
        "list", "bullet", "bulletlist", "points" -> A2uiType.Bullets
        "cards", "cardlist", "card" -> A2uiType.CardList
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
     * 拼进系统提示的排版契约（约 400 字，尽量短）。
     *
     * 规则只写「模型必须遵守的硬约束」：一屏几块、列表几条、结尾必须有按钮，
     * 以及「数据只能来自工具结果」——这条能挡住大部分编造价钱的回答。
     */
    fun contract(): String = buildString {
        append("[个性化界面契约] 正文写完后，调用 a2ui_render 把这次内容整理成一屏界面，不要在正文里贴 JSON。\n")
        append("结构：surface{title, subtitle, blocks[]}；")
        append("block 通用字段：type、title、subtitle、text、icon(route|spot|nearby|avoid|show|help|ticket|food|traffic|guide|flow|checkin)、")
        append("level(info|warn|danger)、value、max、items[{title,subtitle,text,meta,icon,badge}]、")
        append("stats[{label,value,unit}]、actions[{label,prompt,style(primary|secondary)}]。\n")
        append("type 只能取：header、hero、paragraph、bullets、cardList、keyValue、chips、steps、timeline、")
        append("statRow、notice、quote、divider、progress、rating、actionBar、spacer。\n")
        append("排版契约：1) 一屏 3–6 块，首块用 hero 或 header；2) 列表最多 6 条，title 不超过 10 字，text 一句话，")
        append("时间/距离/价格放 meta；3) 结尾必须是 actionBar，2–3 个按钮，prompt 写成用户点击后会发回聊天的完整问句；")
        append("4) 内容只能来自刚调用的工具结果，价格与场次不得编造；5) 需要横向对比数字时用 statRow，不要做表格。")
    }
}
