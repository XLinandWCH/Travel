package com.xlin.travel.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.InputTextColor
import com.xlin.travel.color.SearchModalBackground
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextGray400
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.drawer.ChatHistoryItem
import com.xlin.travel.drawer.ChatHistoryState
import com.xlin.travel.navigation.PageTopBar
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.search

/**
 * 搜索历史页面：独立页面提供搜索输入与历史记录结果。
 */
@Composable
fun SearchScreen(
    history: ChatHistoryState,
    onBack: () -> Unit,
    onSelectItem: (ChatHistoryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding()
            .imePadding(),
    ) {
        PageTopBar(title = "搜索历史", onBack = onBack)
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

/** 搜索结果区 */
@Composable
private fun SearchResults(
    history: ChatHistoryState,
    onSelectItem: (ChatHistoryItem) -> Unit,
) {
    val keyword = history.searchQuery.trim()
    val results = history.searchResults

    val hint = when {
        keyword.isEmpty() -> "输入关键词搜索历史对话"
        results.isEmpty() -> "没有找到匹配的对话"
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(results, key = { it.id }) { item ->
            Text(
                text = item.title,
                color = TextGray300,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelectItem(item) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}
