package com.xlin.travel.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

private const val Columns =
    "id, conversation_id, role, content, reasoning, seq, status, " +
        "prompt_tokens, completion_tokens, created_at, updated_at, image_path, document_path, document_name"

/** 取会话内下一个顺序号（从 1 开始） */
internal fun SQLiteConnection.nextMessageSeq(conversationId: String): Long =
    withStatement("SELECT COALESCE(MAX(seq), 0) + 1 FROM message WHERE conversation_id = ?") { statement ->
        statement.bindText(1, conversationId)
        if (statement.step()) statement.getLong(0) else 1L
    }

/** 插入一条消息 */
internal fun SQLiteConnection.insertMessage(record: MessageRecord) {
    withStatement("INSERT INTO message($Columns) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)") { statement ->
        statement.bindText(1, record.id)
        statement.bindText(2, record.conversationId)
        statement.bindText(3, record.role)
        statement.bindText(4, record.content)
        statement.bindText(5, record.reasoning)
        statement.bindLong(6, record.seq)
        statement.bindText(7, record.status)
        statement.bindIntOrNull(8, record.promptTokens)
        statement.bindIntOrNull(9, record.completionTokens)
        statement.bindLong(10, record.createdAt)
        statement.bindLong(11, record.updatedAt)
        statement.bindTextOrNull(12, record.imagePath)
        statement.bindTextOrNull(13, record.documentPath)
        statement.bindTextOrNull(14, record.documentName)
        statement.step()
    }
}

/** 会话内最后一条消息的 seq；没有消息时返回 0 */
internal fun SQLiteConnection.lastMessageSeq(conversationId: String): Long =
    withStatement("SELECT COALESCE(MAX(seq), 0) FROM message WHERE conversation_id = ?") { statement ->
        statement.bindText(1, conversationId)
        if (statement.step()) statement.getLong(0) else 0L
    }

/** 按会话内顺序读取消息；[untilSeq] 用于只读取某一轮之前的上下文 */
internal fun SQLiteConnection.selectMessages(
    conversationId: String,
    untilSeq: Long? = null,
): List<MessageRecord> {
    val sql = if (untilSeq == null) {
        "SELECT $Columns FROM message WHERE conversation_id = ? ORDER BY seq"
    } else {
        "SELECT $Columns FROM message WHERE conversation_id = ? AND seq <= ? ORDER BY seq"
    }
    return withStatement(sql) { statement ->
        statement.bindText(1, conversationId)
        if (untilSeq != null) statement.bindLong(2, untilSeq)
        buildList {
            while (statement.step()) {
                add(statement.readMessage())
            }
        }
    }
}

/** 更新消息内容与状态（流式结束 / 失败时调用） */
internal fun SQLiteConnection.updateMessage(
    id: String,
    content: String,
    reasoning: String,
    status: String,
    updatedAt: Long,
) {
    withStatement(
        "UPDATE message SET content = ?, reasoning = ?, status = ?, updated_at = ? WHERE id = ?"
    ) { statement ->
        statement.bindText(1, content)
        statement.bindText(2, reasoning)
        statement.bindText(3, status)
        statement.bindLong(4, updatedAt)
        statement.bindText(5, id)
        statement.step()
    }
}

/** 删除单条消息（重新生成时先移除旧的 AI 回答） */
internal fun SQLiteConnection.deleteMessage(id: String) {
    withStatement("DELETE FROM message WHERE id = ?") { statement ->
        statement.bindText(1, id)
        statement.step()
    }
}

/** 取某条消息在会话内的顺序号；记录不存在时返回 null */
internal fun SQLiteConnection.messageSeq(id: String): Long? =
    withStatement("SELECT seq FROM message WHERE id = ?") { statement ->
        statement.bindText(1, id)
        if (statement.step()) statement.getLong(0) else null
    }

/** 「编辑重发」：删除会话内 seq >= [fromSeq] 的全部消息（含被编辑的那条及其后续） */
internal fun SQLiteConnection.deleteMessagesFrom(conversationId: String, fromSeq: Long) {
    withStatement("DELETE FROM message WHERE conversation_id = ? AND seq >= ?") { statement ->
        statement.bindText(1, conversationId)
        statement.bindLong(2, fromSeq)
        statement.step()
    }
}

/** 转义 LIKE 通配符，避免用户输入的 % / _ 被当作模式匹配 */
internal fun escapeLikePattern(keyword: String): String =
    keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

/** 按消息内容搜索命中片段，带会话标题；按时间倒序，[limit]/[offset] 分页 */
internal fun SQLiteConnection.searchMessageHits(
    keyword: String,
    limit: Int,
    offset: Int,
): List<MessageSearchHit> {
    val sql = """
        SELECT m.id, m.conversation_id, c.title, m.role, m.content, m.created_at
        FROM message m JOIN conversation c ON c.id = m.conversation_id
        WHERE m.content LIKE ? ESCAPE '\'
        ORDER BY m.created_at DESC
        LIMIT ? OFFSET ?
    """.trimIndent()
    return withStatement(sql) { statement ->
        statement.bindText(1, "%${escapeLikePattern(keyword)}%")
        statement.bindLong(2, limit.toLong())
        statement.bindLong(3, offset.toLong())
        buildList {
            while (statement.step()) {
                add(
                    MessageSearchHit(
                        messageId = statement.getText(0),
                        conversationId = statement.getText(1),
                        conversationTitle = statement.getText(2),
                        role = statement.getText(3),
                        content = statement.getText(4),
                        createdAt = statement.getLong(5),
                    )
                )
            }
        }
    }
}

private fun SQLiteStatement.readMessage() = MessageRecord(
    id = getText(0),
    conversationId = getText(1),
    role = getText(2),
    content = getText(3),
    reasoning = getText(4),
    seq = getLong(5),
    status = getText(6),
    promptTokens = getIntOrNull(7),
    completionTokens = getIntOrNull(8),
    createdAt = getLong(9),
    updatedAt = getLong(10),
    imagePath = getTextOrNull(11),
    documentPath = getTextOrNull(12),
    documentName = getTextOrNull(13),
)

private fun SQLiteStatement.bindIntOrNull(index: Int, value: Int?) {
    if (value == null) bindNull(index) else bindInt(index, value)
}

private fun SQLiteStatement.bindTextOrNull(index: Int, value: String?) {
    if (value == null) bindNull(index) else bindText(index, value)
}
