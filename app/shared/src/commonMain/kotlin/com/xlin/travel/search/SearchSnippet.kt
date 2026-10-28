package com.xlin.travel.search

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.TextGray300

/** 关键字在片段中的前置保留字符数 */
private const val SnippetLeadingChars = 22

/** 片段窗口总长：约一行上限宽度，更长的正文交给省略号截断 */
private const val SnippetWindowChars = 110

/**
 * 生成单行摘要：压成一行后以首个关键字为中心开窗，保证关键字落在可见首行内；
 * 片段内所有关键字用强调色标出，窗口外的内容用…代替。
 */
fun buildSearchSnippet(content: String, keyword: String): AnnotatedString {
    val flat = content.replace('\n', ' ').replace('\r', ' ').trim()
    val match = flat.indexOf(keyword, ignoreCase = true)
    // 以首个关键字为中心开窗，保证关键字落在可见首行内；无命中时退回展示开头
    val start = if (match < 0) 0 else maxOf(0, match - SnippetLeadingChars)
    val end = minOf(flat.length, start + SnippetWindowChars)
    val prefix = if (start > 0) "… " else ""
    val suffix = if (end < flat.length) " …" else ""

    val base = SpanStyle(color = TextGray300)
    val highlight = SpanStyle(color = AccentOrange, fontWeight = FontWeight.Bold)
    return buildAnnotatedString {
        withStyle(base) { append(prefix) }
        var cursor = start
        if (keyword.isNotEmpty()) {
            var i = flat.indexOf(keyword, start, ignoreCase = true)
            while (i in start until end) {
                if (i > cursor) withStyle(base) { append(flat.substring(cursor, i)) }
                withStyle(highlight) { append(flat.substring(i, i + keyword.length)) }
                cursor = i + keyword.length
                i = flat.indexOf(keyword, cursor, ignoreCase = true)
            }
        }
        if (cursor < end) withStyle(base) { append(flat.substring(cursor, end)) }
        withStyle(base) { append(suffix) }
    }
}
