package com.xlin.travel.a2ui

/**
 * 正文兜底净化：把模型误写进正文的 A2UI 结构（JSON）剪掉。
 *
 * 为什么需要：界面结构本应只走 a2ui_render 工具，但模型偶尔无视契约，在正文末尾再贴一份
 * `{"title": …, "blocks": [...]}` 或 ```json 代码块。聊天页会把它原样渲染成一串 token，
 * 用户看到的就是「正文下面接着一段乱码」。这里在流式结束后做一次保守剥离。
 *
 * 保守判据：片段里必须出现 "blocks" 键，且同时出现 "type" / "surface" / "title" 之一；
 * 普通代码块、用户复述的 JSON 都不会被误伤。
 */
internal fun String.stripA2uiPayload(): String {
    if (!contains("\"blocks\"")) return this
    // 1. 围栏代码块：只删「像界面结构」的那一段
    var text = FENCED_BLOCK.replace(this) { match ->
        if (match.value.looksLikeSurfaceJson()) "" else match.value
    }
    // 2. 裸 JSON：从第一个「像界面结构」的对象开头截断到文末
    val start = text.firstSurfaceJsonStart()
    if (start == 0) text = ""
    if (start > 0) text = text.substring(0, start)
    return text.trim()
}

private val FENCED_BLOCK = Regex("```[^\\n]*\\n?[\\s\\S]*?```")

private fun String.looksLikeSurfaceJson(): Boolean =
    contains("\"blocks\"") &&
        (contains("\"type\"") || contains("\"surface\"") || contains("\"title\""))

/** 从文本里找出第一个「像界面结构」的裸 JSON 对象的起始下标；找不到返回 -1 */
private fun String.firstSurfaceJsonStart(): Int {
    var index = indexOf('{')
    var scanned = 0
    while (index >= 0 && scanned < 40) {
        val end = matchingBraceEnd(this, index)
        if (end > index) {
            if (substring(index, end + 1).looksLikeSurfaceJson()) return index
            index = indexOf('{', end + 1)
        } else {
            index = indexOf('{', index + 1)
        }
        scanned++
    }
    return -1
}

/** 括号配平（跳过字符串内部的括号）；不闭合返回 -1 */
private fun matchingBraceEnd(text: String, start: Int): Int {
    var depth = 0
    var inString = false
    var escaped = false
    for (i in start until text.length) {
        val c = text[i]
        if (inString) {
            when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '"' -> inString = false
            }
        } else {
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
    }
    return -1
}
