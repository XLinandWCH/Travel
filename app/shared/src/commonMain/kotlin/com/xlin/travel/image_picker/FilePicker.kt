package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable

/**
 * 系统文件选择器（「本地文件」通道，expect/actual）。
 * 返回一个 lambda，调用它即拉起系统文档选择界面；选择结果以 [PickedFile] 回调，取消为 null。
 *
 * 与图片通道分开：图片走「拍照 / 相册」规范化为 jpeg/png；这里面向文档（PDF、Office、文本等），
 * actual 需把选中的内容落到一份可读的本地绝对路径上（media_analysis 工具按路径读取，
 * content:// 之类的不透明 URI 对工具无意义），文件名用于扩展名白名单校验与界面展示。
 *
 * 是否属于「支持的文件格式」由调用方用 [DocumentFormats] 判定，判定失败在界面给出提示，
 * 因此本选择器不做格式过滤，允许选择任意文件后回调，把校验集中在 commonMain 一处。
 */
@Composable
expect fun rememberFilePicker(onResult: (PickedFile?) -> Unit): () -> Unit

/** 选中的本地文件：可读的绝对路径 + 展示用文件名 */
data class PickedFile(val path: String, val name: String)
