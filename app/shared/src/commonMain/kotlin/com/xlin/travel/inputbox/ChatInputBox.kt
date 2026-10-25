package com.xlin.travel.inputbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.BtnAddBorder
import com.xlin.travel.color.InputCardBackground
import com.xlin.travel.color.InputPlaceholderColor
import com.xlin.travel.color.InputTextColor
import com.xlin.travel.color.ModeTextColor
import com.xlin.travel.color.RemoveButtonBackground
import com.xlin.travel.color.WhiteBtnBackground
import com.xlin.travel.image_picker.rememberImagePicker
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.add_file
import travel.app.shared.generated.resources.close_path
import travel.app.shared.generated.resources.microphone
import travel.app.shared.generated.resources.send
import travel.app.shared.generated.resources.stop

/**
 * AI 聊天输入框卡片，对应 UI设计/输入框设计初稿.html
 * 结构：上部图片预览 + 中部输入行 + 下部工具栏
 */
@Composable
fun ChatInputBox(
    state: ChatInputState,
    onSend: (String) -> Unit = {},
    isStreaming: Boolean = false,
    onStop: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 系统图片选择器：选中结果（位图 + 本地路径）写入 state（取消为 null）
    val pickImage = rememberImagePicker(onResult = { bitmap, path -> state.onImagePicked(bitmap, path) })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(InputCardBackground)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
    ) {
        // 上：图片预览（真实图片）
        state.pickedImage?.let { image ->
            ImagePreview(image = image, onRemove = { state.onImageRemoved() })
            Spacer(Modifier.height(10.dp))
        }

        // 中：输入行
        InputRow(state = state, onSend = onSend)

        Spacer(Modifier.height(8.dp))

        // 下：工具栏
        Toolbar(
            state = state,
            isStreaming = isStreaming,
            onAddImage = pickImage,
            onSend = { if (state.showSend) { onSend(state.text); state.onSent() } },
            onStop = onStop,
        )
    }
}

/** 中部输入行：文本输入（使用系统默认光标） */
@Composable
private fun InputRow(state: ChatInputState, onSend: (String) -> Unit) {
    Row(
        modifier = Modifier.heightIn(min = 36.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BasicTextField(
            value = state.text,
            onValueChange = { state.onTextChange(it) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 24.dp, max = 120.dp),
            textStyle = TextStyle(color = InputTextColor, fontSize = 16.sp, lineHeight = 24.sp),
            cursorBrush = SolidColor(AccentOrange),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { if (state.hasText) { onSend(state.text); state.onSent() } },
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

/** 上部图片预览：展示真实图片 + 右上角移除按钮（使用 close_path.xml） */
@Composable
private fun ImagePreview(image: ImageBitmap, onRemove: () -> Unit) {
    Box {
        Image(
            bitmap = image,
            contentDescription = "已选择的图片",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        // 右上角删除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(RemoveButtonBackground)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.close_path),
                contentDescription = "移除图片",
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** 下部工具栏：左侧“+”与模式选择，右侧动作按钮 */
@Composable
private fun Toolbar(
    state: ChatInputState,
    isStreaming: Boolean,
    onAddImage: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // “+”按钮 → 拉起系统图片选择器
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, BtnAddBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onAddImage),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.add_file),
                    contentDescription = "添加图片",
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            // 模式选择 Fast
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                Text("Fast", color = ModeTextColor, fontSize = 15.sp)
                Spacer(Modifier.width(4.dp))
//                Text("▾", color = ModeTextColor, fontSize = 12.sp)
            }
        }

        // 右侧白色动作按钮：流式中显示停止，无内容显示麦克风，有内容显示发送
        val actionEnabled = isStreaming || state.showSend
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WhiteBtnBackground)
                .clickable(enabled = actionEnabled) { if (isStreaming) onStop() else onSend() },
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
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
