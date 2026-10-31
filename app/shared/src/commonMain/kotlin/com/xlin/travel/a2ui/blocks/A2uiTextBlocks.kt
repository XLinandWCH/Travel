package com.xlin.travel.a2ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xlin.travel.a2ui.A2uiBlock
import com.xlin.travel.a2ui.A2uiColor
import com.xlin.travel.a2ui.A2uiGlyph
import com.xlin.travel.a2ui.A2uiRadius
import com.xlin.travel.a2ui.A2uiSpace
import com.xlin.travel.a2ui.A2uiText

/**
 * 文字类组件：header / paragraph / notice / quote / divider / spacer。
 *
 * 这六个块只承载文字，不做列表——列表交给 [A2uiListBlocks]，
 * 这样每个组的间距节奏都能单独调，AI 混搭时也不会出现「标题挤在卡片里」。
 */

/** 页头：大标题 + 强调色副标题 + 一句说明 */
@Composable
internal fun A2uiHeaderBlock(block: A2uiBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xs)) {
        if (block.title.isNotBlank()) {
            Text(
                text = block.title,
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.display,
                lineHeight = A2uiText.displayLine,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (block.subtitle.isNotBlank()) {
            Text(
                text = block.subtitle,
                color = A2uiColor.accent,
                fontSize = A2uiText.caption,
                lineHeight = A2uiText.captionLine,
                fontWeight = FontWeight.Medium,
            )
        }
        if (block.text.isNotBlank()) {
            Text(
                text = block.text,
                color = A2uiColor.textSecondary,
                fontSize = A2uiText.body,
                lineHeight = A2uiText.bodyLine,
            )
        }
    }
}

/** 普通段落：可选小标题 + 一段正文 */
@Composable
internal fun A2uiParagraphBlock(block: A2uiBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xs)) {
        if (block.title.isNotBlank()) {
            Text(
                text = block.title,
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.title,
                lineHeight = A2uiText.titleLine,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (block.text.isNotBlank()) {
            Text(
                text = block.text,
                color = A2uiColor.textSecondary,
                fontSize = A2uiText.body,
                lineHeight = A2uiText.bodyLine,
            )
        }
    }
}

/** 提示条：级别决定强调色（info 蓝 / warn 橙 / danger 红），左侧图标 + 标题 + 说明 */
@Composable
internal fun A2uiNoticeBlock(block: A2uiBlock) {
    val tone = A2uiColor.levelColor(block.level)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(A2uiRadius.block))
            .background(tone.copy(alpha = 0.10f))
            .padding(horizontal = A2uiSpace.md, vertical = A2uiSpace.sm),
        horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
    ) {
        A2uiGlyph(icon = block.icon.ifBlank { "avoid" }, tint = tone, size = 16.dp)
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs)) {
            if (block.title.isNotBlank()) {
                Text(
                    text = block.title,
                    color = tone,
                    fontSize = A2uiText.body,
                    lineHeight = A2uiText.bodyLine,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (block.text.isNotBlank()) {
                Text(
                    text = block.text,
                    color = A2uiColor.textSecondary,
                    fontSize = A2uiText.caption,
                    lineHeight = A2uiText.captionLine,
                )
            }
        }
    }
}

/** 引用块：左侧竖线 + 讲解词，出处放右下角 */
@Composable
internal fun A2uiQuoteBlock(block: A2uiBlock) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(A2uiSpace.md),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(A2uiRadius.chip))
                .background(A2uiColor.accent.copy(alpha = 0.5f)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xs)) {
            Text(
                text = block.text,
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.body,
                lineHeight = A2uiText.bodyLine,
            )
            val source = block.subtitle.ifBlank { block.title }
            if (source.isNotBlank()) {
                Text(
                    text = "—— $source",
                    color = A2uiColor.textTertiary,
                    fontSize = A2uiText.caption,
                )
            }
        }
    }
}

/** 细分割线：比块之间的留白更轻的视觉切分 */
@Composable
internal fun A2uiDividerBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(A2uiColor.divider),
    )
}

/** 留白：模型用 value 指定像素高度，统一夹在 8–48 之间，防止出现半屏空隙 */
@Composable
internal fun A2uiSpacerBlock(block: A2uiBlock) {
    Spacer(Modifier.height(block.value.coerceIn(8f, 48f).dp))
}
