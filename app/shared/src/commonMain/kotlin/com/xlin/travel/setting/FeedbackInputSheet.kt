package com.xlin.travel.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlin.travel.sheet.MultilineInputField
import com.xlin.travel.sheet.SheetSubmitButton
import com.xlin.travel.sheet.TravelBottomSheet

/**
 * 意见反馈面板：一个多行输入框 + 底部提交按钮（软键盘弹出时面板整体上移）。
 * 内容为空时提交按钮禁用；提交动作与提示语由调用方（[SettingsSheetHost]）处理。
 */
@Composable
internal fun FeedbackInputSheet(
    state: SettingsSheetState,
    onSubmit: () -> Unit,
) {
    TravelBottomSheet(
        visible = state.open == SettingsSheet.Feedback,
        onDismiss = { state.dismiss() },
        title = "意见反馈",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
        ) {
            MultilineInputField(
                value = state.draft,
                onValueChange = { state.onDraftChange(it) },
                placeholder = HelpFaqs.FeedbackPlaceholder,
                minHeight = 160.dp,
                maxHeight = 300.dp,
            )
        }

        SheetSubmitButton(enabled = state.canSubmitFeedback, onClick = onSubmit)
    }
}
