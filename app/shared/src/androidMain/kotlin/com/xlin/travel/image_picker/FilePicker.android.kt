package com.xlin.travel.image_picker

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.util.UUID

/**
 * Android「本地文件」选择：使用系统文档选择器（[ActivityResultContracts.OpenDocument]）。
 *
 * 选择任意类型（通配 MIME）后回调，格式白名单校验集中在 commonMain 用 [DocumentFormats] 完成，
 * 因此这里不做过滤。选中结果可能是 content:// 之类的不透明 URI，media_analysis 工具需要一份
 * 可读的本地绝对路径，故立即把内容拷贝到 cacheDir/documents/<uuid>.<ext> 并回传该路径与展示文件名。
 * 取消回调 null。
 */
@Composable
actual fun rememberFilePicker(onResult: (PickedFile?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) {
            onResult(null)
            return@rememberLauncherForActivityResult
        }
        val picked = runCatching {
            val displayName = queryDisplayName(context.contentResolver, uri)
                ?: "file_${UUID.randomUUID()}"
            // 扩展名优先取文件名的后缀，缺失时回落 URI 末段，保证落盘文件带正确扩展名（koog 按扩展名拼 MIME）
            val extension = DocumentFormats.extensionOf(displayName)
                .ifBlank { displayName.substringAfterLast('.', "") }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@runCatching null
            val dir = File(context.cacheDir, "documents").apply { mkdirs() }
            val suffix = if (extension.isBlank()) "" else ".$extension"
            val out = File(dir, "doc_${UUID.randomUUID()}$suffix")
            out.writeBytes(bytes)
            PickedFile(path = out.absolutePath, name = displayName)
        }.getOrNull()
        onResult(picked)
    }
    return { launcher.launch(arrayOf("*/*")) }
}

/** 查询 content URI 的展示文件名，查不到返回 null */
private fun queryDisplayName(resolver: android.content.ContentResolver, uri: Uri): String? {
    if (uri.scheme == "content") {
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()?.let { return it }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
}
