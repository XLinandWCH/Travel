package com.xlin.travel.a2ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.xlin.travel.a2ui.A2uiGlyph
import com.xlin.travel.a2ui.A2uiRadius
import com.xlin.travel.a2ui.A2uiSection
import com.xlin.travel.a2ui.A2uiSpace
import com.xlin.travel.a2ui.A2uiText

/**
 * 文字类组件：header / paragraph / notice / quote / divider / spacer。
 *
 * 这六个块只承载文字，不做列表——列表交给 [A2uiListBlocks]，数据交给 [A2uiDataBlocks]，
 * 这样每个组的间距节奏都能单独调，AI 混搭时也不会出现「标题挤在卡片里」。
 */

/** 页头：大标题 + 中性副标题 + 一句说明；全屏唯一一处橙色短竖条 */
@Composable
internal fun A2uiHeaderBlock(block: A2uiBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(end = A2uiSpace.sm)
                    .size(width = 3.dp, height = 20.dp)
                    .clip(RoundedCornerShape(A2uiRadius.chip))
                    .background(A2uiColor.accent),
            )
            Text(
                text = block.title,
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.headline,
                lineHeight = A2uiText.headlineLine,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
        if (block.subtitle.isNotBlank()) {
            Text(
                text = block.subtitle,
                color = A2uiColor.textMeta,
                fontSize = A2uiText.caption,
                lineHeight = A2uiText.captionLine,
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

/** 普通段落：模块标题 + 一段正文，不做卡片底（和上面的列表卡形成轻重对比） */
@Composable
internal fun A2uiParagraphBlock(block: A2uiBlock) {
    A2uiSection(title = block.title, subtitle = block.subtitle) {
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

/**
 * 提示条：只有 warn / danger 用语义色（底 10% + 描边 25%），info 走中性底。
 * 一屏最多一条——每条都带彩色的话，转眼又是一屏强调色。
 */
@Composable
internal fun A2uiNoticeBlock(block: A2uiBlock) {
    val tinted = A2uiColor.levelIsTinted(block.level)
    val tone = if (tinted) A2uiColor.levelColor(block.level) else A2uiColor.textSecondary
    val shape = RoundedCornerShape(A2uiRadius.block)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (tinted) tone.copy(alpha = 0.10f) else A2uiColor.surfaceSunken)
            .border(1.dp, if (tinted) tone.copy(alpha = 0.28f) else A2uiColor.outline, shape)
            .padding(horizontal = A2uiSpace.md, vertical = A2uiSpace.md),
        horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
        verticalAlignment = Alignment.Top,
    ) {
        A2uiGlyph(icon = block.icon.ifBlank { "avoid" }, tint = tone, size = 16.dp)
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs)) {
            if (block.title.isNotBlank()) {
                Text(
                    text = block.title,
                    color = if (tinted) tone else A2uiColor.textPrimary,
                    fontSize = A2uiText.body,
                    lineHeight = A2uiText.bodyLine,
                    fontWeight = FontWeight.Medium,
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

/** 引用块：左侧中性竖线 + 讲解词，出处放右下角 */
@Composable
internal fun A2uiQuoteBlock(block: A2uiBlock) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(A2uiSpace.md),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(A2uiRadius.chip))
                .background(A2uiColor.divider),
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
                    color = A2uiColor.textMeta,
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
