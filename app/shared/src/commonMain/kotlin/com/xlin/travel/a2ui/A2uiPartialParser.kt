package com.xlin.travel.a2ui

/**
 * 半成品 JSON 解析：a2ui_render 的参数在流式输出里是一段一段到达的，
 * 这里只认「已经闭合的块」，让界面先长出前半屏，而不是等整段 JSON 写完才出现。
 *
 * 解析只做两件事：
 * 1. 取 blocks 数组里已闭合的 `{...}`，逐个解码成 [A2uiBlock]；
 * 2. 取 blocks 之前出现的 title / subtitle。
 *
 * 解析不了的片段直接跳过，任何输入都不会抛异常——半成品本来就是常态。
 */
internal object A2uiPartialParser {

    /** 一屏最多块数，与 [com.xlin.travel.agent.tool.A2uiRenderTool] 的裁剪口径一致 */
    private const val MaxBlocks = 8

    fun parse(raw: String): A2uiSurface? {
        if (raw.length < 2) return null
        val arrayStart = blocksArrayStart(raw)
        val head = if (arrayStart >= 0) raw.substring(0, arrayStart) else raw
        val blocks = if (arrayStart >= 0) closedObjects(raw, arrayStart + 1) else emptyList()
        val title = stringValue(head, "title").orEmpty()
        if (blocks.isEmpty() && title.isBlank()) return null
        val parsed = blocks
            .mapNotNull { decodeBlock(it) }
            .filterNot { it.isEmpty }
            .take(MaxBlocks)
        return A2uiSurface(
            title = title,
            subtitle = stringValue(head, "subtitle").orEmpty(),
            blocks = parsed,
        )
    }

    private fun decodeBlock(json: String): A2uiBlock? =
        runCatching { A2uiJson.decodeFromString<A2uiBlock>(json) }.getOrNull()

    /** 顶层 blocks 数组 `[` 的下标；结构不按约定来（blocks 之前先出现对象）时返回 -1 */
    private fun blocksArrayStart(raw: String): Int {
        val key = raw.indexOf("\"blocks\"")
        if (key < 0) return -1
        var i = key + "\"blocks\"".length
        while (i < raw.length && raw[i] != '[') {
            if (raw[i] == '{') return -1
            i++
        }
        return if (i < raw.length) i else -1
    }

    /** 从数组起点扫出所有已闭合的对象字面量；遇到还没闭合的尾部就停 */
    private fun closedObjects(raw: String, from: Int): List<String> {
        val out = mutableListOf<String>()
        var i = from
        while (i < raw.length && out.size < MaxBlocks) {
            when (raw[i]) {
                '{' -> {
                    val end = matchingBrace(raw, i) ?: return out
                    out += raw.substring(i, end + 1)
                    i = end + 1
                }
                ']' -> return out
                else -> i++
            }
        }
        return out
    }

    /** 括号配平，跳过字符串内部的括号与转义；未闭合返回 null */
    private fun matchingBrace(raw: String, start: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until raw.length) {
            val c = raw[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    /** 读 `"key": "value"`；引号还没闭合（正在生成）时返回 null，避免把半截标题当正文 */
    private fun stringValue(raw: String, key: String): String? {
        val keyAt = raw.indexOf("\"$key\"")
        if (keyAt < 0) return null
        var i = keyAt + key.length + 2
        while (i < raw.length && raw[i] != ':') i++
        while (i < raw.length && (raw[i] == ':' || raw[i].isWhitespace())) i++
        if (i >= raw.length || raw[i] != '"') return null
        i++
        val value = StringBuilder()
        var escaped = false
        while (i < raw.length) {
            val c = raw[i]
            if (escaped) {
                value.append(c)
                escaped = false
            } else when (c) {
                '\\' -> escaped = true
                '"' -> return value.toString()
                else -> value.append(c)
            }
            i++
        }
        return null
    }
}
