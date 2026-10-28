package com.xlin.travel.image_picker

import java.io.File
import java.util.zip.ZipInputStream

/**
 * JVM（桌面）文档正文抽取：与 Android 同构（都用 java.util.zip 解 docx、File 读 UTF-8 文本），
 * 便于桌面端本地测试。其余格式（pdf/xls/ppt 等）返回 null，调用方回退为「仅展示」。
 *
 * 读取前按 [MAX_TEXT_BYTES] 限制体积，抽取文本按 [MAX_TEXT_CHARS] 截断，避免超大文件撑爆内存或上下文。
 */
actual fun extractDocumentText(path: String, extension: String): String? = when (extension) {
    in DOCX_EXTENSIONS -> readDocx(path)
    in TEXT_EXTENSIONS -> readPlainUtf8(path)
    else -> null
}

private val TEXT_EXTENSIONS = setOf(
    "txt", "log", "md", "markdown", "mdx", "csv", "tsv",
    "json", "jsonl", "xml", "yaml", "yml", "toml", "ini", "conf", "properties",
    "html", "htm", "css", "scss", "less",
    "kt", "kts", "java", "js", "ts", "jsx", "tsx", "py", "go", "rs", "rb",
    "php", "c", "h", "cpp", "hpp", "cs", "swift", "sql", "sh", "bat", "ps1",
)
private val DOCX_EXTENSIONS = setOf("docx")

private const val MAX_TEXT_BYTES = 8L * 1024 * 1024   // 8MB
private const val MAX_TEXT_CHARS = 100_000

private fun readPlainUtf8(path: String): String? = runCatching {
    val file = File(path)
    if (!file.exists() || file.length() > MAX_TEXT_BYTES) return@runCatching null
    file.readText(Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }?.truncateDoc()
}.getOrNull()

private fun readDocx(path: String): String? = runCatching {
    val xml = File(path).inputStream().use { input ->
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            var docXml: ByteArray? = null
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    docXml = zip.readBytes()
                    break
                }
                entry = zip.nextEntry
            }
            docXml
        }
    } ?: return@runCatching null
    docxXmlToText(xml.toString(Charsets.UTF_8)).truncateDoc().takeIf { it.isNotEmpty() }
}.getOrNull()

/**
 * docx 的 document.xml 转纯文本：段落结束补换行，制表 / 换行符补位，
 * 去掉所有标签后还原基本实体，再把连续空行收敛。
 */
private fun docxXmlToText(xml: String): String {
    val withBreaks = xml
        .replace("</w:p>", "\n")
        .replace("<w:br/>", "\n")
        .replace("<w:tab/>", "\t")
    val stripped = withBreaks.replace(Regex("<[^>]+>"), "")
    val decoded = stripped
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'")
        .replace("&amp;", "&")
    return decoded
        .replace(Regex("[ \t]*\n[ \t]*"), "\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

private fun String.truncateDoc(): String =
    if (length > MAX_TEXT_CHARS) take(MAX_TEXT_CHARS) + "\n…（内容过长，已截断）" else this
