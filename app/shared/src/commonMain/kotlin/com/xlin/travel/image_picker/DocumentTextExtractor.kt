package com.xlin.travel.image_picker

/**
 * 设备端文档正文抽取（expect/actual）。
 *
 * 文档不发给模型做二进制解析（OpenAI 兼容端点只吃 text/image_url），而是在本机把可读文本抽出来，
 * 以纯文本内联进对话提示词，任何模型都能理解。返回 null 表示该平台/格式暂无法抽取正文
 * （如 iOS 无 zip 解 docx、或 pdf/xls/ppt 等设备端不可抽取），调用方据此回退为「仅展示」提示。
 *
 * - [extension] 为小写扩展名，[DocumentFormats.kind] 的判定依据；
 * - 具体各平台按扩展名分派：纯文本按 UTF-8 读、docx 走 zip 解 XML、其余返回 null。
 */
expect fun extractDocumentText(path: String, extension: String): String?
