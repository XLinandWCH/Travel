package com.xlin.travel.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.navigation.PageTopBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.delete

/**
 * 历史管理子界面：目前仅一项「清空历史」。
 * 点击后弹出二次确认弹窗（样式同「注销账号」，但不带倒计时），
 * 确认后清空本地 SQLite 的全部会话与消息（保留主题 / 登录等设置），
 * 并通过 [onHistoryCleared] 让外层复位当前聊天、刷新抽屉列表。
 */
@Composable
fun HistoryManageScreen(
    repository: ChatRepository,
    onHistoryCleared: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = "历史管理", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionTitle("聊天记录")
            SettingsCard {
                DangerRow(
                    icon = Res.drawable.delete,
                    label = "清空历史",
                    onClick = { showClearDialog = true },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = {
                showClearDialog = false
                scope.launch {
                    runCatching { repository.clearChatHistory() }
                    onHistoryCleared()
                }
            },
        )
    }
}

/** 危险操作条目：图标染红 + 白字标题 + 右箭头，复用设置卡片样式 */
@Composable
private fun DangerRow(
    icon: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
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
            colorFilter = ColorFilter.tint(FormErrorText),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            color = FormErrorText,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 清空历史二次确认弹窗：样式对齐「注销账号」，但确认按钮立即可点、无倒计时。
 */
@Composable
private fun ClearHistoryDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(com.xlin.travel.color.SettingCardBackground)
                .padding(20.dp),
        ) {
            Text(
                text = "清空历史",
                color = FormErrorText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "清空历史将永久删除本地的全部对话记录，且无法恢复。账号与设置不受影响。确定要继续吗？",
                color = TextGray300,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "取消",
                    color = TextGray500,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "确认清空",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(FormErrorText)
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}
