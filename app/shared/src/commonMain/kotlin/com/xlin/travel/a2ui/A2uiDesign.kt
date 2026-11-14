package com.xlin.travel.a2ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.MiniPanelDivider
import com.xlin.travel.color.RouteChipSelectedBackground
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextGray400
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.color.themeColor

/**
 * A2UI 的 Design Tokens（排版契约的落点）。
 *
 * 为什么单独一份：AI 只负责「放哪个组件、放几号内容」，间距与字号必须由客户端锁定，
 * 否则每个组件各写一套数，AI 排出来的界面就会忽大忽小。这里把四件事收口：
 * 1. 间距只有 [A2uiSpace] 里的 7 档，组件内不再出现魔法数字；
 * 2. 字号只有 [A2uiText] 里的语义层级（display → label），对应正文 / 标题 / 说明；
 * 3. 颜色全部取自 AppColors 的语义色，深浅主题自动跟随，不新增纯白色相；
 * 4. 强调色配额：一屏只有「一个主按钮 + 1–2 处关键数值 / 进度」用 [A2uiColor.accent]，
 *    时间、徽标、列表图标、块标题一律走中性色——一打开满屏橙色是最容易做坏的界面。
 */

/** 间距刻度：4 的倍数，只有这 7 档 */
object A2uiSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** 模块之间的呼吸：比块内间距大一档，让「这一块和那一块」看得出分界 */
    val module = 22.dp
}

/** 圆角刻度：卡片 / 内部块 / 胶囊 */
object A2uiRadius {
    val card = 20.dp
    val block = 14.dp
    val inner = 10.dp
    val chip = 999.dp
}

/** 字号层级：一屏最多用到 display / headline / title / body / caption / label 六级 */
object A2uiText {
    val display = 24.sp
    val headline = 20.sp
    val title = 16.sp
    val body = 14.sp
    val caption = 12.sp
    val label = 11.sp
    val displayLine = 32.sp
    val headlineLine = 27.sp
    val titleLine = 22.sp
    val bodyLine = 21.sp
    val captionLine = 17.sp
}

/** 语义色：全部是 getter，主题切换后自动重组；warn / danger 为两主题共用的固定语义色 */
object A2uiColor {
    val page: Color get() = ChatBackground
    val surface: Color get() = SettingCardBackground

    /** 次级容器底：图标圆、胶囊、图片占位，比卡片底再沉一档，用来做「块中块」的层次 */
    val surfaceSunken: Color get() = themeColor(Color(0xFF171717), Color(0xFFF0F0F3))

    /** 卡片描边：所有 card 必须能看出边界（不是靠阴影，深浅主题都能看清） */
    val outline: Color get() = themeColor(Color(0x1FFFFFFF), Color(0x1A1A1A1A))

    val textPrimary: Color get() = TextWhite
    val textSecondary: Color get() = TextGray300
    val textTertiary: Color get() = TextGray500

    /** 时间 / 距离 / 价格等元信息：中性灰，绝不整列刷强调色 */
    val textMeta: Color get() = TextGray400

    val accent: Color get() = AccentOrange
    val accentSoft: Color get() = RouteChipSelectedBackground
    val divider: Color get() = MiniPanelDivider
    val info: Color get() = themeColor(Color(0xFF9CA3AF), Color(0xFF6B7280))
    val warn = Color(0xFFFFB74D)
    val danger = Color(0xFFE5544B)
    val onAccent = Color(0xFF141418)

    /** notice 级别 → 语义色；info 走中性，只有 warn / danger 才允许出彩色 */
    fun levelColor(level: String): Color = when (level.lowercase()) {
        "warn", "warning" -> warn
        "danger", "error", "alert" -> danger
        else -> info
    }

    /** 该级别是否需要用语义色（info 用中性底，避免每条提示都是彩色条） */
    fun levelIsTinted(level: String): Boolean =
        level.lowercase() in setOf("warn", "warning", "danger", "error", "alert")
}
