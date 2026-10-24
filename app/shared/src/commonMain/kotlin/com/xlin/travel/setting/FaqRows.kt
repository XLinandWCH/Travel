package com.xlin.travel.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.AssistantTextColor
import com.xlin.travel.color.SettingDivider
import com.xlin.travel.color.SettingValueText
import com.xlin.travel.color.TextWhite
import org.jetbrains.compose.resources.painterResource

/** FAQ 条目之间的分隔线 */
@Composable
internal fun FaqDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(1.dp)
            .background(SettingDivider),
    )
}

/**
 * 一条常见问题：图标 + 问题 + 展开 / 收起，展开后显示答案，
 * 需要外发的信息（联系邮箱）在答案下方附「复制」按钮。
 */
@Composable
internal fun FaqRow(
    item: FaqItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCopy: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp)
            .padding(vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(item.icon),
                contentDescription = item.question,
                colorFilter = ColorFilter.tint(AccentOrange),
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = item.question,
                color = TextWhite,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (expanded) "收起" else "展开",
                color = SettingValueText,
                fontSize = 13.sp,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    text = item.answer,
                    color = AssistantTextColor,
                    fontSize = 13.5.sp,
                    lineHeight = 21.sp,
                )
                val copyValue = item.copyValue
                if (copyValue != null) CopyRow(value = copyValue, onCopy = onCopy)
            }
        }
    }
}

/** 邮箱原文 + 复制按钮（复制结果由页面顶部提示反馈） */
@Composable
private fun CopyRow(value: String, onCopy: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            color = SettingValueText,
            fontSize = 13.5.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "复制",
            color = AccentOrange,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(onClick = { onCopy(value) })
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
