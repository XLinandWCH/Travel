package com.xlin.travel.drawer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 一条历史对话 */
data class ChatHistoryItem(val id: Long, val title: String)

/**
 * 抽屉导航的状态：历史列表、当前选中项与搜索关键词。
 */
@Stable
class ChatHistoryState {

    private val _items = mutableStateListOf<ChatHistoryItem>()

    /** 历史列表（只读视图） */
    val items: List<ChatHistoryItem> get() = _items

    /** 当前选中的历史项 */
    var activeId: Long by mutableStateOf(-1L)
        private set

    /** 搜索关键词 */
    var searchQuery: String by mutableStateOf("")

    fun select(id: Long) {
        activeId = id
    }

    /** 新建一个会话并置顶选中 */
    fun newChat() {
        val id = (_items.maxOfOrNull { it.id } ?: 0L) + 1L
        _items.add(0, ChatHistoryItem(id, "New chat"))
        activeId = id
    }

    /** 清空搜索关键词（离开搜索页时调用） */
    fun clearSearch() {
        searchQuery = ""
    }

    /** 根据关键词过滤后的搜索结果；关键词为空时返回空列表 */
    val searchResults: List<ChatHistoryItem>
        get() {
            val keyword = searchQuery.trim().lowercase()
            if (keyword.isEmpty()) return emptyList()
            return _items.filter { it.title.lowercase().contains(keyword) }
        }
}
