package com.xlin.travel.image_picker

/**
 * 「本地文件」附件的格式分类。
 *
 * 背景：模型侧的 OpenAI 兼容端点只接受 text 与 image_url 两类内容，直接投递文档二进制
 * （.docx/.pdf 等）会被上游拒收（"data did not match any variant of untagged enum MessageContent"）。
 * 因此文档不发给模型做二进制解析，而是在设备端抽取正文、以纯文本内联进提示词。
 *
 * 分类（决定「能否被 AI 读取」与图标选择）：
 * - [DocumentKind.Text]：txt/md/csv/log/json/xml/yaml/常见源码等，直接按 UTF-8 读取；
 * - [DocumentKind.Docx]：.docx 是 zip，解出 word/document.xml 去标签取正文（Android/JVM 可行，iOS 暂不支持）；
 * - [DocumentKind.DisplayOnly]：pdf/doc/xls/xlsx/ppt/pptx 等设备端无法可靠抽取正文，
 *   仅作为附件展示并提示「暂不支持内容分析」；
 * - [DocumentKind.Unsupported]：不在白名单内，选择时直接拒绝并提示。
 *
 * 图片（jpg/png 等）不走这里——它们由「拍照 / 相册」通道规范化后作为 imagePath 交给视觉工具。
 */
enum class DocumentKind { Text, Docx, DisplayOnly, Unsupported }

object DocumentFormats {

    /** 纯文本 / 代码：按 UTF-8 直接读取正文 */
    private val textExtensions: Set<String> = setOf(
        "txt", "log", "md", "markdown", "mdx", "csv", "tsv",
        "json", "jsonl", "xml", "yaml", "yml", "toml", "ini", "conf", "properties",
        "html", "htm", "css", "scss", "less",
        "kt", "kts", "java", "js", "ts", "jsx", "tsx", "py", "go", "rs", "rb",
        "php", "c", "h", "cpp", "hpp", "cs", "swift", "sql", "sh", "bat", "ps1",
    )

    /** Office Open XML 的 Word：zip 解包取正文（依赖 java.util.zip，Android/JVM 可用） */
    private val docxExtensions: Set<String> = setOf("docx")

    /** 可展示但设备端暂不能抽取正文的格式 */
    private val displayOnlyExtensions: Set<String> = setOf(
        "pdf", "doc", "xls", "xlsx", "ppt", "pptx",
    )

    /** 从文件名取小写扩展名（不含点）；无扩展名返回空串 */
    fun extensionOf(fileName: String): String =
        fileName.substringAfterLast('.', "").lowercase()

    /** 归类：决定发送时走内联、展示还是拒绝 */
    fun kind(fileName: String): DocumentKind {
        val ext = extensionOf(fileName)
        return when {
            ext in textExtensions -> DocumentKind.Text
            ext in docxExtensions -> DocumentKind.Docx
            ext in displayOnlyExtensions -> DocumentKind.DisplayOnly
            else -> DocumentKind.Unsupported
        }
    }

    /** 是否属于允许附加的白名单（可展示即算支持；Unsupported 才拒绝） */
    fun isSupported(fileName: String): Boolean = kind(fileName) != DocumentKind.Unsupported

    /** 该格式是否预期可被设备端抽取正文（Text/Docx；抽取仍可能在个别平台失败并回退提示） */
    fun isContentExtractable(fileName: String): Boolean =
        kind(fileName) == DocumentKind.Text || kind(fileName) == DocumentKind.Docx

    /** 支持附件的格式的中文标签，用于「不支持」提示 */
    val supportedLabel: String =
        "TXT、Markdown、CSV、代码、Word(docx)、PDF、Excel、PPT"
}
