package com.xlin.travel.a2ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xlin.travel.a2ui.A2uiBlock
import com.xlin.travel.a2ui.A2uiColor
import com.xlin.travel.a2ui.A2uiIconCircle
import com.xlin.travel.a2ui.A2uiItem
import com.xlin.travel.a2ui.A2uiPanel
import com.xlin.travel.a2ui.A2uiPill
import com.xlin.travel.a2ui.A2uiRadius
import com.xlin.travel.a2ui.A2uiSection
import com.xlin.travel.a2ui.A2uiSpace
import com.xlin.travel.a2ui.A2uiText

/**
 * 卡片类组件：cardList / gridCards。
 *
 * 两者的差别只在密度：cardList 一个条目一张卡（点位、店铺，信息多），
 * gridCards 双列铺开（4–6 个短条目，信息少）。都用「表面色 + 1dp 描边」，
 * 和页面背景拉开一层，用户一眼能看出这是「卡片」而不是一段文字。
 */

/** 卡片列表：每个点位 / 店铺一张描边卡，独立成块，边界最清楚 */
@Composable
internal fun A2uiCardListBlock(block: A2uiBlock) {
    A2uiSection(title = block.title, subtitle = block.subtitle) {
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
            block.items.forEach { item -> A2uiCardItem(item) }
        }
    }
}

@Composable
private fun A2uiCardItem(item: A2uiItem) {
    A2uiPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(A2uiSpace.md)) {
            A2uiIconCircle(icon = item.icon.ifBlank { "spot" })
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = A2uiColor.textPrimary,
                        fontSize = A2uiText.title,
                        lineHeight = A2uiText.titleLine,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.badge.isNotBlank()) A2uiPill(item.badge)
                }
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        color = A2uiColor.textTertiary,
                        fontSize = A2uiText.label,
                    )
                }
                if (item.text.isNotBlank()) {
                    Text(
                        text = item.text,
                        color = A2uiColor.textSecondary,
                        fontSize = A2uiText.caption,
                        lineHeight = A2uiText.captionLine,
                    )
                }
                if (item.meta.isNotBlank()) {
                    Text(
                        text = item.meta,
                        color = A2uiColor.textMeta,
                        fontSize = A2uiText.label,
                    )
                }
            }
        }
    }
}

/** 双列小卡：每个条目一张卡，两列铺开避免一列拉到天际 */
@Composable
internal fun A2uiGridCardsBlock(block: A2uiBlock) {
    A2uiSection(title = block.title, subtitle = block.subtitle) {
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
            block.items.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
                    row.forEach { item ->
                        A2uiGridCell(item = item, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun A2uiGridCell(item: A2uiItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(A2uiRadius.block))
            .background(A2uiColor.surface)
            .border(1.dp, A2uiColor.outline, RoundedCornerShape(A2uiRadius.block))
            .padding(horizontal = A2uiSpace.md, vertical = A2uiSpace.md),
        verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
    ) {
        A2uiIconCircle(icon = item.icon.ifBlank { "spot" }, size = 28.dp)
        Text(
            text = item.title,
            color = A2uiColor.textPrimary,
            fontSize = A2uiText.body,
            lineHeight = A2uiText.bodyLine,
            fontWeight = FontWeight.Medium,
        )
        val meta = item.meta.ifBlank { item.subtitle }
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                color = A2uiColor.textMeta,
                fontSize = A2uiText.label,
            )
        }
    }
}
