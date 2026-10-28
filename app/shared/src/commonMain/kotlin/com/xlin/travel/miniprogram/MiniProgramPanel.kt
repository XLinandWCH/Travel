package com.xlin.travel.miniprogram

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.color.TextGray300
import com.xlin.travel.sheet.TravelBottomSheet
import org.jetbrains.compose.resources.painterResource

/** 面板网格列数，对应 HTML .panel-row 的 grid-template-columns: repeat(4, 1fr) */
private const val MiniProgramColumns = 4

/**
 * 点击快捷条右端圆形菜单按钮后滑出的全部小程序面板（HTML .panel）：
 * 复用项目通用的 [TravelBottomSheet]（自带遮罩 / 下滑关闭 / 返回键优先关闭），
 * 内部按 4 列网格逐行排布，每格为「图标在上、名称在下」的一体条目，格与格之间不再有分隔线。
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
        MiniProgramCatalog.All.chunked(MiniProgramColumns).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                row.forEach { program ->
                    MiniProgramCell(program = program, modifier = Modifier.weight(1f)) {
                        onDismiss()
                        onOpenProgram(program.name)
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

/**
 * 面板网格条目（HTML .item）：图标为主 + 名称为辅的一体卡片。
 * 无描边、无多余线条：底色透明，仅按压时叠一层极淡反馈；图标染成主题文字色。
 */
@Composable
private fun MiniProgramCell(
    program: MiniProgram,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) SettingItemPressed else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(program.icon),
            contentDescription = program.name,
            // 图标自身为深色填充，染成白色才能在深浅主题下都看清
            colorFilter = ColorFilter.tint(TextGray300),
            modifier = Modifier.size(30.dp),
        )
        Text(
            text = program.name,
            color = TextGray300,
            fontSize = 12.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
