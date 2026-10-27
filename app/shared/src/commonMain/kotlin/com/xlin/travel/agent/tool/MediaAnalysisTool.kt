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
 * 文档（PDF/Word 等）不走本工具：上游 OpenAI 兼容端点会拒收文档二进制
 * （"data did not match any variant of untagged enum MessageContent"），
 * 改由设备端抽取正文、以纯文本内联进提示词（见 ChatState.withAttachmentHints）。
 */
object MediaAnalysisTool : Tool<MediaAnalysisTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "media_analysis",
    description = "识别并分析用户提供的图片：读取 imagePath 指向的图片，"
        + "按 instruction 进行理解、提取、对比或总结，返回文本结果。图片路径必须是设备本地绝对路径。",
) {

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
        // 主 Agent 运行前已拉取云端配置（ModelConfigStore.ensure），这里直接取内存快照，不再硬编码密钥
        val config = ModelConfigStore.current()
            ?: error("模型配置尚未加载，无法识别图片，请重新发送")
        val prompt = prompt("mixed_content") {
            system("You are a helpful assistant skilled at reading images.")
            user {
                +args.instruction
                image(Path(args.imagePath))
            }
        }

        // 非流式：直接执行并取回完整文本结果
        val response = createChatExecutor(config).execute(prompt, mediaVisionModel(config.modelId))
        return response.textContent().ifBlank { "（未能识别出有效内容，请确认图片路径是否正确）" }
    }
}
