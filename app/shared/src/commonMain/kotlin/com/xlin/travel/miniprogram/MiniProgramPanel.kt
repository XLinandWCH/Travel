package com.xlin.travel.miniprogram

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.MiniPanelDivider
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.color.TextWhite
import com.xlin.travel.sheet.TravelBottomSheet

/** 面板网格列数，对应 HTML .panel-row 的 grid-template-columns: repeat(4, 1fr) */
private const val MiniProgramColumns = 4

/**
 * 点击快捷条右端圆形菜单按钮后滑出的全部小程序面板（HTML .panel）：
 * 复用项目通用的 [TravelBottomSheet]（自带遮罩 / 下滑关闭 / 返回键优先关闭），
 * 内部按 4 列网格排布，逐行之间用细分隔线隔开，呼应 HTML .panel-row 的 border-bottom。
 */
@Composable
internal fun MiniProgramPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenProgram: (String) -> Unit,
) {
    TravelBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "全部小程序",
    ) {
        MiniProgramCatalog.AllNames.chunked(MiniProgramColumns).forEachIndexed { index, row ->
            if (index > 0) PanelRowDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                row.forEach { name ->
                    MiniProgramCell(text = name, modifier = Modifier.weight(1f)) {
                        onDismiss()
                        onOpenProgram(name)
                    }
                }
                // 末行不足 4 个时补空位，保证每列宽度与网格一致
                repeat(MiniProgramColumns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** 面板网格条目（HTML .item）：无底色，仅居中文字 + 按压淡色反馈 */
@Composable
private fun MiniProgramCell(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) SettingItemPressed else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = TextWhite,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/** 面板行分隔线（HTML .panel-row 的 border-bottom） */
@Composable
private fun PanelRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MiniPanelDivider),
    )
}
