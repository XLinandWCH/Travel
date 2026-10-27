package com.xlin.travel.markdown

import androidx.compose.ui.text.AnnotatedString

/** AI 正文解析后的块级元素，渲染层按类型选择字号与衬底 */
sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: AnnotatedString) : MarkdownBlock()
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock()

    /**
     * 列表：把连续的有序 / 无序条目合成一块，保留每条在原文中的编号（避免「1. 2. 3.」被重排回 1），
     * 并为子条目记录缩进层级。
     */
    data class ListBlock(val items: List<ListItem>) : MarkdownBlock()
    data class Quote(val text: AnnotatedString) : MarkdownBlock()
    data class Code(val language: String, val code: String) : MarkdownBlock()

    /** 独立成块的数学公式（$$..$$ 或 \[..\]），保存裸 LaTeX，交给 MathTypeset 做二维排版 */
    data class Math(val formula: String) : MarkdownBlock()

    /** 表格：首行为表头，[rows] 为数据行（列数统一按表头补齐 / 截断） */
    data class Table(
        val header: List<AnnotatedString>,
        val rows: List<List<AnnotatedString>>,
    ) : MarkdownBlock()

    data object Divider : MarkdownBlock()
}

/**
 * 列表条目：[number] 非空为有序项（保留源编号），为空则是圆点项；[indent] 为缩进层级（0 顶层）。
 */
data class ListItem(val number: Int?, val indent: Int, val text: AnnotatedString)

private val HeadingRegex = Regex("^(#{1,6})\\s+(.*)$")
private val BulletRegex = Regex("^(\\s*)[-*+]\\s+(.*)$")
private val OrderedRegex = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
private val QuoteRegex = Regex("^\\s*>\\s?(.*)$")
private val DividerRegex = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")
private val TableDividerCellRegex = Regex("^:?-{2,}:?$")

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
        val mathBlock = trimmed.startsWith("$$") || trimmed.startsWith("\\[")

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
                val items = mutableListOf<ListItem>()
                var next = index
                var lastNumbered = false
                while (next < lines.size) {
                    val l = lines[next]
                    val om = OrderedRegex.matchEntire(l)
                    val bm = BulletRegex.matchEntire(l)
                    if (om == null && bm == null) break
                    if (om != null) {
                        // 保留源编号，不再按出现顺序重排（修复「1. 2. 断一行后又回到 1.」）
                        val depth = om.groupValues[1].length / 2
                        items.add(ListItem(om.groupValues[2].toIntOrNull(), depth, inlineSpan(om.groupValues[3])))
                        lastNumbered = true
                    } else if (bm != null) {
                        // 源缩进优先；无缩进但紧跟有序项时视为子项（缩进一级），呈现为「•」并内缩
                        val lead = bm.groupValues[1].length
                        val depth = if (lead > 0) lead / 2 else if (lastNumbered) 1 else 0
                        items.add(ListItem(null, depth, inlineSpan(bm.groupValues[2])))
                        lastNumbered = false
                    }
                    next++
                }
                blocks.add(MarkdownBlock.ListBlock(items))
                index = next - 1
            }

            mathBlock -> {
                flushParagraph()
                val closed = MathRender.stripDelimiters(line)
                if (closed != null) {
                    blocks.add(MarkdownBlock.Math(closed))
                } else {
                    val close = if (trimmed.startsWith("$$")) "$$" else "\\]"
                    val first = (if (trimmed.startsWith("$$")) trimmed.removePrefix("$$") else trimmed.removePrefix("\\[")).trim()
                    val body = StringBuilder()
                    if (first.isNotEmpty() && !first.endsWith(close)) body.appendLine(first)
                    var next = index + 1
                    while (next < lines.size && !lines[next].trim().contains(close)) {
                        body.appendLine(lines[next])
                        next++
                    }
                    blocks.add(MarkdownBlock.Math(body.toString().trim()))
                    // 未闭合（仍在流式输出）时把剩余行都当作公式，并结束扫描
                    index = if (next < lines.size) next else lines.size - 1
                }
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
