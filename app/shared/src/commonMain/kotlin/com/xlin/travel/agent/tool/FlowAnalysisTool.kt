package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenFlow
import kotlinx.serialization.Serializable

/**
 * 客流分析工具：小程序「@客流分析」的落点。
 *
 * 输出分时段拥挤度（低 / 中 / 高 / 高峰）而不是一句“建议错峰”，
 * 让模型能明确回答“现在去人多不多”“几点来最合适”。
 */
object FlowAnalysisTool : Tool<FlowAnalysisTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "flow_analysis",
    description = "获取武汉里份景区的客流分析：08:00–22:00 分时段拥挤度、最挤的点位与错峰建议。"
        + "用户点击「@客流分析」小程序，或询问人多不多、什么时候去不挤时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("目标时段或场景，例如“10 点”“下午”“周末”“晚上”；留空返回全天曲线")
        val time: String = "",
    )

    override suspend fun execute(args: Args): String = LifenFlow.answer(args.time)
}
