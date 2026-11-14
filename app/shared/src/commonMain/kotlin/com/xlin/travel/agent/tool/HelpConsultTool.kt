package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenHelp
import kotlinx.serialization.Serializable

/**
 * 求助咨询工具：小程序「@求助咨询」的落点。
 *
 * 现场求助有时效性：工具把「先打哪个电话、再去哪个服务点」写在最前面，
 * 模型回答时按顺序复述即可，不要自行补充未登记的救援电话。
 */
object HelpConsultTool : Tool<HelpConsultTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "help_consult",
    description = "获取武汉里份景区的求助与应急信息：服务点位置、医疗点、走失、失物、投诉、无障碍、紧急电话。"
        + "用户点击「@求助咨询」小程序，或遇到困难需要求助、想找人帮忙时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("遇到的问题，例如“走失”“受伤”“丢东西”“投诉”“无障碍”；留空返回全部指引")
        val issue: String = "",
    )

    override suspend fun execute(args: Args): String = LifenHelp.answer(args.issue)
}
