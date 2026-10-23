package com.xlin.travel.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.navigation.PageTopBar
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.dark
import travel.app.shared.generated.resources.email
import travel.app.shared.generated.resources.help
import travel.app.shared.generated.resources.log_out
import travel.app.shared.generated.resources.phone_number
import travel.app.shared.generated.resources.privacy
import travel.app.shared.generated.resources.safe
import travel.app.shared.generated.resources.terms
import travel.app.shared.generated.resources.user
import travel.app.shared.generated.resources.vibe

/**
 * 设置界面：1:1 还原 UI设计/设置界面UI初稿.html。
 * 账号 / 应用 / 关于 三个分组卡片 + 退出登录；
 * 带 Chevron Right 的条目进入子界面，未开发的子界面显示“XXX即将上线”。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenThemeColor: () -> Unit,
    onOpenComingSoon: (String) -> Unit,
    themeColor: ThemeColorState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = "设置", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // ---------- 账号 ----------
            SectionTitle("账号")
            SettingsCard {
                SettingRow(Res.drawable.user, "用户名")
                SettingRowDivider()
                SettingRow(Res.drawable.phone_number, "手机号")
                SettingRowDivider()
                SettingRow(Res.drawable.email, "邮箱")
                SettingRowDivider()
                SettingRow(
                    icon = Res.drawable.safe,
                    label = "账号控制",
                    showChevron = true,
                    onClick = { onOpenComingSoon("账号控制") },
                )
            }

            // ---------- 应用 ----------
            SectionTitle("应用")
            SettingsCard {
                SettingRow(
                    icon = Res.drawable.dark,
                    label = "主题颜色",
                    value = themeColor.current.label,
                    showChevron = true,
                    onClick = onOpenThemeColor,
                )
            }

            // ---------- 关于 ----------
            SectionTitle("关于")
            SettingsCard {
                SettingRow(
                    icon = Res.drawable.help,
                    label = "帮助中心",
                    showChevron = true,
                    onClick = { onOpenComingSoon("帮助中心") },
                )
                SettingRowDivider()
                SettingRow(
                    icon = Res.drawable.terms,
                    label = "服务条款",
                    showChevron = true,
                    onClick = { onOpenComingSoon("服务条款") },
                )
                SettingRowDivider()
                SettingRow(
                    icon = Res.drawable.privacy,
                    label = "隐私政策",
                    showChevron = true,
                    onClick = { onOpenComingSoon("隐私政策") },
                )
                SettingRowDivider()
                SettingRow(Res.drawable.vibe, "Vibe", value = "Bate")
            }

            // ---------- 退出登录 ----------
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                LogoutRow()
            }

            // 对应 HTML body padding-bottom: 32px
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 退出登录（.logout）：图标与文字整体居中，统一强调橙；账号系统未接入前仅保留按下反馈 */
@Composable
private fun LogoutRow() {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (pressed) SettingItemPressed else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { /* 暂未接入账号系统 */ }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.log_out),
            contentDescription = "退出登录",
            colorFilter = ColorFilter.tint(AccentOrange),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = "退出登录",
            color = AccentOrange,
            fontSize = 16.sp,
        )
    }
}
