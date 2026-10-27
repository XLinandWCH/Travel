package com.xlin.travel.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.InputTextColor
import com.xlin.travel.color.SearchModalBackground
import com.xlin.travel.color.SidebarItemActive
import com.xlin.travel.color.TextGray400
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.db.MessageSearchHit
import com.xlin.travel.drawer.ChatHistoryState
import com.xlin.travel.navigation.PageTopBar
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.search

/**
 * 搜索页：结果直接展示命中的消息正文片段（单行 + 强调色标出关键字），
 * 首屏只查最近 10 条，下滑触底后分页追加。
 */
@Composable
fun SearchScreen(
    history: ChatHistoryState,
    onBack: () -> Unit,
    onSelectItem: (MessageSearchHit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding()
            .imePadding(),
    ) {
        PageTopBar(title = "搜索消息", onBack = onBack)
        SearchInputRow(history)
        SearchResults(history, onSelectItem = onSelectItem)
    }

    // 输入停顿后再查数据库，避免每敲一个字都查一次
    LaunchedEffect(history.searchQuery) {
        delay(250)
        history.search()
    }
}

/** 搜索输入行：图标 + 输入框 + 清除 */
@Composable
private fun SearchInputRow(history: ChatHistoryState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SearchModalBackground)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.search),
            contentDescription = null,
            colorFilter = ColorFilter.tint(TextGray400),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = history.searchQuery,
            onValueChange = { history.searchQuery = it },
            singleLine = true,
            textStyle = TextStyle(color = InputTextColor, fontSize = 15.sp),
            cursorBrush = SolidColor(TextWhite),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (history.searchQuery.isEmpty()) {
                        Text("搜索聊天记录...", color = TextGray500, fontSize = 15.sp)
                    }
                    inner()
                }
            },
        )
        if (history.searchQuery.isNotEmpty()) {
            Text(
                text = "清除",
                color = TextGray400,
                fontSize = 13.sp,
                modifier = Modifier.clickable { history.clearSearch() },
            )
        }
    }
}

/** 搜索结果区：下滑触底自动加载下一页 */
@Composable
private fun SearchResults(
    history: ChatHistoryState,
    onSelectItem: (MessageSearchHit) -> Unit,
) {
    val keyword = history.searchQuery.trim()
    val results = history.searchResults

    val hint = when {
        keyword.isEmpty() -> "输入关键词搜索聊天记录"
        results.isEmpty() -> "没有找到匹配的消息"
        else -> null
    }
    if (hint != null) {
        Text(
            text = hint,
            color = TextGray500,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        )
        return
    }

    val listState = rememberLazyListState()
    // 触底检测：最后一个可见项接近列表末尾时加载下一页
    LaunchedEffect(listState, results.size, history.searchHasMore) {
        val info = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (
            history.searchHasMore &&
            !history.searchLoadingMore &&
            info.totalItemsCount > 0 &&
            lastVisible >= info.totalItemsCount - 2
        ) {
            history.loadMoreSearchResults()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(results, key = { it.messageId }) { hit ->
            SearchHitRow(keyword, hit, onSelectItem)
        }
        if (history.searchHasMore) {
            item(key = "search_footer") {
                Text(
                    text = if (history.searchLoadingMore) "加载中…" else "继续下滑查看更多",
                    color = TextGray500,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 单条命中结果：首行会话标题，次行关键字所在的正文片段（只占一行，超出省略） */
@Composable
private fun SearchHitRow(
    keyword: String,
    hit: MessageSearchHit,
    onSelectItem: (MessageSearchHit) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (pressed) SidebarItemActive else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) {
                onSelectItem(hit)
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = hit.conversationTitle,
            color = TextGray500,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(3.dp))
        Text(
            text = buildSearchSnippet(hit.content, keyword),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
