package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenNarration
import kotlinx.serialization.Serializable

/**
 * 景点讲解工具：小程序「@景点讲解」的落点。
 *
 * 返回的是可以直接念出来的讲稿 + 一条冷知识，模型只做语气与衔接，
 * 年代、人物、桥名等硬信息全部锁死在工具结果里，避免现场讲解张冠李戴。
 */
object SpotGuideTool : Tool<SpotGuideTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "spot_guide",
    description = "获取武汉里份景区的景点讲解词：单个点位的历史背景、看点与冷知识，可直接讲给用户听。"
        + "用户点击「@景点讲解」小程序，或要求讲讲某个点位、想听故事时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("要讲解的景点名称，例如“咸安坊”“江汉关”“巴公房子”；留空返回全部讲稿")
        val spotName: String = "",
    )

    override suspend fun execute(args: Args): String = LifenNarration.answer(args.spotName)
}
