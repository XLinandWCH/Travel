package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.image_picker.DocumentFormats
import com.xlin.travel.image_picker.extractDocumentText
import kotlinx.serialization.Serializable

/**
 * 文档阅读工具（只处理文件，不处理图片——图片走 [MediaAnalysisTool]）。
 *
 * 设计要点（回应「上海人工智能实验室等模型可能不能直接接收文档二进制」）：
 * 工具**不把文件投给模型**，而是在设备端把正文抽成纯文本再返回，
 * 因此任何模型（无论是否支持文件类输入）都能理解结果——这就是格式判断的落点：
 * 统一走本机抽取，而不是依赖上游端点的文件解析能力。
 *
 * 何时调用（与 [com.xlin.travel.chat_screen.withAttachmentHints] 配合）：
 * 纯文本类（txt/md/csv/代码等）已在提示词里直接内联、无需调用本工具；
 * 只有消息中出现「文档本地路径 + 让调用 document_read」的系统提示时（Word/PPT/PDF 等），
 * 模型才发起调用。可解析的格式返回正文，暂不可解析的返回友好提示，交由模型向用户说明。
 */
object DocumentReadTool : Tool<DocumentReadTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "document_read",
    description = "阅读用户附带的文档文件（Word/PPT/PDF 等），在本机抽取并返回其纯文本正文。"
        + "documentPath 必须是设备本地绝对路径；仅当消息里给出了文档路径的系统提示、且需要其内容时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("文档的本地绝对路径，例如 /data/user/0/com.xlin.travel/cache/files/xxx.docx")
        val documentPath: String,
    ) {
        init {
            require(documentPath.isNotBlank()) { "documentPath must not be blank" }
        }
    }

    override suspend fun execute(args: Args): String {
        val name = args.documentPath.substringAfterLast('/', args.documentPath)
        val extension = DocumentFormats.extensionOf(name)
        // 本机抽取：Text/Docx 在 Android/JVM 可成功；iOS 的 docx 及 pdf/ppt 等返回 null
        val body = runCatching { extractDocumentText(args.documentPath, extension) }.getOrNull()
        return if (body.isNullOrBlank()) {
            "（文档《$name》暂无法在本机解析出正文，可能是不支持的格式或平台限制；" +
                "请提示用户改粘贴文本内容。）"
        } else {
            buildString {
                append("【文档《").append(name).append("》正文】\n")
                append(body)
            }
        }
    }
}
