package com.xlin.travel.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.InputTextColor
import com.xlin.travel.color.SearchModalBackground
import com.xlin.travel.color.SearchModalBorder
import com.xlin.travel.color.SearchScrim
import com.xlin.travel.color.TextGray400
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextWhite
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.search

/**
 * 搜索弹窗，对应 HTML 的 #searchModal：遮罩 + 面板（搜索框 + 结果列表）。
 * 由 [ChatHistoryState.isSearchOpen] 控制显隐。
 */
@Composable
fun SearchDialog(history: ChatHistoryState) {
    if (!history.isSearchOpen) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchScrim)
            .clickable(onClick = { history.closeSearch() }),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 80.dp, end = 16.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SearchModalBackground)
                .border(1.dp, SearchModalBorder, RoundedCornerShape(12.dp))
                // 拦截面板内部点击，避免误触遮罩关闭
                .clickable(enabled = false) { },
        ) {
            SearchInputRow(history)
            SearchResults(history)
        }
    }
}

/** 搜索输入行：图标 + 输入框 + 取消 */
@Composable
private fun SearchInputRow(history: ChatHistoryState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.search),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(12.dp))
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
        Spacer(Modifier.size(12.dp))
        Text(
            text = "取消",
            color = TextGray400,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = { history.closeSearch() }),
        )
    }
    HorizontalDivider(color = SearchModalBorder)
}

/** 搜索结果区 */
@Composable
private fun SearchResults(history: ChatHistoryState) {
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
                .padding(vertical = 24.dp),
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .padding(8.dp),
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
                    .clickable { history.closeSearch() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}
