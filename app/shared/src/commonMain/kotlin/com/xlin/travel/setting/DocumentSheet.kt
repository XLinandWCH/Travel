package com.xlin.travel.setting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.chat_screen.MarkdownText
import com.xlin.travel.color.TextGray500
import com.xlin.travel.sheet.TravelBottomSheet
import travel.app.shared.generated.resources.Res

/**
 * 法律文档面板：服务条款 / 隐私政策正文放在
 * `app/shared/src/commonMain/composeResources/files/`（terms.md、privacy.md），
 * 首次展开时才读取文件，之后用聊天同一套 Markdown 渲染器显示，可上下滚动。
 */
@Composable
internal fun DocumentSheet(
    state: SettingsSheetState,
    sheet: SettingsSheet,
    title: String,
    file: String,
) {
    val visible = state.open == sheet
    var content by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(visible, file) {
        if (visible && content == null && !failed) {
            runCatching { loadDocument(file) }
                .onSuccess { content = it }
                .onFailure { failed = true }
        }
    }

    TravelBottomSheet(
        visible = visible,
        onDismiss = { state.dismiss() },
        title = title,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = DocBodyMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            val text = content
            when {
                text != null -> MarkdownText(markdown = text, modifier = Modifier.fillMaxWidth())
                failed -> DocHint("文档加载失败，请稍后重试")
                else -> DocHint("正在加载…")
            }
        }
    }
}

@Composable
private fun DocHint(text: String) {
    Text(text = text, color = TextGray500, fontSize = 14.sp, modifier = Modifier.padding(vertical = 18.dp))
}

/** 读取 markdown 原文：UTF-8 解码后去掉零宽字符与空的标题行（例如只有 “#” 的行） */
private suspend fun loadDocument(file: String): String =
    Res.readBytes(file)
        .decodeToString()
        .replace("\uFEFF", "")
        .replace("\u200B", "")
        .lines()
        .filterNot { EmptyHeading.matches(it) }
        .joinToString("\n")

/** 正文最大高度，超出后在面板内部滚动（用 heightIn 以适配面板自身的测量约束） */
private val DocBodyMaxHeight = 460.dp
private val EmptyHeading = Regex("^\\s*#+\\s*$")
