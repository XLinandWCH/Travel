package com.xlin.travel.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

private const val Columns = "id, title, model, system_prompt, created_at, updated_at"

/** 插入一条会话 */
internal fun SQLiteConnection.insertConversation(record: ConversationRecord) {
    withStatement("INSERT INTO conversation($Columns) VALUES (?, ?, ?, ?, ?, ?)") { statement ->
        statement.bindText(1, record.id)
        statement.bindText(2, record.title)
        statement.bindTextOrNull(3, record.model)
        statement.bindTextOrNull(4, record.systemPrompt)
        statement.bindLong(5, record.createdAt)
        statement.bindLong(6, record.updatedAt)
        statement.step()
    }
}

/** 按最后活跃时间倒序读取全部会话 */
internal fun SQLiteConnection.selectConversations(): List<ConversationRecord> =
    withStatement("SELECT $Columns FROM conversation ORDER BY updated_at DESC") { statement ->
        buildList {
            while (statement.step()) {
                add(statement.readConversation())
            }
        }
    }

/** 搜索标题或消息内容中包含关键词的会话 */
internal fun SQLiteConnection.searchConversations(keyword: String): List<ConversationRecord> {
    val pattern = "%${escapeLikePattern(keyword)}%"
    val sql = """
        SELECT $Columns FROM conversation c
        WHERE c.title LIKE ? ESCAPE '\'
           OR EXISTS (
               SELECT 1 FROM message m
               WHERE m.conversation_id = c.id AND m.content LIKE ? ESCAPE '\'
           )
        ORDER BY c.updated_at DESC
    """.trimIndent()
    return withStatement(sql) { statement ->
        statement.bindText(1, pattern)
        statement.bindText(2, pattern)
        buildList {
            while (statement.step()) {
                add(statement.readConversation())
            }
        }
    }
}

/** 更新会话的最后活跃时间（用于抽屉排序） */
internal fun SQLiteConnection.updateConversationTimestamp(id: String, updatedAt: Long) {
    withStatement("UPDATE conversation SET updated_at = ? WHERE id = ?") { statement ->
        statement.bindLong(1, updatedAt)
        statement.bindText(2, id)
        statement.step()
    }
}

/** 更新会话标题（重命名），不改动最后活跃时间以免重排历史 */
internal fun SQLiteConnection.updateConversationTitle(id: String, title: String) {
    withStatement("UPDATE conversation SET title = ? WHERE id = ?") { statement ->
        statement.bindText(1, title)
        statement.bindText(2, id)
        statement.step()
    }
}

/** 删除会话；message 表通过外键级联删除 */
internal fun SQLiteConnection.deleteConversation(id: String) {
    withStatement("DELETE FROM conversation WHERE id = ?") { statement ->
        statement.bindText(1, id)
        statement.step()
    }
}

private fun SQLiteStatement.readConversation() = ConversationRecord(
    id = getText(0),
    title = getText(1),
    model = getTextOrNull(2),
    systemPrompt = getTextOrNull(3),
    createdAt = getLong(4),
    updatedAt = getLong(5),
)

private fun SQLiteStatement.bindTextOrNull(index: Int, value: String?) {
    if (value == null) bindNull(index) else bindText(index, value)
}
