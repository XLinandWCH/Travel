package com.xlin.travel.agent.tool.kb

import travel.app.shared.generated.resources.Res

/**
 * 武汉里份知识库的读取与检索（假 RAG：没有向量检索，只有关键词 + 段落截取）。
 *
 * 为什么这么做：整库二十多章十几万字，全塞进上下文会直接把 32k 窗口打爆。
 * 所以分两步走——先给目录让模型挑章节，再只取命中关键词的段落（每段截断），
 * 一次工具调用最多带回 3 段、约 2700 字，既够回答又不烧上下文。
 */
internal object LifenKnowledge {

    private const val Dir = "files/lifen/"

    /** 整章返回时的上限：够交代背景，又不会单独吃掉整个上下文 */
    private const val MaxChapterChars = 6000

    /** 单段上限与一次最多返回的段数 */
    private const val MaxSectionChars = 900
    private const val MaxSections = 3

    /** 单个词在段落里的命中数上限：高频词（如「里份」）不该靠数量压过关键词 */
    private const val MaxHitsPerToken = 8

    /**
     * 工具入口。四种组合：
     * - 都为空：返回章节目录（AI 先看目录，再决定翻哪章）；
     * - 只给 chapter：整章返回（截断到 [MaxChapterChars]）；
     * - 只给 query：先按关键词猜章节，再取命中段落；
     * - 都给：在指定章节里找命中段落。
     */
    suspend fun answer(chapterHint: String, query: String): String {
        val hint = chapterHint.trim()
        val question = query.trim()
        if (hint.isEmpty() && question.isEmpty()) return LifenKnowledgeCatalog.toc()

        val chapter = LifenKnowledgeCatalog.byIdOrTitle(hint) ?: LifenKnowledgeCatalog.bestMatch(question)
        if (chapter == null) {
            return "[武汉里份知识库] 没有命中任何章节，先看目录再按章节号取内容。\n\n" +
                LifenKnowledgeCatalog.toc()
        }
        val text = read(chapter)
            ?: return "[武汉里份知识库] 章节《${chapter.title}》读取失败，请按目录换一章再试。"
        val body = if (question.isEmpty()) whole(text) else excerpts(text, question)
        return header(chapter) + body
    }

    /** 读资源文件：markdown 放在 composeResources/files/lifen，按 ASCII 文件名取 */
    private suspend fun read(chapter: LifenChapter): String? =
        runCatching { Res.readBytes(Dir + chapter.file).decodeToString() }
            .getOrNull()
            ?.replace("\r\n", "\n")
            // 知识库文件里混了零宽字符与 BOM，带进上下文会变成看不见的噪声
            ?.replace("\uFEFF", "")
            ?.replace("\u200B", "")
            ?.trim()

    private fun header(chapter: LifenChapter): String = buildString {
        append("[武汉里份知识库 · ${chapter.id} ${chapter.title}]\n")
        append("（离线写死的资料，不是实时数据；只依据下列内容作答，没写的就说资料里没有）\n\n")
    }

    private fun whole(text: String): String {
        if (text.length <= MaxChapterChars) return text
        return text.take(MaxChapterChars) +
            "\n\n（本章较长，已截断；带 query 可以只取相关段落）"
    }

    /** 按「# 标题」切段，只挑命中关键词的段落，避免整章灌进上下文 */
    private fun excerpts(text: String, question: String): String {
        val tokens = LifenKnowledgeTokens.of(question)
        if (tokens.isEmpty()) return whole(text)
        val ranked = sections(text)
            .map { it to score(it.second, tokens) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(MaxSections)
        if (ranked.isEmpty()) {
            return "（本章没有命中「$question」的段落，下面是本章开头，可作为背景）\n\n" + whole(text)
        }
        return buildString {
            ranked.forEachIndexed { index, (section, _) ->
                if (index > 0) append("\n\n")
                append("## ").append(section.first).append('\n')
                val body = section.second
                append(if (body.length <= MaxSectionChars) body else body.take(MaxSectionChars) + "…")
            }
        }
    }

    /**
     * 把一章切成 (标题, 段落)：先按「# 标题」分组，再按空行切细。
     * 知识库大部分内容是空行分隔的 FAQ，只按标题切会把一整组问答当成一段，
     * 命中时永远从组首开始截断，问「门票」却拿到「在哪里」——切到段落才能对上问题。
     */
    private fun sections(text: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        var title = "导读"
        val buffer = StringBuilder()

        fun flush() {
            val group = buffer.toString().trim()
            buffer.clear()
            if (group.isEmpty()) return
            group.split("\n\n")
                .map { it.trim() }
                .filter { block -> block.length >= 12 && block.any { it.isLetterOrDigit() } }
                .forEach { block -> result.add(title to block) }
        }

        text.lineSequence().forEach { line ->
            if (line.startsWith("#")) {
                flush()
                title = line.trimStart('#').trim().ifBlank { "正文" }
            } else {
                buffer.append(line).append('\n')
            }
        }
        flush()
        return result
    }

    private fun score(body: String, tokens: List<String>): Int =
        tokens.sumOf { token ->
            val hits = countOf(body, token).coerceAtMost(MaxHitsPerToken)
            // 长词更有辨识度：「石库门」比「里份」值钱
            hits * (1 + token.length)
        }

    private fun countOf(text: String, token: String): Int {
        if (token.isEmpty()) return 0
        var count = 0
        var index = text.indexOf(token)
        while (index >= 0) {
            count++
            index = text.indexOf(token, index + token.length)
        }
        return count
    }
}
