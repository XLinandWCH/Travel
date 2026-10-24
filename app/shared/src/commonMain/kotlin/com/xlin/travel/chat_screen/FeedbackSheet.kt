package com.xlin.travel.chat_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ActionIconColor
import com.xlin.travel.color.SearchScrim
import com.xlin.travel.color.SheetBackground
import com.xlin.travel.color.SheetDivider
import com.xlin.travel.color.SheetHandle
import com.xlin.travel.color.TextWhite
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.close_path

/**
 * 底部反馈面板，1:1 还原 UI设计/底部动作面板.html：
 * 遮罩 + 从底部滑入的圆角面板（拖拽条 / 标题栏 / 三组多选标签 / 补充输入 / 提交按钮）。
 * 点击遮罩或右上角关闭图标收起面板，标签与补充内容都为空时提交按钮不可用。
 */
@Composable
fun FeedbackSheetOverlay(
    state: FeedbackSheetState,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = state.visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SearchScrim)
                    .clickable(onClick = { state.dismiss() }),
            )
        }

        AnimatedVisibility(
            visible = state.visible,
            enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = tween(180)) { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SheetContent(state = state, onSubmit = onSubmit)
        }
    }
}

@Composable
private fun SheetContent(state: FeedbackSheetState, onSubmit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(SheetBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 顶部拖拽条（仅装饰，面板不支持手势关闭，点遮罩即可收起）
        Box(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 8.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SheetHandle),
        )

        SheetHeader(onClose = { state.dismiss() })

        Column(
            modifier = Modifier
                .heightIn(max = SheetBodyMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
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
            NoteField(state)
        }

        SubmitButton(enabled = state.canSubmit, onClick = onSubmit)
    }
}

/** 标题栏 + 底部 1dp 分隔线 */
@Composable
private fun SheetHeader(onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = FeedbackOptions.Title,
                color = TextWhite,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.close_path),
                    contentDescription = "关闭",
                    colorFilter = ColorFilter.tint(ActionIconColor),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SheetDivider),
        )
    }
}

private val SheetBodyMaxHeight = 380.dp
