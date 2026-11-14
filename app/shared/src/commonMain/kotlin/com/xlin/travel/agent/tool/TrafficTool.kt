package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenTraffic
import kotlinx.serialization.Serializable

/**
 * 交通出行工具：小程序「@交通出行」的落点。
 *
 * 覆盖进场（地铁 / 自驾）、串联（步行 / 单车）与离场（轮渡 / 打车）三段，
 * 每段都带用时与花费，模型可以据此直接排时间。
 */
object TrafficTool : Tool<TrafficTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "traffic_guide",
    description = "获取武汉里份景区的交通方案：地铁站点与出口、轮渡过江、自驾停车、打车上下客点、步行串联与共享单车。"
        + "用户点击「@交通出行」小程序，或询问怎么去、怎么回、停车停哪、要不要坐船时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("出发地或交通方式，例如“地铁”“轮渡”“自驾”“从汉口火车站”；留空返回全部方案")
        val from: String = "",
    )

    override suspend fun execute(args: Args): String = LifenTraffic.answer(args.from)
}
