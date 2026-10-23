package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlin.travel.color.AppTheme
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.ThemePreference
import com.xlin.travel.navigation.PageTopBar
import org.jetbrains.compose.resources.DrawableResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.dark
import travel.app.shared.generated.resources.light
import travel.app.shared.generated.resources.phone_number

/** 各主题选项在主题颜色页使用的图标 */
internal val ThemePreference.iconRes: DrawableResource
    get() = when (this) {
        ThemePreference.System -> Res.drawable.phone_number
        ThemePreference.Dark -> Res.drawable.dark
        ThemePreference.Light -> Res.drawable.light
    }

/**
 * 主题颜色子界面：单选列表，选中项右侧显示强调橙对勾。
 * 点选立即全局生效（[AppTheme.preference]），[onSelect] 由外层负责写入数据库持久化。
 */
@Composable
fun ThemeColorScreen(
    onSelect: (ThemePreference) -> Unit,
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
                ThemePreference.entries.forEachIndexed { index, option ->
                    if (index > 0) SettingRowDivider()
                    SettingRow(
                        icon = option.iconRes,
                        label = option.label,
                        selected = AppTheme.preference == option,
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}
