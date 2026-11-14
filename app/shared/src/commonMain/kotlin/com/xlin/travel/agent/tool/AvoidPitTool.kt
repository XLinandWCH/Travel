package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenPitfalls
import kotlinx.serialization.Serializable

/**
 * 避坑指南工具：小程序「@避坑指南」的落点。
 *
 * 每条都是「场景 + 风险 + 做法」三段式，模型可以直接复述给用户，
 * 不用自己猜哪些坑真的存在——这类内容编错比不说更糟。
 */
object AvoidPitTool : Tool<AvoidPitTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "avoid_pit",
    description = "获取武汉里份景区的避坑指南：闭馆日、排队、黄牛票、消费陷阱、江滩安全、居民区礼仪。"
        + "用户点击「@避坑指南」小程序，或询问有什么要注意的坑、会不会被坑时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("关心的场景，例如“门票”“拍照”“游船”“吃饭”；留空返回全部条目")
        val scene: String = "",
    )

    override suspend fun execute(args: Args): String = LifenPitfalls.answer(args.scene)
}
