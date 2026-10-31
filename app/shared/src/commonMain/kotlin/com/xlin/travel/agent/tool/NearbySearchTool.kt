package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenNearby
import kotlinx.serialization.Serializable

/**
 * 周边查询工具：小程序「@周边查询」的落点。
 *
 * 回答的是「最近的地铁 / 厕所 / 药店在哪」这类现场问题，
 * 每条都带距离与参照点，模型可以据此给出具体走法，而不是只说“附近有”。
 */
object NearbySearchTool : Tool<NearbySearchTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "nearby_search",
    description = "查询武汉里份景区周边配套：地铁、停车、餐饮、便利店、药店、厕所、寄存、医疗。"
        + "用户点击「@周边查询」小程序，或询问附近有什么、洗手间/停车场/药店在哪时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("要查的类别或地点，例如“地铁”“厕所”“停车”“药店”；留空返回全部配套")
        val category: String = "",
    )

    override suspend fun execute(args: Args): String = LifenNearby.answer(args.category)
}
