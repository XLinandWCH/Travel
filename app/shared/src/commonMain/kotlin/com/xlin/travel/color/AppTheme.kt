package com.xlin.travel.color

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 主题颜色选项。
 * 枚举名会写入数据库 app_setting 表（SettingKeys.ThemePreference），修改名称需考虑旧值兼容。
 */
enum class ThemePreference(val label: String) {
    System("跟随系统"),
    Dark("深色"),
    Light("浅色"),
}

/**
 * 全局主题状态。AppColors.kt 中所有颜色 getter 都根据 [isDark] 返回深 / 浅两套值，
 * 切换主题时写入 [preference] 或 [systemIsDark]，所有读取这些颜色的组合函数自动重组。
 *
 * [preference] 的持久化由 App.kt 负责（启动时从数据库恢复，选择时写回数据库）。
 */
@Stable
object AppTheme {

    /** 用户选择的主题偏好（设置页“主题颜色”显示与回写） */
    var preference: ThemePreference by mutableStateOf(ThemePreference.Dark)

    /** 系统当前是否深色模式，由 App.kt 用 isSystemInDarkTheme() 持续同步，供 System 使用 */
    var systemIsDark: Boolean by mutableStateOf(true)

    /** 颜色取值时实际生效的是否深色 */
    val isDark: Boolean
        get() = when (preference) {
            ThemePreference.System -> systemIsDark
            ThemePreference.Dark -> true
            ThemePreference.Light -> false
        }
}

/** 按当前主题在深 / 浅两个值之间取值（在组合中调用即可随主题切换重组） */
internal fun themeColor(dark: Color, light: Color): Color =
    if (AppTheme.isDark) dark else light
