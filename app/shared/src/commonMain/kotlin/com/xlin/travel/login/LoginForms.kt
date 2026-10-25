package com.xlin.travel.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.email
import travel.app.shared.generated.resources.phone_number
import travel.app.shared.generated.resources.safe
import travel.app.shared.generated.resources.user

/**
 * 登录 / 注册两个表单面板，字段与错误提示都绑定在 [LoginState] 上。
 * 图标沿用 drawable 目录：user / email / phone_number / safe。
 */

/** 登录表单：邮箱 / 手机号方式切换 + 账号 + 密码 */
@Composable
fun LoginForm(state: LoginState, modifier: Modifier = Modifier) {
    // 邮箱 / 手机号切换（小号分段，居中）
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        SegmentSwitch(
            options = listOf("邮箱登录", "手机号登录"),
            selectedIndex = if (state.accountType == AccountType.Email) 0 else 1,
            onSelect = { index ->
                state.switchAccountType(if (index == 0) AccountType.Email else AccountType.Phone)
            },
        )
    }
    Spacer(Modifier.height(16.dp))

    val isEmail = state.accountType == AccountType.Email
    FormTextField(
        value = state.account,
        onValueChange = { state.account = it; state.clearError("account") },
        icon = if (isEmail) Res.drawable.email else Res.drawable.phone_number,
        placeholder = if (isEmail) "请输入邮箱" else "请输入手机号",
        keyboardType = if (isEmail) KeyboardType.Email else KeyboardType.Phone,
        errorText = state.fieldError("account"),
    )
    Spacer(Modifier.height(14.dp))
    FormTextField(
        value = state.password,
        onValueChange = { state.password = it; state.clearError("password") },
        icon = Res.drawable.safe,
        placeholder = "请输入密码",
        isPassword = true,
        errorText = state.fieldError("password"),
    )
}

/** 注册表单：用户名 + 邮箱 + 手机号 + 密码 + 确认密码 */
@Composable
fun RegisterForm(state: LoginState, modifier: Modifier = Modifier) {
    Spacer(Modifier.height(2.dp))
    FormTextField(
        value = state.username,
        onValueChange = { state.username = it; state.clearError("username") },
        icon = Res.drawable.user,
        placeholder = "用户名",
        modifier = modifier,
        errorText = state.fieldError("username"),
    )
    Spacer(Modifier.height(14.dp))
    FormTextField(
        value = state.regEmail,
        onValueChange = { state.regEmail = it; state.clearError("email") },
        icon = Res.drawable.email,
        placeholder = "邮箱",
        keyboardType = KeyboardType.Email,
        errorText = state.fieldError("email"),
    )
    Spacer(Modifier.height(14.dp))
    FormTextField(
        value = state.regPhone,
        onValueChange = { state.regPhone = it; state.clearError("phone") },
        icon = Res.drawable.phone_number,
        placeholder = "手机号",
        keyboardType = KeyboardType.Phone,
        errorText = state.fieldError("phone"),
    )
    Spacer(Modifier.height(14.dp))
    FormTextField(
        value = state.regPassword,
        onValueChange = { state.regPassword = it; state.clearError("password") },
        icon = Res.drawable.safe,
        placeholder = "设置密码（6~32 位，含字母和数字）",
        isPassword = true,
        errorText = state.fieldError("password"),
    )
    Spacer(Modifier.height(14.dp))
    FormTextField(
        value = state.regPasswordAgain,
        onValueChange = { state.regPasswordAgain = it; state.clearError("passwordAgain") },
        icon = Res.drawable.safe,
        placeholder = "再次输入密码",
        isPassword = true,
        errorText = state.fieldError("passwordAgain"),
    )
}
