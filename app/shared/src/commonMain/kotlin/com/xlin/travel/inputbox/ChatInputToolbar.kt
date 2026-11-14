package com.xlin.travel.inputbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.BtnAddBorder
import com.xlin.travel.color.InputPlaceholderColor
import com.xlin.travel.color.InputTextColor
import com.xlin.travel.color.ModeTextColor
import com.xlin.travel.color.WhiteBtnBackground
import com.xlin.travel.color.WhiteBtnIcon
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.add_file
import travel.app.shared.generated.resources.microphone
import travel.app.shared.generated.resources.send
import travel.app.shared.generated.resources.stop

/**
 * 输入框中部与底部：文本输入行、工具栏（“+” / 视频讲解 / 动作按钮）。
 * 从 ChatInputBox 拆出，保证两个文件都在项目约定的 250 行以内。
 */

/** 中部输入行：文本输入（使用系统默认光标） */
@Composable
internal fun InputRow(state: ChatInputState, onSend: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // 点小程序快捷条插入「@名称」后把焦点交回输入框：键盘弹出、光标停在末尾，
    // 用户接着补一句需求即可发送（这里只聚焦，不自动发送）
    LaunchedEffect(state.focusRequestTick) {
        if (state.focusRequestTick > 0) focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier.heightIn(min = 36.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BasicTextField(
            value = state.text,
            onValueChange = { state.onTextChange(it) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 24.dp, max = 120.dp)
                .focusRequester(focusRequester),
            textStyle = TextStyle(color = InputTextColor, fontSize = 16.sp, lineHeight = 24.sp),
            cursorBrush = SolidColor(AccentOrange),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (state.hasText) {
                        onSend(state.text)
                        state.onSent()
                        // Compose Multiplatform 的 KeyboardActions 无 autoClose，手动失焦收起键盘
                        focusManager.clearFocus(force = true)
                    }
                },
            ),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (state.text.isEmpty()) {
                        Text("Ask anything", color = InputPlaceholderColor, fontSize = 16.sp)
                    }
                    inner()
                }
            },
        )
    }
}

/** 下部工具栏：左侧“+”与视频讲解入口，右侧动作按钮 */
@Composable
internal fun InputToolbar(
    state: ChatInputState,
    isStreaming: Boolean,
    onToggleAttachments: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onMic: () -> Unit,
    onVideoExplain: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // “+”按钮 → 展开 / 收起「拍照 / 相册 / 本地文件」动作面板
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, BtnAddBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleAttachments),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.add_file),
                    contentDescription = "添加附件",
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            // 视频讲解：不再是模式选择，点一下直接进入模拟通话页
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onVideoExplain)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("视频讲解", color = ModeTextColor, fontSize = 15.sp)
            }
        }

        // 右侧动作按钮：流式中→停止，有内容→发送，否则→麦克风语音输入
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WhiteBtnBackground)
                .clickable {
                    when {
                        isStreaming -> onStop()
                        state.showSend -> onSend()
                        else -> onMic()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val iconRes = when {
                isStreaming -> Res.drawable.stop
                state.showSend -> Res.drawable.send
                else -> Res.drawable.microphone
            }
            Image(
                painter = painterResource(iconRes),
                contentDescription = when {
                    isStreaming -> "停止生成"
                    state.showSend -> "发送"
                    else -> "语音输入"
                },
                // 图标源色为深色，叠加主题反转的 WhiteBtnIcon：深色主题白底配黑图标、
                // 浅色主题黑灰底配白图标，避免图标与背景同色看不见
                colorFilter = ColorFilter.tint(WhiteBtnIcon),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
