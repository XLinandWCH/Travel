package com.xlin.travel.a2ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.xlin.travel.db.SurfaceRecord

/** 历史列表的一屏：原始记录（会话标题 / 时间）+ 解出来的界面结构 */
internal data class SurfaceEntry(
    val record: SurfaceRecord,
    val surface: A2uiSurface,
) {
    /** 列表里展示的块数：空块不算，模型占位不应该被算进「几块」 */
    val blockCount: Int get() = surface.blocks.count { !it.isEmpty }
}

/**
 * 个性化界面历史卡片：一屏一条，点击进入回看。
 *
 * 只展示「这一屏是什么」——标题、副标题、块数与来源会话，不放缩略图：
 * A2UI 的主色块与图片在小尺寸下会糊成一团，反而看不出是哪一屏。
 */
@Composable
internal fun SurfaceGalleryCard(
    entry: SurfaceEntry,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(A2uiRadius.block)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(A2uiColor.surface)
            .border(1.dp, A2uiColor.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = A2uiSpace.md, vertical = A2uiSpace.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(A2uiSpace.md),
    ) {
        A2uiIconCircle(icon = "guide", size = 34.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(A2uiSpace.xxs),
        ) {
            Text(
                text = entry.surface.title.ifBlank { "未命名界面" },
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.title,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.surface.subtitle.ifBlank { "AI 为这次回答编排的一屏界面" },
                color = A2uiColor.textTertiary,
                fontSize = A2uiText.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metaText(entry, now),
                color = A2uiColor.textMeta,
                fontSize = A2uiText.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 元信息：时间 · 块数 · 来源会话 */
private fun metaText(entry: SurfaceEntry, now: Long): String = buildString {
    append(relativeTimeLabel(entry.record.createdAt, now))
    append(" · ")
    append("${entry.blockCount} 块")
    entry.record.conversationTitle.takeIf { it.isNotBlank() }?.let {
        append(" · 来自「").append(it).append("」")
    }
}

/** 相对时间：列表只需要「多久以前」，不引入日期库也不跨时区出错 */
internal fun relativeTimeLabel(createdAt: Long, now: Long): String {
    val diff = (now - createdAt).coerceAtLeast(0L)
    return when {
        diff < Minute -> "刚刚"
        diff < Hour -> "${diff / Minute} 分钟前"
        diff < Day -> "${diff / Hour} 小时前"
        diff < 2 * Day -> "昨天"
        else -> "${diff / Day} 天前"
    }
}

private const val Minute = 60_000L
private const val Hour = 60 * Minute
private const val Day = 24 * Hour
