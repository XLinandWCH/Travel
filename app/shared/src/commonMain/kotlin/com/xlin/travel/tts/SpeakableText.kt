package com.xlin.travel.tts

/**
 * 把 AI 正文（Markdown）清洗成适合朗读的纯文本：
 * 思考内容本来就不在其中（调用方只传 message.content），这里只负责去掉
 * 代码块 / 行内代码 / 图片链接 / 表格分隔 / 强调符号等「读出来会出戏」的排版噪音，
 * 链接保留文字、去掉 URL。规则都是轻量正则，不引 Markdown 解析库。
 */
internal fun String.toSpeakableText(): String {
    var s = this
    // 代码块整段移除（朗读代码字符没有意义）
    s = CODE_BLOCK.replace(s, "")
    // 图片 ![alt](url)：连 alt 一起移除，正文里通常还有文字描述
    s = IMAGE.replace(s, "")
    // 链接 [text](url) → text
    s = LINK.replace(s) { it.groupValues[1] }
    // 标题 / 引用 / 列表符号
    s = BLOCK_MARKERS.replace(s, "")
    // 表格分隔行（| --- | --- |）与残留的竖线
    s = TABLE_DIVIDER.replace(s, "")
    s = s.replace('|', ' ')
    // 加粗 / 斜体 / 删除线 / 行内代码的反引号等强调符号
    s = INLINE_MARKERS.replace(s, "")
    // 折叠多余空白，行首缩进清掉，避免朗读出现奇怪停顿
    s = MULTI_BLANK.replace(s, "\n").replace(LINE_SPACE, " ").trim()
    return s
}

private val CODE_BLOCK = Regex("```[\\s\\S]*?(?:```|$)")
private val IMAGE = Regex("!\\[[^]]*]\\([^)]*\\)")
private val LINK = Regex("\\[([^]]+)]\\([^)]*\\)")
private val BLOCK_MARKERS = Regex("(?m)^\\s*(#{1,6}\\s*|>+\\s*|[-*+]\\s+|\\d+\\.\\s+)")
private val TABLE_DIVIDER = Regex("(?m)^\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")
private val INLINE_MARKERS = Regex("[`*_~]{1,3}")
private val MULTI_BLANK = Regex("\n{3,}")
private val LINE_SPACE = Regex("(?m)^\\s+")
