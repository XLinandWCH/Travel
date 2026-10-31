package com.xlin.travel.a2ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.xlin.travel.a2ui.A2uiPanel
import com.xlin.travel.a2ui.A2uiRadius
import com.xlin.travel.a2ui.A2uiSection
import com.xlin.travel.a2ui.A2uiSpace
import com.xlin.travel.a2ui.A2uiText

/**
 * 数据类组件：statRow / progress / rating / timeline。
 *
 * 强调色只留给「进度填充」和「评分星」这两处小面积；数字、时间、标签全走中性色。
 * 时间线的时间列尤其不能用强调色——一整列橙色看着像告警，读起来也累。
 */

/** 指标横排：2–3 个数字并列，值大标签小，数字用正文色而不是橙色 */
@Composable
internal fun A2uiStatRowBlock(block: A2uiBlock) {
    A2uiSection(title = block.title, subtitle = block.subtitle) {
        A2uiPanel {
            Row(horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
                block.stats.forEach { stat ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs),
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = stat.value,
                                color = A2uiColor.textPrimary,
                                fontSize = A2uiText.headline,
                                lineHeight = A2uiText.headlineLine,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (stat.unit.isNotBlank()) {
                                Text(
                                    text = stat.unit,
                                    color = A2uiColor.textMeta,
                                    fontSize = A2uiText.label,
                                    modifier = Modifier.padding(start = A2uiSpace.xxs, bottom = 3.dp),
                                )
                            }
                        }
                        Text(
                            text = stat.label,
                            color = A2uiColor.textTertiary,
                            fontSize = A2uiText.label,
                        )
                    }
                }
            }
        }
    }
}

/** 进度条：拥挤度、完成度；右侧显示当前值 / 上限，填充条用强调色（小面积） */
@Composable
internal fun A2uiProgressBlock(block: A2uiBlock) {
    val max = if (block.max > 0f) block.max else 100f
    val fraction = (block.value / max).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = block.title,
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.caption,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = block.subtitle.ifBlank { "${block.value.toInt()} / ${block.max.toInt()}" },
                color = A2uiColor.textMeta,
                fontSize = A2uiText.label,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(A2uiRadius.chip))
                .background(A2uiColor.divider),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(A2uiRadius.chip))
                    .background(A2uiColor.accent),
            )
        }
    }
}

/** 评分：实心 / 空心五角星 + 一行说明（评分上限按 max 取，缺省 5） */
@Composable
internal fun A2uiRatingBlock(block: A2uiBlock) {
    val max = block.max.toInt().coerceIn(1, 5)
    val filled = block.value.toInt().coerceIn(0, max)
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xs)) {
        Text(
            text = block.title,
            color = A2uiColor.textPrimary,
            fontSize = A2uiText.caption,
            fontWeight = FontWeight.Medium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "★".repeat(filled),
                color = A2uiColor.accent,
                fontSize = A2uiText.title,
            )
            Text(
                text = "★".repeat(max - filled),
                color = A2uiColor.divider,
                fontSize = A2uiText.title,
            )
            if (block.text.isNotBlank()) {
                Text(
                    text = block.text,
                    color = A2uiColor.textMeta,
                    fontSize = A2uiText.caption,
                    modifier = Modifier.padding(start = A2uiSpace.sm),
                )
            }
        }
    }
}

/**
 * 时间线：左侧时间 + 圆点 + 事件，用于带时刻的行程与演出场次。
 * 时间走中性灰；只有带 badge 的「当前项」圆点是强调色，一屏最多一个焦点。
 */
@Composable
internal fun A2uiTimelineBlock(block: A2uiBlock) {
    A2uiSection(title = block.title, subtitle = block.subtitle) {
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.md)) {
            block.items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
                    Text(
                        text = item.meta,
                        color = A2uiColor.textMeta,
                        fontSize = A2uiText.label,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(56.dp).padding(top = 2.dp),
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.badge.isNotBlank()) A2uiColor.accent else A2uiColor.textTertiary,
                            ),
                    )
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
