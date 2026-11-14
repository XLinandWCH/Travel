package com.xlin.travel.a2ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

/**
 * A2UI 的渲染基元：图标、图标胶囊、卡片底、模块标题、胶囊标签。
 *
 * 所有 block 组件都只用这几个基元拼装，所以「AI 随便换组件」也不会破坏视觉一致性——
 * 间距来自 [A2uiSpace]，圆角来自 [A2uiRadius]，颜色来自 [A2uiColor]。
 *
 * 三条硬规则（针对「一打开满屏强调色、文字挤成一坨」）：
 * 1. 图标默认走中性灰，只有调用方显式传 [A2uiColor.accent] 才出橙色；
 * 2. 卡片一律「表面色 + 1dp 描边」，边界靠线而不是靠阴影，深浅主题都清楚；
 * 3. 模块标题下压一条细分割线，让「大模块 / 小块」的从属关系一眼可见。
 */

/** 语义图标：模型写的是 route / food 这类名字；识别不了就画一个中性色圆点，绝不出现破图 */
@Composable
internal fun A2uiGlyph(
    icon: String,
    modifier: Modifier = Modifier,
    tint: Color = A2uiColor.textSecondary,
    size: Dp = 16.dp,
) {
    val resource = A2uiIcons.painterFor(icon)
    if (resource == null) {
        Box(modifier = modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.35f)))
    } else {
        Image(
            painter = painterResource(resource),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = modifier.size(size),
        )
    }
}

/** 图标胶囊：列表条目左侧的圆形图标底（沉一档的表面色，不做彩色底） */
@Composable
internal fun A2uiIconCircle(
    icon: String,
    modifier: Modifier = Modifier,
    tint: Color = A2uiColor.textSecondary,
    size: Dp = 34.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(A2uiColor.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {
        A2uiGlyph(icon = icon, tint = tint, size = size / 2)
    }
}

/** 卡片底：所有需要「一张卡」的组件共用，带描边保证边界清楚 */
@Composable
internal fun A2uiPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(A2uiRadius.card))
            .background(A2uiColor.surface)
            .border(1.dp, A2uiColor.outline, RoundedCornerShape(A2uiRadius.card))
            .padding(horizontal = A2uiSpace.lg, vertical = A2uiSpace.md),
        verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
        content = content,
    )
}

/**
 * 模块标题：标题 + 右侧补充信息 + 一条细线。
 *
 * 一屏由 3–6 个模块组成，模块之间靠 [A2uiSpace.module] 拉开、靠这条线收口；
 * 竖条与文字都走中性色，强调色留给页面标题那唯一一处。
 */
@Composable
internal fun A2uiSectionHeader(
    title: String,
    subtitle: String = "",
    trailing: String = "",
    accentMark: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (accentMark) {
                Box(
                    modifier = Modifier
                        .padding(end = A2uiSpace.sm)
                        .size(width = 3.dp, height = 16.dp)
                        .clip(RoundedCornerShape(A2uiRadius.chip))
                        .background(A2uiColor.accent),
                )
            }
            Text(
                text = title,
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.title,
                lineHeight = A2uiText.titleLine,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (trailing.isNotBlank()) {
                Text(
                    text = trailing,
                    color = A2uiColor.textMeta,
                    fontSize = A2uiText.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = A2uiColor.textTertiary,
                fontSize = A2uiText.label,
                lineHeight = A2uiText.captionLine,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = A2uiSpace.xxs)
                .height(1.dp)
                .background(A2uiColor.divider),
        )
    }
}

/** 模块容器：有标题就画模块头，没有标题只排内容，块与块之间统一留 [A2uiSpace.md] */
@Composable
internal fun A2uiSection(
    title: String,
    subtitle: String = "",
    trailing: String = "",
    accentMark: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.md)) {
        if (title.isNotBlank()) {
            A2uiSectionHeader(title = title, subtitle = subtitle, trailing = trailing, accentMark = accentMark)
        }
        content()
    }
}

/** 胶囊标签：chips、hero 关键词、条目徽标共用；默认中性，只有调用方指定 tone 才染色 */
@Composable
internal fun A2uiPill(text: String, tone: Color = A2uiColor.textSecondary) {
    Text(
        text = text,
        color = tone,
        fontSize = A2uiText.label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(A2uiRadius.chip))
            .background(A2uiColor.surfaceSunken)
            .padding(horizontal = A2uiSpace.sm, vertical = A2uiSpace.xs),
    )
}
