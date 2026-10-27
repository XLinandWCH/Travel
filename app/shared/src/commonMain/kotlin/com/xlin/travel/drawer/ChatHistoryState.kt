package com.xlin.travel.drawer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.ConversationRecord
import com.xlin.travel.db.MessageSearchHit

/** 一条历史对话 */
data class ChatHistoryItem(val id: String, val title: String)

/**
 * 抽屉导航的状态：历史列表、当前选中项与搜索关键词。
 * 历史数据来自 SQLite，[refresh] / [search] 都是挂起函数，由界面层在协程中调用。
 */
@Stable
class ChatHistoryState(private val repository: ChatRepository) {

    private val _items = mutableStateListOf<ChatHistoryItem>()

    /** 历史列表（只读视图） */
    val items: List<ChatHistoryItem> get() = _items

    /** 当前选中的会话 ID；新建对话时为 null */
    var activeId: String? by mutableStateOf(null)
        private set

    /** 搜索关键词 */
    var searchQuery: String by mutableStateOf("")

    /** 搜索结果：命中的消息片段（含具体正文，而非仅标题） */
    var searchResults: List<MessageSearchHit> by mutableStateOf(emptyList())
        private set

    /** 是否还有下一页可加载（下滑触底时继续查询） */
    var searchHasMore: Boolean by mutableStateOf(false)
        private set

    /** 正在加载下一页，避免触底事件重复触发 */
    var searchLoadingMore: Boolean by mutableStateOf(false)
        private set

    /** 从数据库重新加载历史列表 */
    suspend fun refresh() {
        val records = runCatching { repository.conversations() }.getOrDefault(emptyList())
        _items.clear()
        _items.addAll(records.map { it.toHistoryItem() })
    }

    fun select(id: String) {
        activeId = id
    }

    /** 删除一条会话：写库 + 从当前列表与搜索结果中移除；若删的是当前选中项则取消选中 */
    suspend fun delete(id: String) {
        runCatching { repository.deleteConversation(id) }
        if (activeId == id) activeId = null
        _items.removeAll { it.id == id }
        searchResults = searchResults.filterNot { it.conversationId == id }
    }

    /** 重命名一条会话：写库 + 就地更新列表与搜索结果里的标题，避免整表刷新闪烁 */
    suspend fun rename(id: String, newTitle: String) {
        val title = newTitle.trim()
        if (title.isEmpty()) return
        runCatching { repository.renameConversation(id, title) }
        val index = _items.indexOfFirst { it.id == id }
        if (index >= 0) _items[index] = _items[index].copy(title = title)
        searchResults = searchResults.map {
            if (it.conversationId == id) it.copy(conversationTitle = title) else it
        }
    }

    /** 新建对话：只清空选中项，数据库记录会在首次发送消息时创建 */
    fun newChat() {
        activeId = null
        searchResults = emptyList()
        searchHasMore = false
    }

    /** 重新搜索：只取最近 [SearchPageSize] 条命中消息，并重置分页进度 */
    suspend fun search() {
        val keyword = searchQuery.trim()
        if (keyword.isEmpty()) {
            searchResults = emptyList()
            searchHasMore = false
            return
        }
        searchResults = runCatching { repository.searchMessageHits(keyword, SearchPageSize, 0) }
            .getOrDefault(emptyList())
        searchHasMore = searchResults.size == SearchPageSize
    }

    /** 下滑触底后加载下一页；追加到现有结果末尾，没有更多时置空标记 */
    suspend fun loadMoreSearchResults() {
        val keyword = searchQuery.trim()
        if (keyword.isEmpty() || !searchHasMore || searchLoadingMore) return
        searchLoadingMore = true
        val next = runCatching {
            repository.searchMessageHits(keyword, SearchPageSize, searchResults.size)
        }.getOrDefault(emptyList())
        searchLoadingMore = false
        if (next.isEmpty()) {
            searchHasMore = false
            return
        }
        searchResults = searchResults + next
        searchHasMore = next.size == SearchPageSize
    }

    /** 清空搜索关键词（离开搜索页时调用） */
    fun clearSearch() {
        searchQuery = ""
        searchResults = emptyList()
        searchHasMore = false
    }

    private fun ConversationRecord.toHistoryItem() = ChatHistoryItem(id = id, title = title)

    private companion object {
        /** 搜索单页条数：首次只显示最近 10 条，触底再追加 */
        const val SearchPageSize = 10
    }
}
