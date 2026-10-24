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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.xlin.travel.sheet.SheetSubmitButton
import com.xlin.travel.sheet.TravelBottomSheet
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/**
 * 帮助中心面板：基础 FAQ 手风琴（用户信息 / 数据安全 / 联系作者），
 * 点条目展开答案，联系作者一栏可直接复制邮箱；面板底部固定「意见反馈」入口。
 */
@Composable
internal fun HelpSheet(
    state: SettingsSheetState,
    onCopy: (String) -> Unit,
    onFeedback: () -> Unit,
) {
    // 展开项按问题标题记忆，收起面板后重置为全部收起
    val visible = state.open == SettingsSheet.Help
    var expanded by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(visible) {
        if (!visible) expanded = null
    }

    TravelBottomSheet(
        visible = visible,
        onDismiss = { state.dismiss() },
        title = "帮助中心",
    ) {
        // 高度随展开的内容增长，超过上限内部滚动，保证「意见反馈」按钮始终可见
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = HelpBodyMaxHeight)
                .verticalScroll(rememberScrollState()),
        ) {
            HelpFaqs.items.forEachIndexed { index, item ->
                if (index > 0) FaqDivider()
                FaqRow(
                    item = item,
                    expanded = expanded == item.question,
                    onToggle = { expanded = if (expanded == item.question) null else item.question },
                    onCopy = onCopy,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        SheetSubmitButton(enabled = true, text = "意见反馈", onClick = onFeedback)
    }
}

@Composable
private fun FaqDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(SettingDivider),
    )
}

@Composable
private fun FaqRow(
    item: FaqItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCopy: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 14.dp),
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

/**
 * 邮箱原文 + 复制按钮。
 * 面板是独立窗口，顶部的 Toast 会被它遮住，所以复制结果在面板内就地提示。
 */
@Composable
private fun CopyRow(value: String, onCopy: (String) -> Unit) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }
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
            text = if (copied) "已复制 ✓" else "复制",
            color = AccentOrange,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(onClick = { onCopy(value); copied = true })
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

private val HelpBodyMaxHeight = 380.dp
