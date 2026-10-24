package com.xlin.travel.sheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.SearchScrim
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.color.SheetBackground
import com.xlin.travel.color.SheetHandle
import com.xlin.travel.color.TextGray400
import com.xlin.travel.color.TextWhite
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.close_path

/**
 * 通用底部面板（bottom sheet）：直接使用 Material3 的 ModalBottomSheet，
 * 因此它是独立的窗口层 —— 自带遮罩、支持按住顶部下滑关闭、返回键优先关闭面板、
 * 并自动处理状态栏 / 导航栏 / 软键盘 inset。
 *
 * 关闭动画由 sheetState 播放，动画结束后才把面板移出组合（避免"闪没"）。
 * 所有会滑出的面板都走这里，不再自己手搓动画层。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        // 只有"完全展开 / 收起"两档，下滑即关闭
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    var rendered by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            rendered = true
        } else if (rendered) {
            sheetState.hide()
            rendered = false
        }
    }
    if (!rendered) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // 软键盘弹出时整个面板上移，输入框不会被键盘遮挡
        modifier = Modifier.imePadding(),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = SheetBackground,
        contentColor = TextWhite,
        tonalElevation = 0.dp,
        scrimColor = SearchScrim,
        dragHandle = { SheetDragHandle() },
    ) {
        SheetHeader(title = title, onClose = onDismiss)
        content()
    }
}

/** 顶部拖拽条：按住它下滑即可关闭面板（手势由 ModalBottomSheet 提供） */
@Composable
fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SheetHandle)
        )
    }
}

/**
 * 面板标题栏（位于拖拽条下方）：标题 + 关闭按钮
 */
@Composable
internal fun SheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = TextWhite,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SettingItemPressed)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.close_path),
                contentDescription = "关闭",
                colorFilter = ColorFilter.tint(TextGray400),
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
