package com.xlin.travel.chat_screen

import com.xlin.travel.image_picker.DocumentFormats
import com.xlin.travel.image_picker.DocumentKind
import com.xlin.travel.image_picker.extractDocumentText

/** 用用户的第一句话作为会话标题 */
internal fun String.toConversationTitle(): String =
    lineSequence().firstOrNull()?.trim().orEmpty().take(30).ifBlank { "新对话" }

/**
 * 只在「发送给模型」时拼接到用户原始文本之后：图片把本地路径交给 media_analysis 工具，
 * 文档则按格式判断：
 * - 纯文本类（txt/md/csv/代码等）在本机直接抽取正文并内联（任何模型可读，不调工具）；
 * - Word/PPT/PDF 等交给模型调用 document_read 工具（工具在本机抽取，不依赖端点的文件解析能力）。
 * 这段提示永远不上屏、不落库，因此不会出现在用户自己发送的消息里。
 */
internal fun String.withAttachmentHints(
    imagePath: String?,
    documentPath: String?,
    documentName: String?,
): String {
    if (imagePath.isNullOrBlank() && documentPath.isNullOrBlank()) return this
    return buildString {
        append(this@withAttachmentHints)
        if (isNotEmpty()) append("\n\n")
        if (!imagePath.isNullOrBlank()) {
            append("[系统提示] 用户在本条消息附带了一张图片，本地绝对路径：")
            append(imagePath)
            append("。如需识别图片内容，请调用 media_analysis 工具，并把 imagePath 参数原样填写为上述路径。")
        }
        if (!documentPath.isNullOrBlank()) {
            if (imagePath != null) append("\n")
            // 优先用展示文件名定扩展名（缓存路径可能无后缀），为空时退回路径末段
            val displayName = documentName?.takeIf { it.isNotBlank() }
                ?: documentPath.substringAfterLast('/', "文档")
            if (DocumentFormats.kind(displayName) == DocumentKind.Text) {
                // 之前就能直接读的纯文本：本机抽取内联，不调阅读工具
                val body = extractDocumentText(documentPath, DocumentFormats.extensionOf(displayName))
                if (!body.isNullOrBlank()) {
                    append("[系统提示] 用户在本条消息附带了文档《")
                    append(displayName)
                    append("》，以下为设备端解析出的纯文本正文，请直接依据其内容回答：\n<<<document\n")
                    append(body)
                    append("\ndocument>>>")
                } else {
                    append("[系统提示] 用户在本条消息附带了文档《")
                    append(displayName)
                    append("》，但该文件在设备端读取失败；若需分析内容，请提示用户改粘贴文本。")
                }
            } else {
                // Word/PPT/PDF 等：把本地路径交给模型，由它调用 document_read（会呈现为工具卡片）
                append("[系统提示] 用户在本条消息附带了文档《")
                append(displayName)
                append("》，本地绝对路径：")
                append(documentPath)
                append("。如需了解其内容，请调用 document_read 工具，并把 documentPath 参数原样填写为上述路径。")
            }
        }
    }
}
