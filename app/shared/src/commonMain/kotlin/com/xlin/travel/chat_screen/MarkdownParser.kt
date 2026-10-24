package com.xlin.travel.chat_screen

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.xlin.travel.color.InlineCodeBackground
import com.xlin.travel.color.MarkdownLinkColor

/** AI 正文解析后的块级元素，渲染层按类型选择字号与衬底 */
sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: AnnotatedString) : MarkdownBlock()
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock()
    data class ListBlock(val ordered: Boolean, val items: List<AnnotatedString>) : MarkdownBlock()
    data class Quote(val text: AnnotatedString) : MarkdownBlock()
    data class Code(val language: String, val code: String) : MarkdownBlock()

    /** 表格：首行为表头，[rows] 为数据行（列数统一按表头补齐 / 截断） */
    data class Table(
        val header: List<AnnotatedString>,
        val rows: List<List<AnnotatedString>>,
    ) : MarkdownBlock()

    data object Divider : MarkdownBlock()
}

private val HeadingRegex = Regex("^(#{1,6})\\s+(.*)$")
private val BulletRegex = Regex("^\\s*[-*+]\\s+(.*)$")
private val OrderedRegex = Regex("^\\s*\\d+[.)]\\s+(.*)$")
private val QuoteRegex = Regex("^\\s*>\\s?(.*)$")
private val DividerRegex = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")
private val TableDividerCellRegex = Regex("^:?-{2,}:?$")

/** 需要用反斜杠转义的标点 */
private val Escapeable = "`*_~[]()<>#|!-+:".toSet()

/**
 * 块级解析：代码围栏 → 分隔线 → 标题 → 引用 → 列表 → 表格 → 段落。
 * 流式输出时最后一块可能不完整（例如 ``` 未闭合），这里按“已有内容”宽容处理，不报错。
 */
internal fun parseMarkdown(source: String): List<MarkdownBlock> {
    val lines = source.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = StringBuilder()
    var index = 0

    fun flushParagraph() {
        val text = paragraph.toString().trimEnd()
        if (text.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(inlineSpan(text)))
            paragraph.setLength(0)
        }
    }

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trimStart()
        val heading = HeadingRegex.matchEntire(trimmed)
        val fence = trimmed.startsWith("```")
        val quoted = QuoteRegex.matches(line)
        val bulleted = BulletRegex.matches(line)
        val numbered = OrderedRegex.matches(line)
        val tableRow = trimmed.startsWith("|")

        when {
            line.isBlank() -> flushParagraph()

            fence -> {
                flushParagraph()
                val language = trimmed.removePrefix("```").trim()
                val body = StringBuilder()
                var next = index + 1
                while (next < lines.size && !lines[next].trimStart().startsWith("```")) {
                    body.appendLine(lines[next])
                    next++
                }
                blocks.add(MarkdownBlock.Code(language, body.toString().trimEnd()))
                // 未闭合（仍在流式输出）时把剩余行都当作代码，并结束扫描
                index = if (next < lines.size) next else lines.size - 1
            }

            DividerRegex.matches(line) -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Divider)
            }

            heading != null -> {
                flushParagraph()
                blocks.add(
                    MarkdownBlock.Heading(
                        level = heading.groupValues[1].length,
                        text = inlineSpan(heading.groupValues[2]),
                    )
                )
            }

            quoted -> {
                flushParagraph()
                val body = StringBuilder()
                var next = index
                while (next < lines.size && QuoteRegex.matches(lines[next])) {
                    body.appendLine(QuoteRegex.matchEntire(lines[next])!!.groupValues[1])
                    next++
                }
                blocks.add(MarkdownBlock.Quote(inlineSpan(body.toString().trimEnd())))
                index = next - 1
            }

            bulleted || numbered -> {
                flushParagraph()
                val ordered = numbered
                val marker = if (ordered) OrderedRegex else BulletRegex
                val items = mutableListOf<AnnotatedString>()
                var next = index
                while (next < lines.size && marker.matches(lines[next])) {
                    items.add(inlineSpan(marker.matchEntire(lines[next])!!.groupValues[1]))
                    next++
                }
                blocks.add(MarkdownBlock.ListBlock(ordered, items))
                index = next - 1
            }

            tableRow -> {
                flushParagraph()
                val rows = mutableListOf<List<String>>()
                var next = index
                while (next < lines.size && lines[next].trimStart().startsWith("|")) {
                    val cells = splitTableRow(lines[next])
                    // | --- | --- | 这种分隔行只用来标记表格，本身不渲染
                    if (cells.none { it.isNotEmpty() } || !cells.all { TableDividerCellRegex.matches(it) }) {
                        rows.add(cells)
                    }
                    next++
                }
                if (rows.isNotEmpty()) blocks.add(buildTable(rows))
                index = next - 1
            }

            else -> paragraph.append(line).append('\n')
        }
        index++
    }
    flushParagraph()
    return blocks
}

