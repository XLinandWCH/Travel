package com.xlin.travel.image_picker

import androidx.compose.runtime.Composable

/**
 * Web「本地文件」选择桩实现：浏览器无本地绝对路径供 media_analysis 工具读取，
 * 首版网页不支持文档附件，回调 null 视为取消（与图片通道处理一致）。
 */
@Composable
actual fun rememberFilePicker(onResult: (PickedFile?) -> Unit): () -> Unit = {
    onResult(null)
}
