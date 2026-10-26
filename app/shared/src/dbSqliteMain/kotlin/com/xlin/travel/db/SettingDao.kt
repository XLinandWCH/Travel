package com.xlin.travel.db

import androidx.sqlite.SQLiteConnection

/**
 * app_setting 表的读写 DAO。
 * 键名定义在 commonMain 的 [SettingKeys]（Web 端的 localStorage 也用同一套键）。
 */

/** 读取设置项，不存在时返回 null */
internal fun SQLiteConnection.selectSetting(key: String): String? =
    withStatement("SELECT value FROM app_setting WHERE key = ?") { statement ->
        statement.bindText(1, key)
        if (statement.step()) statement.getText(0) else null
    }

/** 写入设置项（存在则覆盖，不存在则插入） */
internal fun SQLiteConnection.upsertSetting(key: String, value: String) {
    withStatement(
        """
        INSERT INTO app_setting(key, value) VALUES (?, ?)
        ON CONFLICT(key) DO UPDATE SET value = excluded.value
        """.trimIndent()
    ) { statement ->
        statement.bindText(1, key)
        statement.bindText(2, value)
        statement.step()
    }
}
