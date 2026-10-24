package com.xlin.travel.drawer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.ConversationRecord

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

    /** 搜索结果（标题或消息内容匹配） */
    var searchResults: List<ChatHistoryItem> by mutableStateOf(emptyList())
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
        searchResults = searchResults.filterNot { it.id == id }
    }

    /** 重命名一条会话：写库 + 就地更新列表与搜索结果里的标题，避免整表刷新闪烁 */
    suspend fun rename(id: String, newTitle: String) {
        val title = newTitle.trim()
        if (title.isEmpty()) return
        runCatching { repository.renameConversation(id, title) }
        val index = _items.indexOfFirst { it.id == id }
        if (index >= 0) _items[index] = _items[index].copy(title = title)
        searchResults = searchResults.map { if (it.id == id) it.copy(title = title) else it }
    }

    /** 新建对话：只清空选中项，数据库记录会在首次发送消息时创建 */
    fun newChat() {
        activeId = null
        searchResults = emptyList()
    }

    /** 在当前历史中按关键词搜索 */
    suspend fun search() {
        val keyword = searchQuery.trim()
        if (keyword.isEmpty()) {
            searchResults = emptyList()
            return
        }
        searchResults = runCatching { repository.searchConversations(keyword) }
            .getOrDefault(emptyList())
            .map { it.toHistoryItem() }
    }

    /** 清空搜索关键词（离开搜索页时调用） */
    fun clearSearch() {
        searchQuery = ""
        searchResults = emptyList()
    }

    private fun ConversationRecord.toHistoryItem() = ChatHistoryItem(id = id, title = title)
}
