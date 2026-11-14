package com.xlin.travel.chat_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ToastBackground
import com.xlin.travel.color.ToastText
import kotlinx.coroutines.delay

/** 顶部提示的状态：同一时间只显示一条，出现后自动消失 */
@Stable
class ChatToastState {

    /** 当前提示文字，null 表示不显示 */
    var text: String? by mutableStateOf(null)
        private set

    /** 每次 show 自增，作为重新计时和重播入场动画的 key */
    var generation: Long by mutableStateOf(0L)
        private set

    fun show(message: String) {
        text = message
        generation++
    }

    fun hide() {
        text = null
    }
}

/**
 * 顶部居中提示胶囊：从顶部滑入，文字消失前停留 [ToastVisibleMillis]（长文案则按阅读时间放宽）。
 * 放在聊天区根 Box 的最上层，不拦截点击以外的内容交互。
 */
@Composable
fun TopToast(
    state: ChatToastState,
    modifier: Modifier = Modifier,
) {
    val message = state.text.orEmpty()
    LaunchedEffect(state.generation) {
        if (state.generation > 0) {
            delay(visibleMillisFor(message))
            state.hide()
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = state.text != null,
            enter = slideInVertically(animationSpec = tween(180)) { -it } + fadeIn(),
            exit = slideOutVertically(animationSpec = tween(180)) { -it } + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = ToastTopGap)
                    // 长文案（如后端停维说明）限宽后自动换行，不会撑出屏幕
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ToastBackground)
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(
                    text = message,
                    color = ToastText,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                )
            }
        }
    }
}

/** 停留时长：短提示按固定时长，超过阈值的长文案多给一点阅读时间 */
private fun visibleMillisFor(message: String): Long =
    if (message.length > LongToastCharThreshold) LongToastVisibleMillis else ToastVisibleMillis

private const val ToastVisibleMillis = 1800L
private const val LongToastCharThreshold = 40
private const val LongToastVisibleMillis = 6000L
private val ToastTopGap = 56.dp
