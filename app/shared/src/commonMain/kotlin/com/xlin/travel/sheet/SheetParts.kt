package com.xlin.travel.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChipBorder
import com.xlin.travel.color.InputCardBackground
import com.xlin.travel.color.InputPlaceholderColor
import com.xlin.travel.color.InputTextColor
import com.xlin.travel.color.SubmitDisabledBackground
import com.xlin.travel.color.SubmitDisabledText
import com.xlin.travel.color.SubmitReadyBackground
import com.xlin.travel.color.SubmitReadyText

/**
 * 底部面板里的多行输入框：卡片底 + 描边 + 占位文字，光标用主题强调橙。
 * 聊天反馈面板的「我要补充」与帮助中心的「意见反馈」共用。
 */
@Composable
fun MultilineInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 150.dp,
    maxHeight: Dp = 190.dp,
) {
    val shape = RoundedCornerShape(15.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            .clip(shape)
            .background(InputCardBackground)
            .border(1.dp, ChipBorder, shape)
            .padding(14.dp),
        textStyle = TextStyle(color = InputTextColor, fontSize = 15.sp, lineHeight = 25.sp),
        cursorBrush = SolidColor(AccentOrange),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.TopStart) {
                if (value.isEmpty()) {
                    Text(placeholder, color = InputPlaceholderColor, fontSize = 15.sp)
                }
                inner()
            }
        },
    )
}

/** 面板底部通栏提交按钮：内容可用时高亮，否则保持禁用灰 */
@Composable
fun SheetSubmitButton(
    enabled: Boolean,
    text: String = "提交",
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)
            .clip(shape)
            .background(if (enabled) SubmitReadyBackground else SubmitDisabledBackground)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) SubmitReadyText else SubmitDisabledText,
            fontSize = 16.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 15.dp),
        )
    }
}
