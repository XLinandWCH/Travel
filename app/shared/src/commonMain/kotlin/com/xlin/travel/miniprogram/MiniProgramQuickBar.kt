package com.xlin.travel.miniprogram

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.InputCardBackground
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.color.TextWhite
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.mini_menu

/**
 * 输入框上方的小程序快捷条，对应 UI设计/小程序选项UI参考.html 的 .quick-container：
 * 一行分左右两栏 —— 左侧 [LazyRow] 用 weight(1f) 撑满剩余宽度、横向滚动展示常用小程序胶囊，
 * 右侧固定一个圆形菜单按钮（mini_menu.xml，深灰填充需按主题染色），
 * 点它弹出 [MiniProgramPanel] 展示全部小程序。
 * 点任一胶囊或面板条目都回调 [onOpenProgram]，由上层跳到该小程序的「开发中」占位页。
 */
@Composable
fun MiniProgramQuickBar(
    onOpenProgram: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var panelVisible by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左：横滑快捷胶囊，吃掉整行剩余宽度
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        ) {
            items(MiniProgramCatalog.QuickNames) { name ->
                QuickChip(text = name, onClick = { onOpenProgram(name) })
            }
        }
        // 右：剩下的位置留给固定的圆形菜单按钮
        Spacer(Modifier.size(12.dp))
        MenuButton(onClick = { panelVisible = true })
    }

    MiniProgramPanel(
        visible = panelVisible,
        onDismiss = { panelVisible = false },
        onOpenProgram = onOpenProgram,
    )
}

/** 横向滚动的快捷胶囊（.quick-item）：卡片底色 + 全圆角，按压时叠加淡色反馈 */
@Composable
private fun QuickChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(if (pressed) SettingItemPressed else InputCardBackground)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = TextWhite, fontSize = 15.sp, maxLines = 1)
    }
}

/** 圆形菜单按钮（.menu-btn）：常驻显示在快捷条右端 */
@Composable
private fun MenuButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(InputCardBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.mini_menu),
            contentDescription = "全部小程序",
            // mini_menu.xml 自身是深灰填充，浅色主题下需反色才能在浅底上看见
            colorFilter = ColorFilter.tint(TextWhite),
            modifier = Modifier.size(20.dp),
        )
    }
}
