package com.xlin.travel.a2ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
 * A2UI 的四个渲染基元：图标、图标胶囊、卡片底、块标题。
 *
 * 所有 block 组件都只用这四个基元拼装，所以「AI 随便换组件」也不会破坏视觉一致性——
 * 间距来自 [A2uiSpace]，圆角来自 [A2uiRadius]，颜色来自 [A2uiColor]。
 */

/** 语义图标：模型写的是 route / food 这类名字；识别不了就画一个强调色圆点，绝不出现破图 */
@Composable
internal fun A2uiGlyph(
    icon: String,
    modifier: Modifier = Modifier,
    tint: Color = A2uiColor.accent,
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

/** 图标胶囊：列表条目左侧的圆形图标底（比纯图标多一层柔和的强调色） */
@Composable
internal fun A2uiIconCircle(
    icon: String,
    modifier: Modifier = Modifier,
    tint: Color = A2uiColor.accent,
    size: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        A2uiGlyph(icon = icon, tint = tint, size = size / 2)
    }
}

/** 卡片底：所有需要「一张卡」的组件共用，内边距固定，避免各组件各写一套 */
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
            .padding(horizontal = A2uiSpace.lg, vertical = A2uiSpace.md),
        verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
        content = content,
    )
}

/** 块标题：左侧一条 3dp 强调色竖条 + 标题，是「一屏多块」时的视觉分隔 */
@Composable
internal fun A2uiBlockHeading(title: String, subtitle: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 18.dp)
                .clip(RoundedCornerShape(A2uiRadius.chip))
                .background(A2uiColor.accent),
        )
        Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs)) {
            Text(
                text = title,
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.title,
                lineHeight = A2uiText.titleLine,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = A2uiColor.textTertiary,
                    fontSize = A2uiText.label,
                )
            }
        }
    }
}

/** 胶囊标签：chips 组件与 hero 上的关键词共用 */
@Composable
internal fun A2uiPill(text: String, tone: Color = A2uiColor.accent) {
    Text(
        text = text,
        color = tone,
        fontSize = A2uiText.label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(A2uiRadius.chip))
            .background(tone.copy(alpha = 0.14f))
            .padding(horizontal = A2uiSpace.sm, vertical = A2uiSpace.xs),
    )
}
