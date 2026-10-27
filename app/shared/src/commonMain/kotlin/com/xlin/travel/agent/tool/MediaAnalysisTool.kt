package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.ModelConfigStore
import com.xlin.travel.agent.createChatExecutor
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable

/** 图片识别专用模型：在云端下发的模型 id 基础上补充视觉输入能力（文档不再走本工具，改走设备端正文内联）。 */
private fun mediaVisionModel(modelId: String): LLModel = LLModel(
    provider = LLMProvider.OpenAI,
    id = modelId,
    capabilities = listOf(
        LLMCapability.Completion,
        LLMCapability.Temperature,
        LLMCapability.OpenAIEndpoint.Completions,
        LLMCapability.Vision.Image,
    ),
    contextLength = 32_000,
)

/**
 * 图片识别工具（只处理图片）。
 *
 * 给定本地绝对路径的图片，用一次性（非流式）的多模态 prompt 让模型读取并分析，
 * 把结果作为字符串返回给主 Agent，再由主 Agent 组织最终回答。
 * 参考 koog「mixed content」用法：user { +文本; image(Path) }。
 *
 * 格式判断：本工具只接受图片扩展名；若模型误传文档（docx/ppt/pdf 等），直接提示改走
 * [com.xlin.travel.agent.tool.DocumentReadTool]（它在本机抽取正文，不依赖模型能否接收文件二进制）。
 * 识别过程异常时不再抛出中断整轮，而是返回可读文本，上层会展示为「调用识图工具（未完成）」卡片。
 */
object MediaAnalysisTool : Tool<MediaAnalysisTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "media_analysis",
    description = "识别并分析用户提供的图片：读取 imagePath 指向的图片，"
        + "按 instruction 进行理解、提取、对比或总结，返回文本结果。图片路径必须是设备本地绝对路径。",
) {

    /** 允许的图片扩展名（输入图片已由 image_processor 规范化为 jpeg/png） */
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    @Serializable
    data class Args(
        @property:LLMDescription("对识别结果的具体要求，例如“把图中信息整理成表格”")
        val instruction: String,
        @property:LLMDescription("图片的本地绝对路径，例如 /data/user/0/com.xlin.travel/cache/images/xxx.jpg")
        val imagePath: String,
    ) {
        init {
            require(instruction.isNotBlank()) { "instruction must not be blank" }
            require(imagePath.isNotBlank()) { "imagePath must not be blank" }
        }
    }

    override suspend fun execute(args: Args): String {
        // 格式判断：非图片路径不当视觉输入发出（避免端点拒收），改引导到阅读工具
        val ext = args.imagePath.substringAfterLast('.', "").lowercase()
        if (ext.isNotEmpty() && ext !in imageExtensions) {
            return "（media_analysis 只处理图片；当前路径不是图片格式（.$ext）。若是文档请改用 document_read 工具。）"
        }

        // 主 Agent 运行前已拉取云端配置（ModelConfigStore.ensure），这里直接取内存快照，不再硬编码密钥
        val config = ModelConfigStore.current()
            ?: return "（模型配置尚未加载，无法识别图片，请重新发送）"

        // 非流式 + 容错：任何异常（网络 / 端点不接受图片输入等）都降级为可读文本，不中断整轮
        return runCatching {
            val prompt = prompt("mixed_content") {
                system("You are a helpful assistant skilled at reading images.")
                user {
                    +args.instruction
                    image(Path(args.imagePath))
                }
            }
            createChatExecutor(config).execute(prompt, mediaVisionModel(config.modelId))
                .textContent()
        }.getOrElse { t ->
            "（识图失败：${t.message ?: "未知错误"}；若本模型不支持图片输入，请改由用户描述图片内容。）"
        }.ifBlank { "（未能识别出有效内容，请确认图片路径是否正确）" }
    }
}
