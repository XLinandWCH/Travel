package com.xlin.travel.a2ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xlin.travel.a2ui.A2uiAction
import com.xlin.travel.a2ui.A2uiBlock
import com.xlin.travel.a2ui.A2uiColor
import com.xlin.travel.a2ui.A2uiIconCircle
import com.xlin.travel.a2ui.A2uiPanel
import com.xlin.travel.a2ui.A2uiPill
import com.xlin.travel.a2ui.A2uiRadius
import com.xlin.travel.a2ui.A2uiSpace
import com.xlin.travel.a2ui.A2uiText

/**
 * 主视觉与操作区：hero / actionBar。
 *
 * hero 一屏只允许一个（契约第 1 条），放在最上面承担「这是关于什么的界面」；
 * actionBar 固定吸附在页面底部（见 [com.xlin.travel.a2ui.A2uiSurfaceScreen]），
 * 点按钮不是跳页面，而是把 prompt 发回聊天继续问。
 */

/** 主视觉卡：图标 + 大标题 + 副标题 + 一句概述 + 关键词胶囊；中性底 + 描边，不做橙色渐变 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun A2uiHeroBlock(block: A2uiBlock) {
    A2uiPanel {
        Row(
            horizontalArrangement = Arrangement.spacedBy(A2uiSpace.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            A2uiIconCircle(icon = block.icon.ifBlank { "spot" }, size = 44.dp)
            Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs)) {
                Text(
                    text = block.title,
                    color = A2uiColor.textPrimary,
                    fontSize = A2uiText.headline,
                    lineHeight = A2uiText.headlineLine,
                    fontWeight = FontWeight.SemiBold,
                )
                if (block.subtitle.isNotBlank()) {
                    Text(
                        text = block.subtitle,
                        color = A2uiColor.textMeta,
                        fontSize = A2uiText.caption,
                        lineHeight = A2uiText.captionLine,
                    )
                }
            }
        }
        if (block.text.isNotBlank()) {
            Text(
                text = block.text,
                color = A2uiColor.textSecondary,
                fontSize = A2uiText.body,
                lineHeight = A2uiText.bodyLine,
            )
        }
        val tags = block.items.map { it.title.ifBlank { it.text } }.filter { it.isNotBlank() }
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
                verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
            ) {
                tags.take(4).forEach { A2uiPill(it) }
            }
        }
    }
}

/**
 * 操作条：横排 2–3 个按钮。
 * 只有 primary 用实心强调色（全屏唯一的大面积橙色），其余是表面色 + 描边。
 */
@Composable
internal fun A2uiActionBarBlock(block: A2uiBlock, onAction: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
        block.actions.take(3).forEach { action ->
            A2uiActionButton(
                action = action,
                onClick = { onAction(action.prompt.ifBlank { action.label }) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun A2uiActionButton(
    action: A2uiAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = action.style.equals("primary", ignoreCase = true)
    val shape = RoundedCornerShape(A2uiRadius.inner)
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (primary) A2uiColor.accent else A2uiColor.surface)
            .then(if (primary) Modifier else Modifier.border(1.dp, A2uiColor.outline, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = A2uiSpace.md, vertical = A2uiSpace.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = action.label,
            color = if (primary) A2uiColor.onAccent else A2uiColor.textPrimary,
            fontSize = A2uiText.caption,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
