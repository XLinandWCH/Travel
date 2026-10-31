package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenSpots
import kotlinx.serialization.Serializable

/**
 * 景点推荐工具：小程序「@景点推荐」的落点。
 *
 * 用户点快捷条后输入框只多出「@景点推荐」几个字，具体怎么推荐交给模型：
 * 模型调用本工具拿到点位清单，再按用户的偏好（拍照 / 亲子 / 免费 / 夜游）组织回答。
 * 数据与路线工具同源，避免「推荐的点位」和「路线里的站点」说法不一致。
 */
object SpotRecommendTool : Tool<SpotRecommendTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "spot_recommend",
    description = "获取武汉里份景区的景点推荐清单（看点 / 建议时长 / 门票 / 贴士 / 顺路点位）。"
        + "用户点击「@景点推荐」小程序，或询问有什么值得去、怎么挑景点、某个偏好去哪时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("用户的偏好或筛选条件，例如“拍照”“亲子”“免费”“夜景”；留空返回全部点位")
        val preference: String = "",
    )

    override suspend fun execute(args: Args): String = LifenSpots.recommend(args.preference)
}
