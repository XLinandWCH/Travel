package com.xlin.travel.chat_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ActionIconColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.copy
import travel.app.shared.generated.resources.dislike
import travel.app.shared.generated.resources.like
import travel.app.shared.generated.resources.regenerate

/**
 * AI 正文下方的动作按钮条：赞同、不赞同、重新生成、复制。
 * 从左往右排列，按钮之间间隔 [Gap]，图标颜色随主题（深色白 / 浅色黑）。
 */
@Composable
fun MessageActionRow(
    message: ChatMessage,
    canRegenerate: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onRegenerate: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Gap),
    ) {
        ActionButton(
            icon = Res.drawable.like,
            description = "赞同",
            tint = feedbackTint(message.feedback, Feedback.Like),
            onClick = onLike,
        )
        ActionButton(
            icon = Res.drawable.dislike,
            description = "不赞同",
            tint = feedbackTint(message.feedback, Feedback.Dislike),
            onClick = onDislike,
        )
        ActionButton(
            icon = Res.drawable.regenerate,
            description = "重新生成",
            tint = ActionIconColor,
            enabled = canRegenerate,
            onClick = onRegenerate,
        )
        ActionButton(
            icon = Res.drawable.copy,
            description = "复制正文",
            tint = ActionIconColor,
            onClick = onCopy,
        )
    }
}

/** 已选中的反馈项用主强调色高亮，其余按主题取白 / 黑 */
@Composable
private fun feedbackTint(current: Feedback?, expected: Feedback): Color =
    if (current == expected) AccentOrange else ActionIconColor

@Composable
private fun ActionButton(
    icon: DrawableResource,
    description: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(ButtonSize)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(IconSize),
        )
    }
}

private val Gap = 3.dp
private val ButtonSize = 30.dp
private val IconSize = 17.dp
