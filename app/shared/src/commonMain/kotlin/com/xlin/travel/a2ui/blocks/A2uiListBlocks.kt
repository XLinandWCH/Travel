package com.xlin.travel.a2ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.xlin.travel.a2ui.A2uiColor
import com.xlin.travel.a2ui.A2uiGlyph
import com.xlin.travel.a2ui.A2uiPanel
import com.xlin.travel.a2ui.A2uiPill
import com.xlin.travel.a2ui.A2uiSection
import com.xlin.travel.a2ui.A2uiSpace
import com.xlin.travel.a2ui.A2uiText

/**
 * 列表类组件（文字向）：bullets / keyValue / chips / steps。
 *
 * bullets 与 keyValue 都是一张卡里的多行（行间细线分隔），chips 只放关键词，
 * steps 强调先后顺序。卡片一律带描边，不靠阴影把不同模块糊在一起。
 * 卡片/网格类容器见 [A2uiCardBlocks]。
 */

/** 要点列表：一张卡内的多行，行间用细线分隔 */
@Composable
internal fun A2uiBulletsBlock(block: A2uiBlock) {
    A2uiSection(title = block.title, subtitle = block.subtitle) {
        A2uiPanel {
            block.items.forEachIndexed { index, item ->
                if (index > 0) A2uiRowDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(A2uiSpace.md)) {
                    Box(modifier = Modifier.padding(top = 3.dp)) {
                        A2uiGlyph(icon = item.icon.ifBlank { "spot" }, size = 14.dp)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs),
                    ) {
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
                                color = A2uiColor.textMeta,
                                fontSize = A2uiText.label,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 键值对表：门票、时长、地址这类成对信息，左标签右取值，行间细线 */
@Composable
internal fun A2uiKeyValueBlock(block: A2uiBlock) {
    A2uiSection(title = block.title, subtitle = block.subtitle) {
        A2uiPanel {
            block.items.forEachIndexed { index, item ->
                if (index > 0) A2uiRowDivider()
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
}

/** 标签组：免费 / 室内 / 夜景这类关键词，自动折行；中性胶囊，不做满屏橙色标签 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun A2uiChipsBlock(block: A2uiBlock) {
    A2uiSection(title = block.title, subtitle = block.subtitle) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
            verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
        ) {
            block.items.forEach { item -> A2uiPill(item.title.ifBlank { item.text }) }
        }
    }
}

/** 步骤条：按顺序做的一二三四，序号用中性描边圆，不用实心强调色 */
@Composable
internal fun A2uiStepsBlock(block: A2uiBlock) {
    A2uiSection(title = block.title, subtitle = block.subtitle) {
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.md)) {
            block.items.forEachIndexed { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(A2uiSpace.md)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(A2uiColor.surfaceSunken)
                            .border(1.dp, A2uiColor.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = A2uiColor.textPrimary,
                            fontSize = A2uiText.label,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs),
                    ) {
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
}

/** 卡内行分隔线：1dp、左右不出卡，视觉上只切分不断开 */
@Composable
private fun A2uiRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(A2uiColor.divider),
    )
}
