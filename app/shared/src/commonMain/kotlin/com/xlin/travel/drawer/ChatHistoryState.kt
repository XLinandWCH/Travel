package com.xlin.travel.drawer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 一条历史对话 */
data class ChatHistoryItem(val id: Long, val title: String)

/**
 * 抽屉导航的状态：历史列表、当前选中项、搜索弹窗开关与查询词。
 * 对应 UI设计/带导航抽屉的主聊天界面.html 中的侧边栏 + 搜索弹窗逻辑。
 */
@Stable
class ChatHistoryState(titles: List<String> = DefaultTitles) {

    private val _items = mutableStateListOf<ChatHistoryItem>().apply {
        titles.forEachIndexed { index, title -> add(ChatHistoryItem(index.toLong(), title)) }
    }

    /** 历史列表（只读视图） */
    val items: List<ChatHistoryItem> get() = _items

    /** 当前选中的历史项 */
    var activeId: Long by mutableStateOf(_items.firstOrNull()?.id ?: -1L)
        private set

    /** 搜索弹窗是否打开 */
    var isSearchOpen: Boolean by mutableStateOf(false)
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

    fun openSearch() {
        isSearchOpen = true
    }

    fun closeSearch() {
        isSearchOpen = false
        searchQuery = ""
    }

    /** 根据关键词过滤后的搜索结果；关键词为空时返回空列表 */
    val searchResults: List<ChatHistoryItem>
        get() {
            val keyword = searchQuery.trim().lowercase()
            if (keyword.isEmpty()) return emptyList()
            return _items.filter { it.title.lowercase().contains(keyword) }
        }

    companion object {
        /** 设计稿里的示例历史标题 */
        val DefaultTitles = listOf(
            "New chat",
            "矩阵计算的起源与应用",
            "心理支持对话",
            "AI虚假信息治理",
            "打破无聊时的选择",
            "VPN 分片功能特性",
            "Kotlin Multiplatform 架构设计",
            "Jetpack Compose 动画实践",
        )
    }
}
