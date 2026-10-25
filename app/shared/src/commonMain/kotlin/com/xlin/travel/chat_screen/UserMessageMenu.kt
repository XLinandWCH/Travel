package com.xlin.travel.chat_screen

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.TextWhite
import com.xlin.travel.color.TextGray300
import com.xlin.travel.drawer.ActionDialog
import com.xlin.travel.drawer.DialogButtons
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

/**
 * 长按用户消息的上下文菜单：与抽屉历史项长按「编辑名称 / 删除」同一形态（DropdownMenu），
 * 浮现在消息旁而不是从底部滑出，锚点由 [UserMessageBubble] 提供（贴气泡右下角）。
 * 最新一条用户消息才有「编辑」；「删除」染红。
 */
@Composable
fun UserMessageContextMenu(
    expanded: Boolean,
    canEdit: Boolean,
    onDismissRequest: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = SettingCardBackground,
        modifier = Modifier.width(140.dp),
    ) {
        if (canEdit) {
            DropdownMenuItem(
                text = { Text("编辑", color = TextWhite, fontSize = 15.sp) },
                onClick = onEdit,
            )
        }
        DropdownMenuItem(
            text = { Text("复制", color = TextWhite, fontSize = 15.sp) },
            onClick = onCopy,
        )
        DropdownMenuItem(
            text = { Text("删除", color = FormErrorText, fontSize = 15.sp) },
            onClick = onDelete,
        )
    }
}

/**
 * 删除消息确认弹窗：与历史会话删除同款式（取消 / 删除，删除染红），
 * 点菜单里的「删除」不会立即删除，确认后才执行。
 * 删除以「一问一答」为一组，因此文案里明说会连带删掉 AI 回答。
 */
@Composable
fun ConfirmDeleteMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ActionDialog(onDismiss = onDismiss) {
        Text(
            text = "删除消息",
            color = TextWhite,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Text(
            text = "确定要删除这条消息吗？对应的 AI 回答会一起删除，且无法恢复。",
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
