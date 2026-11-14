package com.xlin.travel.a2ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.chevron_right

/**
 * 「进入个性化界面」入口条。
 *
 * 出现在「这一轮 AI 真的在调 a2ui_render」的回复里，位置固定在正文之后、动作按钮之前——
 * 先让用户看到「还有一屏可以点开」，再是点赞 / 不赞同这些反馈动作，符合从上到下的阅读顺序。
 *
 * [building] 为 true 时界面还在生成：入口不隐藏、不置灰，而是换成「正在编排」+ 进度，
 * 点进去就能看着块一块块长出来——等待期间有反馈，比干等强。
 *
 * 视觉上用中性卡片 + 描边，不铺橙色底：它是入口，不是告警，强调色留给真正的操作按钮。
 */
@Composable
fun A2uiEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    building: Boolean = false,
    blockCount: Int = 0,
) {
    val shape = RoundedCornerShape(A2uiRadius.block)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(A2uiColor.surface)
            .border(1.dp, A2uiColor.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = A2uiSpace.md, vertical = A2uiSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
    ) {
        if (building) {
            Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = A2uiColor.accent,
                )
            }
        } else {
            A2uiIconCircle(icon = "guide", size = 30.dp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs),
        ) {
            Text(
                text = if (building) "正在编排个性化界面" else "进入个性化界面",
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.body,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = when {
                    building && blockCount > 0 -> "已排好 $blockCount 块，点开看它一屏屏长出来"
                    building -> "正在整理内容，点开可看生成过程"
                    title.isNotBlank() -> "AI 已排好「$title」"
                    else -> "AI 为这次回答排好了一屏界面"
                },
                color = A2uiColor.textTertiary,
                fontSize = A2uiText.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Image(
            painter = painterResource(Res.drawable.chevron_right),
            contentDescription = null,
            colorFilter = ColorFilter.tint(A2uiColor.textTertiary),
            modifier = Modifier.size(14.dp),
        )
    }
}
