package com.xlin.travel.image_picker

/**
 * Web（Kotlin/JS）文档正文抽取桩：浏览器无本地绝对文件系统，「本地文件」通道亦留桩
 * （见 [rememberFilePicker] JS actual），文档附件在网页端不可用，统一返回 null。
 */
actual fun extractDocumentText(path: String, extension: String): String? = null
