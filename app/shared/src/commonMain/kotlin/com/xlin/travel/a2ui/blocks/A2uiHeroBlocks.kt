package com.xlin.travel.a2ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xlin.travel.a2ui.A2uiBlock
import com.xlin.travel.a2ui.A2uiColor
import com.xlin.travel.a2ui.A2uiIconCircle
import com.xlin.travel.a2ui.A2uiPill
import com.xlin.travel.a2ui.A2uiRadius
import com.xlin.travel.a2ui.A2uiSpace
import com.xlin.travel.a2ui.A2uiText

/**
 * 主视觉与操作区：hero / actionBar。
 *
 * hero 一屏只允许一个（契约第 1 条），放在最上面承担「这是关于什么的界面」；
 * actionBar 必须是最后一块（契约第 3 条），点按钮不是跳页面，而是把 prompt 发回聊天继续问。
 */

/** 主视觉卡：图标 + 大标题 + 副标题 + 一句概述 + 关键词胶囊 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun A2uiHeroBlock(block: A2uiBlock) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(A2uiRadius.card))
            .background(
                Brush.linearGradient(
                    listOf(A2uiColor.accent.copy(alpha = 0.22f), A2uiColor.surface),
                ),
            )
            .padding(A2uiSpace.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(A2uiSpace.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                A2uiIconCircle(icon = block.icon.ifBlank { "route" }, size = 44.dp)
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
                            color = A2uiColor.accent,
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
}

/** 操作条：2–3 个按钮，点击把 prompt 原样发回聊天（模型据此继续对话） */
@Composable
internal fun A2uiActionBarBlock(block: A2uiBlock, onAction: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
        block.actions.forEach { action ->
            val primary = action.style.equals("primary", ignoreCase = true)
            val shape = RoundedCornerShape(A2uiRadius.inner)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (primary) A2uiColor.accent else A2uiColor.surface)
                    .then(if (primary) Modifier else Modifier.border(1.dp, A2uiColor.divider, shape))
                    .clickable { onAction(action.prompt.ifBlank { action.label }) }
                    .padding(vertical = A2uiSpace.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = action.label,
                    color = if (primary) A2uiColor.onAccent else A2uiColor.textPrimary,
                    fontSize = A2uiText.body,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
