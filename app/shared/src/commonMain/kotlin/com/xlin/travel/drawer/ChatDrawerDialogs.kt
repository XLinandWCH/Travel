package com.xlin.travel.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.InputCardBackground
import com.xlin.travel.color.InputPlaceholderColor
import com.xlin.travel.color.InputTextColor
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.SettingDivider
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite

/**
 * 历史项长按菜单触发的两个弹窗，均纯文字按钮、无图标：
 * - [ConfirmDeleteDialog]：询问是否删除，「取消 / 删除」，删除染红；
 * - [RenameDialog]：单行输入改名，「取消 / 完成」，完成用强调橙。
 * 配色统一取自 AppColors，跟随深浅主题。
 */

/** 删除会话确认弹窗 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ActionDialog(onDismiss = onDismiss) {
        Text(
            text = "删除对话",
            color = TextWhite,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Text(
            text = "确定要删除「$title」吗？删除后该对话的全部消息将无法恢复。",
            color = TextGray300,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(24.dp))
        DialogButtons(
            cancelText = "取消",
            confirmText = "删除",
            confirmColor = FormErrorText,
            confirmEnabled = true,
            onCancel = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

/** 重命名会话弹窗 */
@Composable
fun RenameDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialTitle) }
    val canSubmit = text.isNotBlank()

    ActionDialog(onDismiss = onDismiss) {
        Text(
            text = "重命名",
            color = TextWhite,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InputCardBackground)
                .border(1.dp, SettingDivider, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = InputTextColor, fontSize = 15.sp),
                cursorBrush = SolidColor(AccentOrange),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (canSubmit) onConfirm(text.trim()) },
                ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text("请输入新的名称", color = InputPlaceholderColor, fontSize = 15.sp)
                        }
                        inner()
                    }
                },
            )
        }
        Spacer(Modifier.height(22.dp))
        DialogButtons(
            cancelText = "取消",
            confirmText = "完成",
            confirmColor = AccentOrange,
            confirmEnabled = canSubmit,
            onCancel = onDismiss,
            onConfirm = { onConfirm(text.trim()) },
        )
    }
}

// ==================== 弹窗共用部件 ====================

/** 弹窗卡片外壳：圆角卡片，内容纵向排布（聊天消息删除确认弹窗也复用这套骨架） */
@Composable
internal fun ActionDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SettingCardBackground)
                .padding(20.dp),
            content = content,
        )
    }
}

/** 右下按钮组：取消（灰字）+ 动作（着色文字） */
@Composable
internal fun DialogButtons(
    cancelText: String,
    confirmText: String,
    confirmColor: Color,
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
            text = cancelText,
            color = TextGray500,
            fontSize = 15.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onCancel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Spacer(Modifier.height(0.dp))
        Text(
            text = confirmText,
            color = confirmColor,
            fontSize = 15.sp,
            modifier = Modifier
                .alpha(if (confirmEnabled) 1f else 0.4f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = confirmEnabled, onClick = onConfirm)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
