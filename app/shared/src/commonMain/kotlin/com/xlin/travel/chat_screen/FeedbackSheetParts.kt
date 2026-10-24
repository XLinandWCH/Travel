package com.xlin.travel.chat_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChipBorder
import com.xlin.travel.color.ChipSelectedBackground
import com.xlin.travel.color.ChipSelectedText
import com.xlin.travel.color.FeedbackAccent
import com.xlin.travel.color.InputCardBackground
import com.xlin.travel.color.InputPlaceholderColor
import com.xlin.travel.color.InputTextColor
import com.xlin.travel.color.SubmitDisabledBackground
import com.xlin.travel.color.SubmitDisabledText
import com.xlin.travel.color.SubmitReadyBackground
import com.xlin.travel.color.SubmitReadyText
import com.xlin.travel.color.TextWhite

/** 分组标题 + 一组可多选的标签 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(
    title: String,
    options: List<String>,
    picked: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp)) {
        SectionTitle(title)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            options.forEach { label ->
                Chip(text = label, selected = label in picked) { onToggle(label) }
            }
        }
    }
}

/** 标签：默认描边透明底，选中后用主题蓝描边 + 蓝色半透明底 */
@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) ChipSelectedBackground else Color.Transparent)
            .border(width = 1.dp, color = if (selected) FeedbackAccent else ChipBorder, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
    ) {
        Text(
            text = text,
            color = if (selected) ChipSelectedText else TextWhite,
            fontSize = 14.5.sp,
        )
    }
}

/** 区块标题（针对问题 / 针对回答 / 针对格式 / 我要补充） */
@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TextWhite,
        fontSize = 16.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 14.dp),
    )
}

/** 多行补充输入框，占位文字在内容为空时显示 */
@Composable
internal fun NoteField(state: FeedbackSheetState) {
    val shape = RoundedCornerShape(15.dp)
    BasicTextField(
        value = state.note,
        onValueChange = { state.onNoteChange(it) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 190.dp)
            .clip(shape)
            .background(InputCardBackground)
            .border(1.dp, ChipBorder, shape)
            .padding(14.dp),
        textStyle = TextStyle(color = InputTextColor, fontSize = 15.sp, lineHeight = 25.sp),
        cursorBrush = SolidColor(AccentOrange),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.TopStart) {
                if (state.note.isEmpty()) {
                    Text(FeedbackOptions.NotePlaceholder, color = InputPlaceholderColor, fontSize = 15.sp)
                }
                inner()
            }
        },
    )
    Box(modifier = Modifier.height(6.dp))
}

/** 提交按钮：可提交时高亮，否则保持禁用灰 */
@Composable
internal fun SubmitButton(enabled: Boolean, onClick: () -> Unit) {
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
            text = "提交",
            color = if (enabled) SubmitReadyText else SubmitDisabledText,
            fontSize = 16.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 15.dp),
        )
    }
}
