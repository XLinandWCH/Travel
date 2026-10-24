package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.login.FormTextField
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.change_password
import travel.app.shared.generated.resources.safe
import travel.app.shared.generated.resources.user

/**
 * 账号控制页的两个编辑弹窗：修改用户名 / 修改密码。
 * 弹窗只负责收集输入与本地校验，网络提交交给外部 [onSubmit]，
 * onSubmit 通过回调回报结果：传 null 表示成功并关闭弹窗，传字符串表示服务端错误提示。
 */

/** 修改用户名弹窗 */
@Composable
fun UsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onSubmit: (newUsername: String, reportResult: (String?) -> Unit) -> Unit,
) {
    var text by remember { mutableStateOf(currentUsername) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    AccountDialog(onDismiss = { if (!submitting) onDismiss() }) {
        DialogTitle("修改用户名")
        FormTextField(
            value = text,
            onValueChange = { text = it; error = null },
            icon = Res.drawable.user,
            placeholder = "请输入新的用户名",
            errorText = error,
        )
        Spacer(Modifier.height(20.dp))
        DialogButtons(
            confirmText = if (submitting) "保存中…" else "保存",
            confirmEnabled = !submitting,
            onCancel = onDismiss,
            onConfirm = {
                val name = text.trim()
                error = when {
                    name.isBlank() -> "用户名不能为空"
                    name.length !in 2..32 -> "用户名长度 2~32 位"
                    else -> null
                }
                if (error == null) {
                    submitting = true
                    onSubmit(name) { message ->
                        submitting = false
                        if (message == null) onDismiss() else error = message
                    }
                }
            },
        )
    }
}

/** 修改密码弹窗：原密码 + 新密码 + 确认新密码，底部提示 14 天冷却规则 */
@Composable
fun PasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (oldPassword: String, newPassword: String, reportResult: (String?) -> Unit) -> Unit,
) {
    var oldPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    AccountDialog(onDismiss = { if (!submitting) onDismiss() }) {
        DialogTitle("修改密码")
        FormTextField(
            value = oldPwd,
            onValueChange = { oldPwd = it; error = null },
            icon = Res.drawable.safe,
            placeholder = "请输入原密码",
            isPassword = true,
        )
        Spacer(Modifier.height(12.dp))
        FormTextField(
            value = newPwd,
            onValueChange = { newPwd = it; error = null },
            icon = Res.drawable.change_password,
            placeholder = "请输入新密码",
            isPassword = true,
        )
        Spacer(Modifier.height(12.dp))
        FormTextField(
            value = confirmPwd,
            onValueChange = { confirmPwd = it; error = null },
            icon = Res.drawable.change_password,
            placeholder = "请再次输入新密码",
            isPassword = true,
            errorText = error,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "为账号安全，两次修改密码需间隔 14 天",
            color = TextGray500,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(18.dp))
        DialogButtons(
            confirmText = if (submitting) "提交中…" else "确认修改",
            confirmEnabled = !submitting,
            onCancel = onDismiss,
            onConfirm = {
                error = when {
                    oldPwd.isBlank() -> "请输入原密码"
                    newPwd.length !in 6..32 ||
                        !newPwd.any { it.isLetter() } || !newPwd.any { it.isDigit() } ->
                        "新密码 6~32 位，需含字母和数字"
                    confirmPwd != newPwd -> "两次输入的密码不一致"
                    else -> null
                }
                if (error == null) {
                    submitting = true
                    onSubmit(oldPwd, newPwd) { message ->
                        submitting = false
                        if (message == null) onDismiss() else error = message
                    }
                }
            },
        )
    }
}

// ==================== 弹窗共用外壳与部件 ====================

/** 弹窗卡片容器：圆角卡片，标题之上、按钮之下由 content 填充 */
@Composable
private fun AccountDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(com.xlin.travel.color.SettingCardBackground)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}

@Composable
private fun DialogTitle(text: String) {
    Text(
        text = text,
        color = TextWhite,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

/** 右下按钮组：取消（灰字）+ 确认（强调橙填充） */
@Composable
private fun DialogButtons(
    confirmText: String,
    confirmEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "取消",
            color = TextGray500,
            fontSize = 15.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = confirmEnabled, onClick = onCancel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Spacer(Modifier.height(0.dp))
        Text(
            text = confirmText,
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .alpha(if (confirmEnabled) 1f else 0.6f)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentOrange)
                .clickable(enabled = confirmEnabled, onClick = onConfirm)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
