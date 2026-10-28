package com.xlin.travel.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.SubmitReadyBackground
import com.xlin.travel.color.SubmitReadyText
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextWhite
import com.xlin.travel.drawer.ActionDialog
import com.xlin.travel.drawer.DialogButtons
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.add_file

/**
 * 消息中心右下角悬浮「+」圆形按钮：用户反馈 / 公告两个页签共用一枚。
 * 配色取双主题反转的提交按钮色（深主题白底黑字 / 浅主题黑底白字），图标染成对应文字色，
 * 黑白系统配色下始终与背景形成对比。具体显隐与点击行为由调用方按角色 / 页签控制。
 */
@Composable
fun MessageAddFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(SubmitReadyBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.add_file),
            contentDescription = "添加",
            colorFilter = ColorFilter.tint(SubmitReadyText),
            modifier = Modifier.size(26.dp),
        )
    }
}

/**
 * 删除公告确认弹窗：与删除消息同款式（取消 / 删除，删除染红），确认后才真正调用删除接口。
 */
@Composable
fun ConfirmDeleteBroadcastDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ActionDialog(onDismiss = onDismiss) {
        Text(
            text = "删除公告",
            color = TextWhite,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Text(
            text = "确定要删除这条公告吗？删除后所有用户都将不再看到，且无法恢复。",
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
