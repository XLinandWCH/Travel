package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
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
import com.xlin.travel.markdown.MarkdownText
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.TextGray500
import com.xlin.travel.navigation.PageTopBar
import travel.app.shared.generated.resources.Res

/** 设置页“关于”分组里进入的两份法律文档 */
enum class LegalDoc(val title: String, val file: String) {
    Terms("服务条款（暂行版）", "files/terms.md"),
    Privacy("隐私政策（暂行版）", "files/privacy.md"),
}

/**
 * 法律文档子页面（与服务条款 / 隐私政策条目对应）：
 * 正文放在 `app/shared/src/commonMain/composeResources/files/`，
 * 进入页面时才读取文件，之后用聊天同一套 Markdown 渲染器显示，整页可上下滚动。
 */
@Composable
fun DocumentScreen(
    doc: LegalDoc,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var content by remember(doc) { mutableStateOf<String?>(null) }
    var failed by remember(doc) { mutableStateOf(false) }

    LaunchedEffect(doc) {
        runCatching { loadMarkdown(doc.file) }
            .onSuccess { content = it }
            .onFailure { failed = true }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = doc.title, onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
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
private suspend fun loadMarkdown(file: String): String =
    Res.readBytes(file)
        .decodeToString()
        .replace("\uFEFF", "")
        .replace("\u200B", "")
        .lines()
        .filterNot { EmptyHeading.matches(it) }
        .joinToString("\n")

private val EmptyHeading = Regex("^\\s*#+\\s*$")
