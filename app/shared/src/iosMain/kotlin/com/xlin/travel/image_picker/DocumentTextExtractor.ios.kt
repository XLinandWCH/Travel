package com.xlin.travel.image_picker

/**
 * iOS 文档正文抽取桩：iOS 的「本地文件」通道当前留桩（见 [rememberFilePicker] iOS actual），
 * 不会产出可读本地路径，故这里统一返回 null；即便补齐选择器，也需在 Mac/Xcode CI 侧用
 * Foundation 的 NSAttributedString(docx) / NSData(zip) 实现，Windows 无法编译验证。
 * 返回 null 时调用方回退为「仅展示 + 暂不支持内容分析」，不影响编译与运行。
 */
actual fun extractDocumentText(path: String, extension: String): String? = null
