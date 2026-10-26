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

/** 需要用反斜杠转义的标点 */
private val Escapeable = "`*_~[]()<>#|!-+:".toSet()

/** 行内公式的近似排版：衬线 + 斜体，与正文区分 */
private val MathStyle = SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)

/** 行内语法：`代码`、**粗体**、*斜体*、~~删除线~~、[文字](链接)、\(..\)/$..$ 数学、\\@ 转义，可嵌套 */
internal fun inlineSpan(raw: String): AnnotatedString = buildAnnotatedString {
    appendInline(raw)
}

private fun AnnotatedString.Builder.appendInline(source: String) {
    var i = 0
    while (i < source.length) {
        val char = source[i]

        // 行内数学公式 \( .. \)：先于转义处理，避免 ( 被当作转义标点吃掉
        if (char == '\\' && i + 1 < source.length && source[i + 1] == '(') {
            val end = source.indexOf("\\)", i + 2)
            if (end > i) {
                pushStyle(MathStyle)
                append(MathRender.format(source.substring(i + 2, end)))
                pop()
                i = end + 2
                continue
            }
        }

        // 反斜杠转义：\@ \| 等处直接输出下一个字符（法律文档里邮箱常写成 \@）
        if (char == '\\' && i + 1 < source.length && source[i + 1] in Escapeable) {
            append(source[i + 1])
            i += 2
            continue
        }

        // 行内数学公式 $..$，去掉定界符后用 Unicode 近似渲染
        if (char == '$') {
            val end = source.indexOf('$', i + 1)
            if (end > i + 1) {
                val inner = source.substring(i + 1, end)
                // 内部不以空格贴合且不含换行才视为公式，避免把「$5 和 $10」这类金额误判
                val looksMath = inner.isNotEmpty() && !inner.first().isWhitespace() &&
                    !inner.last().isWhitespace() && !inner.contains('\n')
                if (looksMath) {
                    pushStyle(MathStyle)
                    append(MathRender.format(inner))
                    pop()
                    i = end + 1
                    continue
                }
            }
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
