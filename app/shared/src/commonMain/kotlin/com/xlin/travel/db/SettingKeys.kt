package com.xlin.travel.db

/**
 * 设置项的键名集中定义，value 一律用字符串存储。
 * 从 SettingDao.kt 拆出：键名是跨平台约定（SQLite 与 Web localStorage 都用它），
 * DAO 实现只属于非 Web 平台。
 */
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

    /**
     * 服务端下发的语音合成（Qwen-Audio-TTS-Flash）配置缓存，断网兜底，随其它设置一并清空。
     */
    const val TtsConfigCache = "tts_config_cache"

    /**
     * 服务端下发的高德地图密钥（map 表）本地缓存，供断网兜底；随登录态清理。
     */
    const val MapConfigCache = "map_config_cache"

    /** 聊天顶栏的「自动朗读最新回复」开关："1" 开 / 其余关，跨启动记住 */
    const val TtsAutoRead = "tts_auto_read"
}
