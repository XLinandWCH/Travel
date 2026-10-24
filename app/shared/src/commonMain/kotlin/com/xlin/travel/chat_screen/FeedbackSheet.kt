package com.xlin.travel.chat_screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlin.travel.sheet.SheetSubmitButton
import com.xlin.travel.sheet.TravelBottomSheet

/**
 * 点踩后的反馈面板：真正的底部面板（Material3 ModalBottomSheet，自带遮罩、可下滑拖拽关闭）。
 * 标签与补充内容都为空时提交按钮不可用。
 */
@Composable
fun FeedbackBottomSheet(
    state: FeedbackSheetState,
    onSubmit: (String) -> Unit,
) {
    TravelBottomSheet(
        visible = state.visible,
        onDismiss = { state.dismiss() },
        title = FeedbackOptions.Title,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = FeedbackBodyMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            FeedbackOptions.groups.forEach { (title, options) ->
                ChipGroup(
                    title = title,
                    options = options,
                    picked = state.picked,
                    onToggle = { state.toggle(it) },
                )
            }
            SectionTitle(FeedbackOptions.NoteTitle)
            NoteField(state = state)
        }
        SheetSubmitButton(
            enabled = state.canSubmit,
            onClick = { onSubmit(state.submit()) },
        )
    }
}

/** 面板正文最大高度，超出后面板内部滚动（提交按钮始终可见） */
private val FeedbackBodyMaxHeight = 380.dp
