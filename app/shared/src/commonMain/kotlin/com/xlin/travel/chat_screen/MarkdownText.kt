package com.xlin.travel.chat_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AppTheme
import com.xlin.travel.color.AssistantTextColor
import com.xlin.travel.color.CodeBlockBackground
import com.xlin.travel.color.CodeBlockText
import com.xlin.travel.color.QuoteBarColor
import com.xlin.travel.color.ReasoningTextColor
import com.xlin.travel.color.SettingDivider
import com.xlin.travel.color.TextWhite

/**
 * AI 正文的 Markdown 渲染：支持标题、粗体 / 斜体 / 行内代码 / 删除线 / 链接文字、
 * 无序与有序列表、引用、水平线、围栏代码块（长行可横向滚动）。
 *
 * 内容或主题变化时重新解析一次，流式输出过程中每帧只解析新增文本。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val isDark = AppTheme.isDark
    val blocks = remember(markdown, isDark) { parseMarkdown(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BlockGap),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> HeadingText(block)
                is MarkdownBlock.Paragraph -> BodyText(block.text)
                is MarkdownBlock.ListBlock -> ListText(block)
                is MarkdownBlock.Quote -> QuoteText(block.text)
                is MarkdownBlock.Code -> CodeBlock(block.code)
                is MarkdownBlock.Table -> TableText(block)
                MarkdownBlock.Divider -> Divider()
            }
        }
    }
}

@Composable
private fun HeadingText(block: MarkdownBlock.Heading) {
    Text(
        text = block.text,
        color = TextWhite,
        fontSize = when (block.level) {
            1 -> 20.sp
            2 -> 18.sp
            3 -> 17.sp
            else -> 16.sp
        },
        fontWeight = FontWeight.Bold,
        lineHeight = when (block.level) {
            1 -> 28.sp
            else -> 24.sp
        },
        modifier = Modifier.padding(top = if (block.level <= 2) 4.dp else 0.dp),
    )
}

@Composable
private fun BodyText(text: AnnotatedString) {
    Text(
        text = text,
        color = AssistantTextColor,
        fontSize = 16.sp,
        lineHeight = BodyLineHeight,
    )
}

@Composable
private fun ListText(block: MarkdownBlock.ListBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (block.ordered) "${index + 1}." else "•",
                    color = AssistantTextColor,
                    fontSize = 16.sp,
                    lineHeight = BodyLineHeight,
                )
                Text(
                    text = item,
                    color = AssistantTextColor,
                    fontSize = 16.sp,
                    lineHeight = BodyLineHeight,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QuoteText(text: AnnotatedString) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(QuoteBarColor),
        )
        Text(
            text = text,
            color = ReasoningTextColor,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CodeBlockBackground)
            .horizontalScroll(rememberScrollState()),
    ) {
        Text(
            text = code,
            color = CodeBlockText,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SettingDivider),
    )
}

/**
 * 表格：等宽分列（列多时每列宽度有限，长文本自动换行），
 * 表头加粗 + 浅色衬底，数据行之间用细分隔线区分。
 */
@Composable
private fun TableText(block: MarkdownBlock.Table) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableRow(block.header, header = true)
        block.rows.forEach { row ->
            Divider()
            TableRow(row, header = false)
        }
    }
}

@Composable
private fun TableRow(cells: List<AnnotatedString>, header: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (header) CodeBlockBackground else Color.Transparent),
    ) {
        cells.forEach { cell ->
            Text(
                text = cell,
                color = if (header) TextWhite else AssistantTextColor,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
            )
        }
    }
}

private val BlockGap = 6.dp
private val BodyLineHeight = 24.sp
