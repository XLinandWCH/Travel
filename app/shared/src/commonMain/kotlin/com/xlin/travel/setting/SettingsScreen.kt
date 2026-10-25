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
import com.xlin.travel.color.AppTheme
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.navigation.PageTopBar
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.dark
import travel.app.shared.generated.resources.email
import travel.app.shared.generated.resources.help
import travel.app.shared.generated.resources.history
import travel.app.shared.generated.resources.log_out
import travel.app.shared.generated.resources.message
import travel.app.shared.generated.resources.phone_number
import travel.app.shared.generated.resources.privacy
import travel.app.shared.generated.resources.safe
import travel.app.shared.generated.resources.terms
import travel.app.shared.generated.resources.user
import travel.app.shared.generated.resources.vibe

/**
 * 账号 / 应用 / 关于 三个分组卡片 + 退出登录；
 * 帮助中心 / 服务条款 / 隐私政策 与主题颜色、账号控制一样都是跳转到独立子页面。
 */
@Composable
fun SettingsScreen(
    username: String,
    maskedPhone: String,
    email: String,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenThemeColor: () -> Unit,
    onOpenMessageCenter: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    onOpenDoc: (LegalDoc) -> Unit,
    onOpenAccountControl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 背景铺满整屏（画到系统栏之下），列表内容自己避开系统栏
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
                SettingRow(Res.drawable.user, "用户名", value = username)
                SettingRowDivider()
                SettingRow(Res.drawable.phone_number, "手机号", value = maskedPhone)
                SettingRowDivider()
                SettingRow(Res.drawable.email, "邮箱", value = email)
                SettingRowDivider()
                SettingRow(
                    icon = Res.drawable.safe,
                    label = "账号控制",
                    showChevron = true,
                    onClick = onOpenAccountControl,
                )
            }

            // ---------- 应用 ----------
            SectionTitle("应用")
            SettingsCard {
                SettingRow(
                    icon = Res.drawable.dark,
                    label = "主题颜色",
                    value = AppTheme.preference.label,
                    showChevron = true,
                    onClick = onOpenThemeColor,
                )
                SettingRowDivider()
                SettingRow(
                    icon = Res.drawable.message,
                    label = "消息中心",
                    showChevron = true,
                    onClick = onOpenMessageCenter,
                )
                SettingRowDivider()
                SettingRow(
                    icon = Res.drawable.history,
                    label = "历史管理",
                    showChevron = true,
                    onClick = onOpenHistory,
                )
            }

            // ---------- 关于 ----------
            SectionTitle("关于")
            SettingsCard {
                SettingRow(
                    icon = Res.drawable.help,
                    label = "帮助中心",
                    showChevron = true,
                    onClick = onOpenHelpCenter,
                )
                SettingRowDivider()
                SettingRow(
                    icon = Res.drawable.terms,
                    label = "服务条款",
                    showChevron = true,
                    onClick = { onOpenDoc(LegalDoc.Terms) },
                )
                SettingRowDivider()
                SettingRow(
                    icon = Res.drawable.privacy,
                    label = "隐私政策",
                    showChevron = true,
                    onClick = { onOpenDoc(LegalDoc.Privacy) },
                )
                SettingRowDivider()
                SettingRow(Res.drawable.vibe, "当前版本", value = "Bate")
            }

            // ---------- 退出登录 ----------
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                LogoutRow(onLogout = onLogout)
            }

            // 对应 HTML body padding-bottom: 32px
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 退出登录（.logout）：图标与文字整体居中，统一强调橙；点击进入登录页 */
@Composable
private fun LogoutRow(onLogout: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (pressed) SettingItemPressed else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { onLogout() }
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
