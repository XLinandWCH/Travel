package com.xlin.travel.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AvatarBackground
import com.xlin.travel.color.IconButtonBackground
import com.xlin.travel.color.MenuButtonBackground
import com.xlin.travel.color.NewChatButton
import com.xlin.travel.color.SidebarBackground
import com.xlin.travel.color.SidebarDivider
import com.xlin.travel.color.SidebarItemActive
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.search
import travel.app.shared.generated.resources.setting

/**
 * 顶部 New chat + 搜索、中部历史记录、底部用户 + 设置。
 */
@Composable
fun ChatDrawer(
    history: ChatHistoryState,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        history.newChat()
                        onCloseDrawer()
                    },
                )
                Spacer(Modifier.width(8.dp))
                CircleIconButton(
                    painter = painterResource(Res.drawable.search),
                    contentDescription = "搜索",
                    background = IconButtonBackground,
                    onClick = { history.openSearch() },
                )
            }

            // 中部：历史记录
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
                    HistoryRow(
                        item = item,
                        selected = item.id == history.activeId,
                        onClick = {
                            history.select(item.id)
                            onCloseDrawer()
                        },
                    )
                }
            }

            // 底部：用户 + 设置
            HorizontalDivider(color = SidebarDivider)
            UserFooter()
        }
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
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
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
    background: androidx.compose.ui.graphics.Color,
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

/** 单条历史记录 */
@Composable
private fun HistoryRow(item: ChatHistoryItem, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) SidebarItemActive else androidx.compose.ui.graphics.Color.Transparent
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
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

/** 底部用户信息与设置按钮 */
@Composable
private fun UserFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            Text("用", color = TextGray300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "用户",
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
                .background(MenuButtonBackground)
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(Res.drawable.setting), contentDescription = "设置", modifier = Modifier.size(18.dp))
        }
    }
}
