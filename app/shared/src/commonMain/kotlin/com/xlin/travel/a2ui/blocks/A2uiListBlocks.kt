package com.xlin.travel.a2ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xlin.travel.a2ui.A2uiBlock
import com.xlin.travel.a2ui.A2uiBlockHeading
import com.xlin.travel.a2ui.A2uiColor
import com.xlin.travel.a2ui.A2uiGlyph
import com.xlin.travel.a2ui.A2uiIconCircle
import com.xlin.travel.a2ui.A2uiItem
import com.xlin.travel.a2ui.A2uiPanel
import com.xlin.travel.a2ui.A2uiPill
import com.xlin.travel.a2ui.A2uiRadius
import com.xlin.travel.a2ui.A2uiSpace
import com.xlin.travel.a2ui.A2uiText

/**
 * 列表类组件：bullets / cardList / keyValue / chips / steps。
 *
 * 五者的差别只在「信息密度」和「有没有卡片底」：
 * bullets 最轻（纯要点）、cardList 最重（一items一卡）、chips 只放关键词。
 * 模型选错也不致命：内容少时 cardList 也读得下去，内容多时 bullets 会自动折行。
 */

/** 要点列表：图标（或圆点）+ 标题 + 一句话解释 */
@Composable
internal fun A2uiBulletsBlock(block: A2uiBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.md)) {
        if (block.title.isNotBlank()) A2uiBlockHeading(block.title, block.subtitle)
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
            block.items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
                    Box(modifier = Modifier.padding(top = 3.dp)) {
                        A2uiGlyph(icon = item.icon, size = 14.dp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs)) {
                        Text(
                            text = item.title,
                            color = A2uiColor.textPrimary,
                            fontSize = A2uiText.body,
                            lineHeight = A2uiText.bodyLine,
                            fontWeight = FontWeight.Medium,
                        )
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
                                color = A2uiColor.textTertiary,
                                fontSize = A2uiText.label,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 卡片列表：每个点位 / 店铺一张卡，适合「景点推荐」「美食攻略」 */
@Composable
internal fun A2uiCardListBlock(block: A2uiBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.md)) {
        if (block.title.isNotBlank()) A2uiBlockHeading(block.title, block.subtitle)
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
            block.items.forEach { item -> A2uiCardItem(item) }
        }
    }
}

@Composable
private fun A2uiCardItem(item: A2uiItem) {
    A2uiPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
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
                        color = A2uiColor.accent,
                        fontSize = A2uiText.label,
                    )
                }
            }
        }
    }
}

/** 键值表：门票、时长、地址这类成对信息，左标签右取值 */
@Composable
internal fun A2uiKeyValueBlock(block: A2uiBlock) {
    A2uiPanel {
        if (block.title.isNotBlank()) A2uiBlockHeading(block.title, block.subtitle)
        block.items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    text = item.title,
                    color = A2uiColor.textTertiary,
                    fontSize = A2uiText.caption,
                    modifier = Modifier.width(84.dp),
                )
                Text(
                    text = item.text.ifBlank { item.meta },
                    color = A2uiColor.textPrimary,
                    fontSize = A2uiText.caption,
                    lineHeight = A2uiText.captionLine,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 标签组：免费 / 室内 / 夜景这类关键词，自动折行 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun A2uiChipsBlock(block: A2uiBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
        if (block.title.isNotBlank()) A2uiBlockHeading(block.title, block.subtitle)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
            verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
        ) {
            block.items.forEach { item -> A2uiPill(item.title.ifBlank { item.text }) }
        }
    }
}

/** 步骤条：按顺序做的一二三四，序号用强调色圆形数字 */
@Composable
internal fun A2uiStepsBlock(block: A2uiBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.md)) {
        if (block.title.isNotBlank()) A2uiBlockHeading(block.title, block.subtitle)
        block.items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(A2uiColor.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        color = A2uiColor.onAccent,
                        fontSize = A2uiText.label,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs)) {
                    Text(
                        text = item.title,
                        color = A2uiColor.textPrimary,
                        fontSize = A2uiText.body,
                        lineHeight = A2uiText.bodyLine,
                        fontWeight = FontWeight.Medium,
                    )
                    if (item.text.isNotBlank()) {
                        Text(
                            text = item.text,
                            color = A2uiColor.textSecondary,
                            fontSize = A2uiText.caption,
                            lineHeight = A2uiText.captionLine,
                        )
                    }
                }
            }
        }
    }
}
