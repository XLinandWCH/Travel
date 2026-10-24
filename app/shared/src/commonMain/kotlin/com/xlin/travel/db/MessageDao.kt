package com.xlin.travel.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

private const val Columns =
    "id, conversation_id, role, content, reasoning, seq, status, " +
        "prompt_tokens, completion_tokens, created_at, updated_at"

/** 取会话内下一个顺序号（从 1 开始） */
internal fun SQLiteConnection.nextMessageSeq(conversationId: String): Long =
    withStatement("SELECT COALESCE(MAX(seq), 0) + 1 FROM message WHERE conversation_id = ?") { statement ->
        statement.bindText(1, conversationId)
        if (statement.step()) statement.getLong(0) else 1L
    }

/** 插入一条消息 */
internal fun SQLiteConnection.insertMessage(record: MessageRecord) {
    withStatement("INSERT INTO message($Columns) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)") { statement ->
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
)

private fun SQLiteStatement.bindIntOrNull(index: Int, value: Int?) {
    if (value == null) bindNull(index) else bindInt(index, value)
}
