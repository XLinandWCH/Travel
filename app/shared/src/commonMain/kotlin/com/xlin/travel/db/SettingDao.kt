package com.xlin.travel.db

import androidx.sqlite.SQLiteConnection

/** app_setting 表的键名集中定义，value 一律用字符串存储 */
object SettingKeys {
    /** 主题颜色偏好，值为 ThemePreference 的枚举名（System / Dark / Light） */
    const val ThemePreference = "theme_preference"
}

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
