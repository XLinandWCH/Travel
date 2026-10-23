package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.navigation.PageTopBar
import org.jetbrains.compose.resources.DrawableResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.dark
import travel.app.shared.generated.resources.light
import travel.app.shared.generated.resources.phone_number

/** 主题颜色选项 */
enum class ThemeColorOption(val label: String, val icon: DrawableResource) {
    System("跟随系统", Res.drawable.phone_number),
    Dark("深色", Res.drawable.dark),
    Light("浅色", Res.drawable.light),
}

/**
 * 主题颜色偏好状态。
 * 目前仅记录选择并回显在设置列表中，浅色主题样式暂未全局实现。
 */
@Stable
class ThemeColorState {

    /** 默认值与设置界面初稿一致：深色 */
    var current: ThemeColorOption by mutableStateOf(ThemeColorOption.Dark)
        internal set

    fun select(option: ThemeColorOption) {
        current = option
    }
}

/**
 * 主题颜色子界面：单选列表，选中项右侧显示强调橙对勾。
 * 沿用设置界面的分组卡片样式，保持配色一致。
 */
@Composable
fun ThemeColorScreen(
    state: ThemeColorState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = "主题颜色", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            SectionTitle("外观")
            SettingsCard {
                ThemeColorOption.entries.forEachIndexed { index, option ->
                    if (index > 0) SettingRowDivider()
                    SettingRow(
                        icon = option.icon,
                        label = option.label,
                        selected = state.current == option,
                        onClick = { state.select(option) },
                    )
                }
            }
        }
    }
}
