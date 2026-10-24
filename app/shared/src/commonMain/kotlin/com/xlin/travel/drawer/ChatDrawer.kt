package com.xlin.travel.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AvatarBackground
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.IconButtonBackground
import com.xlin.travel.color.MenuButtonBackground
import com.xlin.travel.color.NewChatButton
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.SidebarBackground
import com.xlin.travel.color.SidebarDivider
import com.xlin.travel.color.SidebarItemActive
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.login.UserSession
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.search
import travel.app.shared.generated.resources.setting

/**
 * 顶部 New chat + 搜索、中部历史记录、底部用户 + 设置。
 * 历史项长按弹出上下文菜单（编辑名称 / 删除）；底部展示登录用户的头像与用户名。
 */
@Composable
fun ChatDrawer(
    session: UserSession,
    history: ChatHistoryState,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 长按弹出的菜单归属哪一项；null 表示无菜单展开
    var contextItemId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<ChatHistoryItem?>(null) }
    var pendingRename by remember { mutableStateOf<ChatHistoryItem?>(null) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(SidebarBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
        ) {
            // 顶部：New chat + 搜索
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NewChatButtonRow(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onNewChat()
                        onCloseDrawer()
                    },
                )
                Spacer(Modifier.width(8.dp))
                CircleIconButton(
                    painter = painterResource(Res.drawable.search),
                    contentDescription = "搜索",
                    background = IconButtonBackground,
                    onClick = onOpenSearch,
                )
            }

            // 中部：历史记录（长按某项弹出操作菜单）
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                item {
                    Text(
                        text = "Older",
                        color = TextGray500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
                items(history.items, key = { it.id }) { item ->
                    val expanded = item.id == contextItemId
                    HistoryRow(
                        item = item,
                        selected = item.id == history.activeId,
                        menuExpanded = expanded,
                        onClick = {
                            onSelectConversation(item.id)
                            onCloseDrawer()
                        },
                        onLongClick = { contextItemId = item.id },
                        onDismissMenu = { contextItemId = null },
                        onRename = {
                            contextItemId = null
                            pendingRename = item
                        },
                        onDelete = {
                            contextItemId = null
                            pendingDelete = item
                        },
                    )
                }
                if (history.items.isEmpty()) {
                    item {
                        Text(
                            text = "暂无历史对话",
                            color = TextGray500,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                        )
                    }
                }
            }

            // 底部：用户 + 设置（整行即一个按钮）
            HorizontalDivider(color = SidebarDivider)
            UserSettingsButton(session = session, onClick = onOpenSettings)
        }
    }

    // 删除确认弹窗
    pendingDelete?.let { item ->
        ConfirmDeleteDialog(
            title = item.title,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                onDeleteConversation(item.id)
            },
        )
    }

    // 重命名弹窗
    pendingRename?.let { item ->
        RenameDialog(
            initialTitle = item.title,
            onDismiss = { pendingRename = null },
            onConfirm = { newTitle ->
                pendingRename = null
                onRenameConversation(item.id, newTitle)
            },
        )
    }
}

/** 橙色 New chat 主按钮 */
@Composable
private fun NewChatButtonRow(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(NewChatButton)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("+", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text("New chat", color = TextWhite, fontSize = 14.sp)
    }
}

/** 圆形图标按钮（搜索） */
@Composable
private fun CircleIconButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    background: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painter = painter, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
    }
}

/** 单条历史记录：长按弹出「编辑名称 / 删除」上下文菜单（纯文字，删除染红） */
@Composable
private fun HistoryRow(
    item: ChatHistoryItem,
    selected: Boolean,
    menuExpanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val background = if (selected) SidebarItemActive else Color.Transparent
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = item.title,
            color = if (selected) TextWhite else TextGray300,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(background)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = onDismissMenu,
            containerColor = SettingCardBackground,
            modifier = Modifier.width(160.dp),
        ) {
            DropdownMenuItem(
                text = { Text("编辑名称", color = TextWhite, fontSize = 15.sp) },
                onClick = onRename,
            )
            DropdownMenuItem(
                text = { Text("删除", color = FormErrorText, fontSize = 15.sp) },
                onClick = onDelete,
            )
        }
    }
}

/**
 * 底部用户信息与设置按钮：整行作为一个按钮，点击进入设置页。
 * 展示登录用户头像（无图回退用户名首字母）与用户名。
 */
@Composable
private fun UserSettingsButton(session: UserSession, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (pressed) SettingItemPressed else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
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
                Text(session.avatarInitial, color = TextGray300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = session.username.ifBlank { "用户" },
            color = TextWhite,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MenuButtonBackground),
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(Res.drawable.setting), contentDescription = "设置", modifier = Modifier.size(18.dp))
        }
    }
}
