package com.xlin.travel.a2ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 「进入个性化界面」入口条。
 *
 * 它只出现在「这一轮 AI 真的调了 a2ui_render」的消息下面（见 [com.xlin.travel.chat_screen.AssistantMessageBlock]），
 * 所以用户看到它就知道「这次回答除了文字，还有一屏可以点开的界面」。
 * 点击后进入 [A2uiSurfaceScreen]；不放「展开 / 收起」，避免聊天页里塞一屏长内容。
 */
@Composable
fun A2uiEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(A2uiRadius.block))
            .background(A2uiColor.accent.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = A2uiSpace.md, vertical = A2uiSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
    ) {
        A2uiGlyph(icon = "spot", size = 14.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs),
        ) {
            Text(
                text = "进入个性化界面",
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.caption,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = title.takeIf { it.isNotBlank() }?.let { "AI 已排好「$it」" } ?: "AI 为这次回答排好了一屏界面",
                color = A2uiColor.textTertiary,
                fontSize = A2uiText.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "›",
            color = A2uiColor.accent,
            fontSize = A2uiText.title,
            fontWeight = FontWeight.Medium,
        )
    }
}
