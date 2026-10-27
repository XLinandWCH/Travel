package com.xlin.travel.inputbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
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
import com.xlin.travel.color.WhiteBtnBackground
import com.xlin.travel.color.WhiteBtnIcon
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.image_picker.DocumentFormats
import com.xlin.travel.image_picker.rememberCameraCapture
import com.xlin.travel.image_picker.rememberFilePicker
import com.xlin.travel.image_picker.rememberImagePicker
import com.xlin.travel.inputbox.voice.VoiceContent
import com.xlin.travel.inputbox.voice.VoiceInputState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.add_file
import travel.app.shared.generated.resources.microphone
import travel.app.shared.generated.resources.send
import travel.app.shared.generated.resources.stop

/**
 * AI 聊天输入框卡片，对应 UI设计/输入框设计初稿.html 与 UI设计/输入框文件参考UI初稿.html
 * 结构：上部附件预览（图片 / 文件）+ 中部输入行 + 下部工具栏；
 * 点“+”在工具栏下方展开「拍照 / 相册 / 本地文件」动作面板。
 */
@Composable
fun ChatInputBox(
    state: ChatInputState,
    onSend: (String) -> Unit = {},
    isStreaming: Boolean = false,
    onStop: () -> Unit = {},
    repository: ChatRepository? = null,
    modifier: Modifier = Modifier,
) {
    // 相册：系统图片选择器，选中结果（位图 + 本地路径）写入 state（取消为 null）
    val pickImage = rememberImagePicker(onResult = { bitmap, path ->
        state.onImagePicked(bitmap, path)
        if (bitmap != null) state.collapseAttachments()
    })
    // 拍照：拉起系统相机，拍出的照片在各平台 actual 中一步转 JPEG 后写入 state
    val captureCamera = rememberCameraCapture(onResult = { bitmap, path ->
        state.onImagePicked(bitmap, path)
        if (bitmap != null) state.collapseAttachments()
    })
    // 本地文件：选中文档后按白名单校验，不支持的格式给出提示
    val pickFile = rememberFilePicker(onResult = { file ->
        if (file != null) {
            if (DocumentFormats.isSupported(file.name)) {
                val ext = DocumentFormats.extensionOf(file.name)
                state.onDocumentPicked(file.path, file.name, DocumentFormats.mimeFor(ext))
            } else {
                val shown = DocumentFormats.extensionOf(file.name).ifBlank { "该" }
                state.notifyAttachmentHint("不支持的$shown 文件格式，仅支持 ${DocumentFormats.supportedLabel}")
            }
            state.collapseAttachments()
        }
    })
    // 语音模式状态（波形 / 阶段 / 倒计时），仅在语音卡片组合时使用
    val voice = remember { VoiceInputState() }
    // 发送后主动失焦，收起输入法键盘（工具栏按钮发送不会触发 IME 动作，需手动关闭）
    val focusManager = LocalFocusManager.current

    // 附件提示出现后自动消失（与 TopToast 同理，按 tick 重新计时）
    LaunchedEffect(state.attachmentHintTick) {
        if (state.attachmentHintTick > 0) {
            delay(AttachmentHintVisibleMillis)
            state.clearAttachmentHint()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(InputCardBackground)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
    ) {
        // 上：图片预览（真实图片）；语音模式进入时已清空图片，通常不显示
        state.pickedImage?.let { image ->
            ImagePreview(image = image, onRemove = { state.onImageRemoved() })
            Spacer(Modifier.height(10.dp))
        }
        // 上：已选文档条幅（本地文件），与图片预览可共存
        state.pickedDocumentName?.let { name ->
            FileChip(name = name, onRemove = { state.onDocumentRemoved() })
            Spacer(Modifier.height(10.dp))
        }

        // 语音模式：复用同一卡片，中部换波形、底部换关闭/状态/完成
        if (state.voiceMode && repository != null) {
            VoiceContent(state = state, voice = voice, repository = repository, onSend = onSend)
            return@Column
        }

        // 中：输入行
        InputRow(state = state, onSend = onSend)

        Spacer(Modifier.height(8.dp))

        // 下：工具栏（“+”切换动作面板展开 / 收起）
        Toolbar(
            state = state,
            isStreaming = isStreaming,
            onToggleAttachments = { state.toggleAttachments() },
            onSend = {
                if (state.showSend) {
                    onSend(state.text)
                    state.onSent()
                    focusManager.clearFocus(force = true)
                }
            },
            onStop = onStop,
            // 麦克风：有仓库（已登录）则进入语音模式；否则（游客）按发送引导登录
            onMic = { if (repository != null) state.startVoice() else onSend(state.text) },
        )

        // 附件提示：紧跟工具栏下方（本地文件选择后即时反馈）
        state.attachmentHint?.let { hint ->
            Spacer(Modifier.height(6.dp))
            AttachmentHintBanner(message = hint)
        }

        // “+”展开的拍照 / 相册 / 本地文件动作面板（在工具栏下方）
        AnimatedVisibility(
            visible = state.showAttachments,
            enter = expandVertically(animationSpec = tween(180)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(),
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                AttachmentActionsRow(
                    onCamera = captureCamera,
                    onGallery = pickImage,
                    onFile = pickFile,
                )
            }
        }
    }
}

/** 附件提示停留时长（毫秒） */
private const val AttachmentHintVisibleMillis = 2600L

/** 中部输入行：文本输入（使用系统默认光标） */
@Composable
private fun InputRow(state: ChatInputState, onSend: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
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

/** 下部工具栏：左侧“+”与模式选择，右侧动作按钮 */
@Composable
private fun Toolbar(
    state: ChatInputState,
    isStreaming: Boolean,
    onToggleAttachments: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onMic: () -> Unit,
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
            // 模式选择 Fast
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                Text("Fast", color = ModeTextColor, fontSize = 15.sp)
                Spacer(Modifier.width(4.dp))
//                Text("▾", color = ModeTextColor, fontSize = 12.sp)
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
