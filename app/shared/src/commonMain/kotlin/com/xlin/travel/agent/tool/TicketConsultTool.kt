package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenTickets
import kotlinx.serialization.Serializable

/**
 * 门票咨询工具：小程序「@门票咨询」的落点。
 *
 * 与「景点推荐」里的门票字段同口径，但这里额外给出预约方式与闭馆日，
 * 因为用户问“多少钱”时往往紧接着问“怎么约、周一开不开”。
 */
object TicketConsultTool : Tool<TicketConsultTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "ticket_consult",
    description = "获取武汉里份景区的票务信息：各点位门票、是否免费、预约方式、闭馆日、优惠与代步票。"
        + "用户点击「@门票咨询」小程序，或询问门票多少钱、要不要预约、周一开不开时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("景点或项目名称，例如“唐华里”“江汉关”“夜游”；留空返回全部票务")
        val spotName: String = "",
    )

    override suspend fun execute(args: Args): String = LifenTickets.answer(args.spotName)
}
