package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenCheckIn
import kotlinx.serialization.Serializable

/**
 * 打卡分享工具：小程序「@打卡分享」的落点。
 *
 * 每条机位都写清「几点拍、站哪里、怎么构图、用什么话题」，
 * 模型可以据此给出可直接照做的拍照建议，而不是“随手一拍都好看”。
 */
object CheckInTool : Tool<CheckInTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "check_in",
    description = "获取武汉里份景区的打卡分享指南：最佳机位、拍摄时间、构图建议与话题标签。"
        + "用户点击「@打卡分享」小程序，或询问哪里拍照好看、怎么拍出片时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("拍摄风格或偏好，例如“建筑”“人像”“夜景”“人少”；留空返回全部机位")
        val style: String = "",
    )

    override suspend fun execute(args: Args): String = LifenCheckIn.answer(args.style)
}
