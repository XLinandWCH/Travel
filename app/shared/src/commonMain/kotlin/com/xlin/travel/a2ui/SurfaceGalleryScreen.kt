package com.xlin.travel.a2ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.currentEpochMillis
import com.xlin.travel.navigation.PageTopBar
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.down

/** 默认展示天数、可选上限与单页条数 */
private const val DefaultDays = 3
private const val MaxDays = 7
private const val MaxItems = 80
private const val MillisPerDay = 24 * 60 * 60 * 1000L

/**
 * 个性化窗口展示页：把近 N 天 AI 编排过的界面收成一条时间线，由新到旧。
 *
 * 为什么要单独一页：个性化界面此前只挂在「那一轮回答」下面，隔天想再看一眼
 * 得先翻回那个会话；这里按时间汇总，进来就能回看，天数可 1~7 天切换。
 *
 * 不做缩略图：A2UI 的色块与图片缩到卡片大小会糊成一团，反而认不出是哪一屏，
 * 所以列表只给标题 / 副标题 / 块数 / 来源会话，点进去看完整界面。
 */
@Composable
fun SurfaceGalleryScreen(
    repository: ChatRepository,
    onBack: () -> Unit,
    onOpenSurface: (A2uiSurface) -> Unit,
    modifier: Modifier = Modifier,
) {
    var days by remember { mutableStateOf(DefaultDays) }
    var entries by remember { mutableStateOf<List<SurfaceEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    // 相对时间的基准：切天数或重进页面时更新一次，避免停留久了显示「59 分钟前」
    var now by remember { mutableStateOf(currentEpochMillis()) }

    LaunchedEffect(days) {
        loading = true
        now = currentEpochMillis()
        val since = now - days * MillisPerDay
        val records = runCatching { repository.surfacesSince(since, MaxItems) }.getOrDefault(emptyList())
        // 解不出来的记录（旧版本写入的脏 JSON）直接丢弃，不让列表出现点不开的空卡
        entries = records.mapNotNull { record ->
            decodeA2uiSurface(record.surfaceJson)?.let { SurfaceEntry(record, it) }
        }
        loading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(A2uiColor.page)
            .safeContentPadding(),
    ) {
        PageTopBar(title = "个性化窗口展示", onBack = onBack)
        GalleryHeader(
            days = days,
            count = entries.size,
            loading = loading,
            onDaysChange = { days = it },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(26.dp),
                    color = A2uiColor.accent,
                    strokeWidth = 2.dp,
                )
                entries.isEmpty() -> GalleryEmptyState(days = days, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = A2uiSpace.lg,
                        end = A2uiSpace.lg,
                        bottom = A2uiSpace.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
                ) {
                    items(entries, key = { it.record.messageId }) { entry ->
                        SurfaceGalleryCard(
                            entry = entry,
                            now = now,
                            onClick = { onOpenSurface(entry.surface) },
                        )
                    }
                }
            }
        }
    }
}

/** 页头：左边说明这一页是什么，右边是可切换的「展示近 N 天」下拉菜单 */
@Composable
private fun GalleryHeader(
    days: Int,
    count: Int,
    loading: Boolean,
    onDaysChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = A2uiSpace.lg, vertical = A2uiSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "个性化界面",
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.title,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (loading) "正在读取…" else "共 $count 屏 · 由新到旧",
                color = A2uiColor.textTertiary,
                fontSize = A2uiText.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RangeMenu(days = days, onDaysChange = onDaysChange)
    }
}

/** 「展示近 N 天」：点击展开 1~7 天下拉菜单（不用弹窗，避免盖住整页） */
@Composable
private fun RangeMenu(days: Int, onDaysChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(A2uiRadius.chip))
                .background(A2uiColor.surfaceSunken)
                .clickable { expanded = true }
                .padding(horizontal = A2uiSpace.md, vertical = A2uiSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "展示近${days}天",
                color = A2uiColor.textSecondary,
                fontSize = A2uiText.caption,
            )
            Spacer(Modifier.width(A2uiSpace.xs))
            Image(
                painter = painterResource(Res.drawable.down),
                contentDescription = null,
                colorFilter = ColorFilter.tint(A2uiColor.textMeta),
                modifier = Modifier.size(12.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..MaxDays).forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "近${option}天",
                            color = if (option == days) A2uiColor.accent else A2uiColor.textPrimary,
                            fontSize = A2uiText.body,
                        )
                    },
                    onClick = {
                        expanded = false
                        if (option != days) onDaysChange(option)
                    },
                )
            }
        }
    }
}

/** 空态：不白屏，顺手告诉用户界面是怎么来的 */
@Composable
private fun GalleryEmptyState(days: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = A2uiSpace.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "近${days}天还没有生成过界面",
            color = A2uiColor.textPrimary,
            fontSize = A2uiText.title,
        )
        Spacer(Modifier.height(A2uiSpace.sm))
        Text(
            text = "在聊天里问一句路线或攻略，AI 会把回答再排成一屏，这里就能回看。",
            color = A2uiColor.textTertiary,
            fontSize = A2uiText.caption,
            textAlign = TextAlign.Center,
        )
    }
}
