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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ChipBorder
import com.xlin.travel.color.ChipSelectedBackground
import com.xlin.travel.color.ChipSelectedText
import com.xlin.travel.color.FeedbackAccent
import com.xlin.travel.color.TextWhite
import com.xlin.travel.sheet.MultilineInputField

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

/** 多行补充输入框（复用底部面板的通用输入件），占位文字在内容为空时显示 */
@Composable
internal fun NoteField(state: FeedbackSheetState) {
    MultilineInputField(
        value = state.note,
        onValueChange = { state.onNoteChange(it) },
        placeholder = FeedbackOptions.NotePlaceholder,
    )
    Box(modifier = Modifier.height(6.dp))
}
