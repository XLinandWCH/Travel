package com.xlin.travel.db

import androidx.sqlite.SQLiteConnection

/** app_setting 表的键名集中定义，value 一律用字符串存储 */
object SettingKeys {
    /** 主题颜色偏好，值为 ThemePreference 的枚举名（System / Dark / Light） */
    const val ThemePreference = "theme_preference"

    /** 登录 token，由 UserSession 读写；模型配置接口也用它鉴权 */
    const val AuthToken = "auth_token"

    /**
     * 服务端下发的模型配置（含 apiKey）缓存，供断网时兜底使用。
     * 退出登录 / 注销时会清空，避免密钥长期留在本地。
     */
    const val ModelConfigCache = "model_config_cache"

    /**
     * 服务端下发的语音识别（Fun-ASR-Flash）配置缓存，供断网兜底。
     * 退出登录 / 注销时随其它设置一并清空。
     */
    const val AsrConfigCache = "asr_config_cache"
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
