package com.xlin.travel.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.AvatarBackground
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.SettingChevron
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextWhite
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.image_picker.rememberImagePicker
import com.xlin.travel.login.AuthApi
import com.xlin.travel.login.UserSession
import com.xlin.travel.navigation.PageTopBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.change_password
import travel.app.shared.generated.resources.chevron_right
import travel.app.shared.generated.resources.delete_account
import travel.app.shared.generated.resources.user

/**
 * 账号控制页：头像、修改用户名、修改密码（服务端 14 天冷却）、注销账号（二次确认倒计时弹窗）。
 * 用户名 / 密码走后端 /api/auth 接口并回写本地会话；注销先清本地再请求服务端，最后回到未登入界面。
 */
@Composable
fun AccountControlScreen(
    session: UserSession,
    repository: ChatRepository,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 头像：选中即写入本次会话（无跨平台 PNG 编解码，重启回退首字母）
    val pickAvatar = rememberImagePicker(onResult = { bitmap -> if (bitmap != null) session.avatar = bitmap })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = "账号控制", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionTitle("头像")
            AvatarCard(session = session, onClick = pickAvatar)

            SectionTitle("账号安全")
            SettingsCard {
                ActionRow(Res.drawable.user, "修改用户名", current = session.username) { showUsernameDialog = true }
                SettingRowDivider()
                ActionRow(Res.drawable.change_password, "修改密码") { showPasswordDialog = true }
                SettingRowDivider()
                ActionRow(Res.drawable.delete_account, "注销账号", danger = true) { showDeleteDialog = true }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showUsernameDialog) {
        UsernameDialog(
            currentUsername = session.username,
            onDismiss = { showUsernameDialog = false },
            onSubmit = { newName, reportResult ->
                scope.launch {
                    val result = AuthApi.changeUsername(session.token, newName)
                    when (result) {
                        is com.xlin.travel.login.MutationApiResult.Success -> {
                            result.user?.let { session.updateUser(it, repository) }
                            reportResult(null)   // 成功：关闭弹窗
                        }
                        is com.xlin.travel.login.MutationApiResult.Failed -> reportResult(result.message)
                    }
                }
            },
        )
    }

    if (showPasswordDialog) {
        PasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSubmit = { oldPwd, newPwd, reportResult ->
                scope.launch {
                    val result = AuthApi.changePassword(session.token, oldPwd, newPwd)
                    when (result) {
                        is com.xlin.travel.login.MutationApiResult.Success -> reportResult(null)
                        is com.xlin.travel.login.MutationApiResult.Failed -> reportResult(result.message)
                    }
                }
            },
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                scope.launch {
                    // 先清本地全部数据，再尽力请求服务端注销，最后复位会话并回到未登入界面
                    val token = session.token
                    session.erase()
                    repository.clearAllLocalData()
                    runCatching { AuthApi.deleteAccount(token) }
                    onAccountDeleted()
                }
            },
        )
    }
}

/** 头像卡片：圆形头像（有图显示图，无图显示用户名首字母）+ “点击更换头像”提示 */
@Composable
private fun AvatarCard(session: UserSession, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SettingCardBackground)
            .background(if (pressed) SettingItemPressed else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AvatarBackground),
            contentAlignment = Alignment.Center,
        ) {
            val avatar = session.avatar
            if (avatar != null) {
                Image(
                    bitmap = avatar,
                    contentDescription = "头像",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = session.avatarInitial,
                    color = TextGray300,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(text = "点击更换头像", color = TextWhite, fontSize = 16.sp)
    }
}

/** 账号操作条目：图标 + 标题 + 可选当前值 / 箭头；danger 时整体染红 */
@Composable
private fun ActionRow(
    icon: DrawableResource,
    label: String,
    current: String? = null,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tint = if (danger) FormErrorText else AccentOrange
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (pressed) SettingItemPressed else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            color = if (danger) FormErrorText else TextWhite,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        if (current != null) {
            Text(text = current, color = SettingChevron, fontSize = 14.sp)
        }
        Spacer(Modifier.width(4.dp))
        Image(
            painter = painterResource(Res.drawable.chevron_right),
            contentDescription = null,
            colorFilter = ColorFilter.tint(SettingChevron),
            modifier = Modifier.size(20.dp),
        )
    }
}

