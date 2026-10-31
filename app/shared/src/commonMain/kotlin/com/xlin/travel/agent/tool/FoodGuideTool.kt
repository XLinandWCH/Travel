package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.lifen.LifenFoods
import kotlinx.serialization.Serializable

/**
 * 美食攻略工具：小程序「@美食攻略」的落点。
 *
 * 给的是一天从早到晚的具体店与时段（早市过点就吃不到），
 * 而不是泛泛列菜名——这类工具的价值就在于“现在几点该吃什么”。
 */
object FoodGuideTool : Tool<FoodGuideTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "food_guide",
    description = "获取武汉里份景区的美食攻略：热干面、豆皮、藕汤、糊汤粉、面窝、鸭脖、吉庆街夜宵的店铺/价格/时段/点单建议。"
        + "用户点击「@美食攻略」小程序，或询问吃什么、哪家好吃、早餐去哪吃时调用。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("口味或场景，例如“早餐”“夜宵”“清淡”“小吃”“两人正餐”；留空返回全部推荐")
        val preference: String = "",
    )

    override suspend fun execute(args: Args): String = LifenFoods.answer(args.preference)
}
