package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.createChatExecutor
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable

/** 图片 / 文件识别专用模型：在旅行主模型能力基础上补充视觉与文档输入能力。 */
private val MediaVisionModel = LLModel(
    provider = LLMProvider.OpenAI,
    id = "Sage-A1.5",
    capabilities = listOf(
        LLMCapability.Completion,
        LLMCapability.Temperature,
        LLMCapability.OpenAIEndpoint.Completions,
        LLMCapability.Vision.Image,
        LLMCapability.Document,
    ),
    contextLength = 32_000,
)

/**
 * 图片与文件识别工具（取代原来的两位数加法计算器）。
 *
 * 给定本地绝对路径的图片 / 文档，用一次性（非流式）的多模态 prompt 让模型读取并分析，
 * 把结果作为字符串返回给主 Agent，再由主 Agent 组织最终回答。
 * 参考 koog「mixed content」用法：user { +文本; image(Path); binaryFile(Path, mime) }。
 */
object MediaAnalysisTool : Tool<MediaAnalysisTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "media_analysis",
    description = "识别并分析用户提供的图片与文档：读取 imagePath 指向的图片、documentPath 指向的文件，" +
        "按 instruction 进行理解、提取、对比或总结，返回文本结果。图片/文件路径必须是设备本地绝对路径。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("对识别结果的具体要求，例如“把图中信息与文档内容对比成表格”")
        val instruction: String,
        @property:LLMDescription("图片的本地绝对路径，例如 /data/user/0/com.xlin.travel/cache/images/xxx.jpg；没有图片时留空")
        val imagePath: String = "",
        @property:LLMDescription("文档（如 PDF）的本地绝对路径；没有文档时留空")
        val documentPath: String = "",
        @property:LLMDescription("文档的 MIME 类型，例如 application/pdf、application/msword")
        val documentMime: String = "application/pdf",
    ) {
        init {
            require(instruction.isNotBlank()) { "instruction must not be blank" }
            require(imagePath.isNotBlank() || documentPath.isNotBlank()) {
                "at least one of imagePath or documentPath must be provided"
            }
        }
    }

    override suspend fun execute(args: Args): String {
        val prompt = prompt("mixed_content") {
            system("You are a helpful assistant skilled at reading images and documents.")
            user {
                +args.instruction
                if (args.imagePath.isNotBlank()) image(Path(args.imagePath))
                if (args.documentPath.isNotBlank()) binaryFile(Path(args.documentPath), args.documentMime)
            }
        }

        // 非流式：直接执行并取回完整文本结果
        val response = createChatExecutor().execute(prompt, MediaVisionModel)
        return response.textContent().ifBlank { "（未能识别出有效内容，请确认图片 / 文件路径是否正确）" }
    }
}
