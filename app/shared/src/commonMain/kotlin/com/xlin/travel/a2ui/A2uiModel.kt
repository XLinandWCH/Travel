package com.xlin.travel.a2ui

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A2UI（Agent-to-UI）数据模型：AI 只产出这份 JSON，客户端只做渲染。
 *
 * 三层约定（与 [A2uiCatalog] 的排版契约配套）：
 * 1. SDUI：界面结构由后端 / AI 下发的 JSON 描述，客户端不写死页面；
 * 2. A2UI：AI 只「选组件 + 填 props」，永远碰不到 Compose 代码；
 * 3. Design Tokens：组件怎么排、字号多大由 [A2uiSpace] / [A2uiText] 统一收口，
 *    AI 只管内容与顺序，不管像素。
 *
 * 字段全部给默认值：模型少填一个字段就退化成简版组件，而不是整屏渲染失败。
 */
@Serializable
data class A2uiSurface(
    @property:LLMDescription("界面主标题，不超过 12 个字，例如「里份半日步行路线」")
    val title: String = "",
    @property:LLMDescription("副标题：一句话补充时长 / 起点 / 适合人群，例如「3 小时 · 黎黄陂路起 · 适合第一次来」")
    val subtitle: String = "",
    @property:LLMDescription("界面块列表，按从上到下的顺序，一屏 4–7 块；首块用 hero 或 header，涉及地点时插一块 image，末块用 actionBar")
    val blocks: List<A2uiBlock> = emptyList(),
) {
    /** 没有任何可渲染内容时宿主页显示空态，而不是一片空白 */
    val isEmpty: Boolean get() = blocks.all { it.isEmpty }
}

/**
 * 一个界面块。19 种 `type` 共用这一份字段表——
 * 对模型来说「只记一份结构、换 type 即可」，对客户端来说解析永远不会因为多态失败。
 */
@Serializable
data class A2uiBlock(
    /** 组件类型，取值见 [A2uiType] 或 [A2uiCatalog] 中的别名 */
    @property:LLMDescription(
        "组件类型，只能取：header、hero、image、paragraph、bullets、cardList、gridCards、keyValue、chips、steps、"
            + "timeline、statRow、notice、quote、divider、progress、rating、actionBar、spacer"
    )
    val type: String = A2uiType.Paragraph,
    @property:LLMDescription("块标题：列表类组件用作分组名，不超过 10 个字；header / hero 用大标题")
    val title: String = "",
    @property:LLMDescription("块副标题，可留空")
    val subtitle: String = "",
    @property:LLMDescription("正文：paragraph / quote / notice 使用，一句到两句话；列表类组件不要填这里")
    val text: String = "",
    /** 语义图标名（route / food / spot …），由 [A2uiIcons] 映射到项目内矢量图 */
    @property:LLMDescription("语义图标名：route、spot、nearby、avoid、show、help、ticket、food、traffic、guide、flow、checkin，可留空")
    val icon: String = "",
    /** 语义图片名，仅 image 块使用；由 [A2uiImages] 映射到本地图片，认不出则退化占位 */
    @property:LLMDescription("图片名，仅 image 使用：map（武汉历史城区与里份分布图）、scenic、food、route，可留空")
    val image: String = "",
    /** notice 的语义级别：info / warn / danger */
    @property:LLMDescription("提示级别，仅 notice 使用：info / warn / danger")
    val level: String = "info",
    /** progress / rating 的当前值与上限 */
    @property:LLMDescription("当前值：progress 填 0–max 的数值，rating 填评分；spacer 填留白像素 8–48")
    val value: Float = 0f,
    @property:LLMDescription("上限值：progress 默认 100，rating 建议填 5")
    val max: Float = 100f,
    /** 多列布局提示（statRow / keyValue），1 表示默认单列 */
    @property:LLMDescription("列数提示，仅 statRow 使用，1–3；其余组件留空即可")
    val columns: Int = 1,
    @property:LLMDescription("列表条目：bullets / cardList / keyValue / chips / steps / timeline 使用，最多 6 条")
    val items: List<A2uiItem> = emptyList(),
    @property:LLMDescription("指标：仅 statRow 使用，2–4 项")
    val stats: List<A2uiStat> = emptyList(),
    @property:LLMDescription("按钮：仅 actionBar 使用，2–3 个")
    val actions: List<A2uiAction> = emptyList(),
) {
    /** 空块（模型占位或漏填）直接跳过，避免屏幕上出现空白卡片；divider / spacer 本身无文字，不算空 */
    val isEmpty: Boolean
        get() {
            val canonical = A2uiCatalog.canonicalType(type)
            if (canonical == A2uiType.Divider || canonical == A2uiType.Spacer) return false
            if (canonical == A2uiType.Image) {
                return title.isBlank() && subtitle.isBlank() && text.isBlank() && image.isBlank()
            }
            return title.isBlank() && subtitle.isBlank() && text.isBlank() &&
                items.isEmpty() && stats.isEmpty() && actions.isEmpty()
        }
}

