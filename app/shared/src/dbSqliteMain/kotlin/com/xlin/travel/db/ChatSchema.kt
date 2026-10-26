package com.xlin.travel.db

/** 数据库文件名，Android / iOS / JVM 共用 */
internal const val ChatDatabaseName = "travel_chat.db"

/**
 * 数据库结构。
 * 所有语句都是 IF NOT EXISTS，每次打开连接时直接执行即可完成建表。
 */
internal object ChatSchema {

    val statements: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS conversation (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL DEFAULT '新对话',
            model TEXT,
            system_prompt TEXT,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent(),

        """
        CREATE TABLE IF NOT EXISTS message (
            id TEXT PRIMARY KEY,
            conversation_id TEXT NOT NULL,
            role TEXT NOT NULL,
            content TEXT NOT NULL DEFAULT '',
            reasoning TEXT NOT NULL DEFAULT '',
            image_path TEXT,
            seq INTEGER NOT NULL,
            status TEXT NOT NULL DEFAULT 'done',
            prompt_tokens INTEGER,
            completion_tokens INTEGER,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            FOREIGN KEY (conversation_id)
                REFERENCES conversation(id)
                ON DELETE CASCADE
        )
        """.trimIndent(),

        """
        CREATE INDEX IF NOT EXISTS idx_message_conv
            ON message(conversation_id, seq)
        """.trimIndent(),

        """
        CREATE INDEX IF NOT EXISTS idx_conversation_updated
            ON conversation(updated_at DESC)
        """.trimIndent(),

        """
        CREATE TABLE IF NOT EXISTS app_setting (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        )
        """.trimIndent(),
    )
}
