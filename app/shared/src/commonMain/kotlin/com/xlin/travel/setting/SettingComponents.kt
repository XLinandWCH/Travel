package com.xlin.travel.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.SettingChevron
import com.xlin.travel.color.SettingDivider
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.color.SettingValueText
import com.xlin.travel.color.TextWhite
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.chevron_right

/**
 * 设置界面共用组件，尺寸 / 配色 1:1 对应 UI设计/设置界面UI初稿.html：
 * 卡片 16dp 圆角、条目最小高 56dp、左右内边距 16dp、图标 24dp 染强调橙。
 */

/** 分组标题（.section-title）：15sp 中等字重，上 16 / 下 8 / 左缩进 4 */
@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TextWhite,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

/** 圆角分组卡片（.card）：条目纵向排列，内部自带裁切 */
@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SettingCardBackground),
        content = content,
    )
}

/** 卡片内条目分隔线（.item + .item::before）：白 5%，左右缩进 16dp */
@Composable
internal fun SettingRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(SettingDivider),
    )
}

/**
 * 通用设置条目（.item）：图标 + 标题 + 可选右侧数值 / 选中勾 / Chevron Right 箭头。
 * 按下反馈对应 .item:active（白 4% 背景），不使用水波纹。
 *
 * @param onClick 为 null 时条目仍保留按下反馈，但不触发动作
 */
@Composable
internal fun SettingRow(
    icon: DrawableResource,
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    selected: Boolean = false,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (pressed) SettingItemPressed else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(AccentOrange),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            color = TextWhite,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                color = SettingValueText,
                fontSize = 15.sp,
            )
        }
        if (selected) {
            Text(
                text = "✓",
                color = AccentOrange,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (showChevron) {
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource(Res.drawable.chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(SettingChevron),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