/** 列表类组件（bullets / cardList / timeline / steps / chips）的通用条目 */
@Serializable
data class A2uiItem(
    @property:LLMDescription("条目名称，不超过 10 个字")
    val title: String = "",
    @property:LLMDescription("条目副标题：分类 / 地址等，可留空")
    val subtitle: String = "",
    @property:LLMDescription("条目正文：一句话说明，不超过 30 个字")
    val text: String = "",
    /** 时间 / 距离 / 价格等短信息 */
    @property:LLMDescription("短信息：时间 / 距离 / 价格，例如「19:30 开演」「步行 8 分钟」「免费」")
    val meta: String = "",
    @property:LLMDescription("条目图标名，取值同 block.icon，可留空")
    val icon: String = "",
    /** 右上角小徽标，例如「免费」「19:00」 */
    @property:LLMDescription("右上角徽标，例如「免费」「需预约」，可留空")
    val badge: String = "",
)

/** 指标卡：statRow 用 */
@Serializable
data class A2uiStat(
    @property:LLMDescription("指标名，例如「步行距离」「人均」")
    val label: String = "",
    @property:LLMDescription("指标值，只填数字或很短的文字")
    val value: String = "",
    @property:LLMDescription("单位，例如 km / 分钟 / 元，可留空")
    val unit: String = "",
)

/** 操作按钮：点击后把 [prompt] 发回聊天（继续对话），而不是跳转客户端页面 */
@Serializable
data class A2uiAction(
    @property:LLMDescription("按钮文字，不超过 8 个字")
    val label: String = "",
    @property:LLMDescription("点击后发回聊天的完整问句，例如「把这条路线改成亲子版，节奏放慢一点」")
    val prompt: String = "",
    /** primary / secondary */
    @property:LLMDescription("按钮样式：primary（第一个按钮用）/ secondary")
    val style: String = "secondary",
)

/** 组件类型常量：渲染器与契约共用一份，避免两处字符串写错对不上 */
object A2uiType {
    const val Header = "header"
    const val Hero = "hero"
    const val Image = "image"
    const val Paragraph = "paragraph"
    const val Bullets = "bullets"
    const val CardList = "cardList"
    const val GridCards = "gridCards"
    const val KeyValue = "keyValue"
    const val Chips = "chips"
    const val Steps = "steps"
    const val Timeline = "timeline"
    const val StatRow = "statRow"
    const val Notice = "notice"
    const val Quote = "quote"
    const val Divider = "divider"
    const val Progress = "progress"
    const val Rating = "rating"
    const val ActionBar = "actionBar"
    const val Spacer = "spacer"
}

/** 宽松解析：忽略未知字段、缺字段走默认值、类型不符直接判失败（调用方自行兜底，不 Crash） */
internal val A2uiJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

internal fun decodeA2uiSurface(json: String): A2uiSurface? =
    runCatching { A2uiJson.decodeFromString<A2uiSurface>(json) }.getOrNull()

/** 落库用：把界面结构序列化成一行 JSON，历史会话重开时再解回来 */
internal fun encodeA2uiSurface(surface: A2uiSurface): String =
    A2uiJson.encodeToString(A2uiSurface.serializer(), surface)