/** 拆一行表格：去掉首尾竖线后按竖线切格，格内文字两端留白 */
private fun splitTableRow(line: String): List<String> =
    line.trim().removeSurrounding("|").split("|").map { it.trim() }

/** 首行当表头，其余为数据行，列数按表头补齐或截断 */
private fun buildTable(rows: List<List<String>>): MarkdownBlock.Table {
    val columnCount = rows.maxOf { it.size }
    val normalized = rows.map { row -> List(columnCount) { column -> row.getOrElse(column) { "" } } }
    val header = normalized.first().map { inlineSpan(it) }
    val body = if (normalized.size > 1) normalized.drop(1).map { row -> row.map { inlineSpan(it) } } else emptyList()
    return MarkdownBlock.Table(header, body)
}

/** 行内语法：`代码`、**粗体**、*斜体*、~~删除线~~、[文字](链接)、\\@ 转义，可嵌套 */
internal fun inlineSpan(raw: String): AnnotatedString = buildAnnotatedString {
    appendInline(raw)
}

private fun AnnotatedString.Builder.appendInline(source: String) {
    var i = 0
    while (i < source.length) {
        val char = source[i]

        // 反斜杠转义：\@ \| 等处直接输出下一个字符（法律文档里邮箱常写成 \@）
        if (char == '\\' && i + 1 < source.length && source[i + 1] in Escapeable) {
            append(source[i + 1])
            i += 2
            continue
        }

        // 行内代码
        if (char == '`') {
            val end = source.indexOf('`', i + 1)
            if (end > i) {
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = InlineCodeBackground))
                append(source.substring(i + 1, end))
                pop()
                i = end + 1
                continue
            }
        }

        // 粗体
        if (source.startsWith("**", i)) {
            val end = source.indexOf("**", i + 2)
            if (end > i + 1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInline(source.substring(i + 2, end))
                pop()
                i = end + 2
                continue
            }
        }

        // 删除线
        if (source.startsWith("~~", i)) {
            val end = source.indexOf("~~", i + 2)
            if (end > i + 1) {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                appendInline(source.substring(i + 2, end))
                pop()
                i = end + 2
                continue
            }
        }

        // 斜体（* 与 _）
        if (char == '*' || char == '_') {
            val end = source.indexOf(char, i + 1)
            if (end > i) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendInline(source.substring(i + 1, end))
                pop()
                i = end + 1
                continue
            }
        }

        // 链接：只渲染文字并着色，跳转交给系统浏览器（当前无 WebView）
        if (char == '[') {
            val close = source.indexOf("](", i)
            val urlEnd = if (close > i) source.indexOf(')', close + 2) else -1
            if (close > i && urlEnd > close) {
                pushStyle(SpanStyle(color = MarkdownLinkColor, textDecoration = TextDecoration.Underline))
                append(source.substring(i + 1, close))
                pop()
                i = urlEnd + 1
                continue
            }
        }

        append(char)
        i++
    }
}
