package com.xlin.travel.image_picker

import org.jetbrains.compose.resources.DrawableResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.csv
import travel.app.shared.generated.resources.excel
import travel.app.shared.generated.resources.markdown
import travel.app.shared.generated.resources.pdf
import travel.app.shared.generated.resources.ppt
import travel.app.shared.generated.resources.txt
import travel.app.shared.generated.resources.unknown_file
import travel.app.shared.generated.resources.word

/**
 * 扩展名 → 文件类型图标资源。
 *
 * 这些图标（word/csv/excel/ppt/txt/pdf/markdown/unknown_file）已在 drawable 里专门调好配色，
 * 渲染时**不要**再叠加 ColorFilter.tint，否则会抹掉品牌色。未知/无专属图标的扩展名统一用 [unknown_file]。
 */
object FileIcons {

    /** 按小写扩展名挑选类型图标；无专属图标（如源码、json、html 等）回退到「未知文件」 */
    fun iconFor(extension: String): DrawableResource = when (extension.lowercase()) {
        "pdf" -> Res.drawable.pdf
        "doc", "docx" -> Res.drawable.word
        "xls", "xlsx" -> Res.drawable.excel
        "ppt", "pptx" -> Res.drawable.ppt
        "csv", "tsv" -> Res.drawable.csv
        "md", "markdown", "mdx" -> Res.drawable.markdown
        "txt", "log" -> Res.drawable.txt
        else -> Res.drawable.unknown_file
    }

    /** 直接用文件名判定（内部取扩展名） */
    fun iconForName(fileName: String): DrawableResource =
        iconFor(DocumentFormats.extensionOf(fileName))
}
