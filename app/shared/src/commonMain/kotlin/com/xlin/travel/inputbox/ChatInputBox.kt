package com.xlin.travel.inputbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.xlin.travel.color.InputCardBackground
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.image_picker.DocumentFormats
import com.xlin.travel.image_picker.DocumentKind
import com.xlin.travel.image_picker.rememberCameraCapture
import com.xlin.travel.image_picker.rememberFilePicker
import com.xlin.travel.image_picker.rememberImagePicker
import com.xlin.travel.inputbox.voice.VoiceContent
import com.xlin.travel.inputbox.voice.VoiceInputState
import kotlinx.coroutines.delay

/**
 * AI 聊天输入框卡片，对应 UI设计/输入框设计初稿.html 与 UI设计/输入框文件参考UI初稿.html
 * 结构：上部附件预览（图片 / 文件）+ 中部输入行 + 下部工具栏；
 * 点“+”在工具栏下方展开「拍照 / 相册 / 本地文件」动作面板。
 *
 * 输入行与工具栏本身在 [ChatInputToolbar] 里，工具栏左侧的「视频讲解」点开模拟通话页。
 */
@Composable
fun ChatInputBox(
    state: ChatInputState,
    onSend: (String) -> Unit = {},
    isStreaming: Boolean = false,
    onStop: () -> Unit = {},
    repository: ChatRepository? = null,
    onOpenVideoExplain: () -> Unit = {},
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
    // 本地文件：选中文档后按格式分类——白名单外直接拒绝；仅展示格式附上但提示暂不能分析；
    // 文本/docx 正常附上（发送时由 ChatState 在本机抽取正文并内联给模型）
    val pickFile = rememberFilePicker(onResult = { file ->
        if (file != null) {
            when (DocumentFormats.kind(file.name)) {
                DocumentKind.Unsupported -> {
                    val shown = DocumentFormats.extensionOf(file.name).ifBlank { "该" }
                    state.notifyAttachmentHint("不支持的$shown 文件格式，仅支持 ${DocumentFormats.supportedLabel}")
                }
                DocumentKind.DisplayOnly -> {
                    state.onDocumentPicked(file.path, file.name)
                    state.notifyAttachmentHint("《${file.name}》暂不支持内容分析，仅作为附件发送")
                }
                else -> state.onDocumentPicked(file.path, file.name)
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
        InputToolbar(
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
            // 视频讲解：进入模拟通话页（参考 UI设计/视频讲解参考UI.html）
            onVideoExplain = onOpenVideoExplain,
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
