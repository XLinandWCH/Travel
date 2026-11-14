package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenShows
import kotlinx.serialization.Serializable

/**
 * 表演时间工具：小程序「@表演时间」的落点。
 *
 * 只给场次 / 时长 / 票价 / 购票方式，不承诺“今天一定有”，
 * 场次本身会随季节调整，工具结果末尾已带官方公告提醒。
 */
object ShowTimeTool : Tool<ShowTimeTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "show_time",
    description = "获取武汉里份景区的演出与活动场次：长江灯光秀、江汉关钟声、夜游长江、知音号、汉秀的时间地点票价。"
        + "用户点击「@表演时间」小程序，或询问几点有表演、今晚看什么时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("日期或时段，例如“今晚”“周末”“19 点”；留空返回全部场次")
        val date: String = "",
    )

    override suspend fun execute(args: Args): String = LifenShows.answer(args.date)
}
