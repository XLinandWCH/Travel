package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.serialization.typeToken
import com.xlin.travel.a2ui.A2uiRelay
import com.xlin.travel.a2ui.A2uiSurface

/**
 * A2UI 渲染工具：让模型「选组件 + 填内容」，产出一屏个性化界面。
 *
 * 它不是查数据，而是**排版**——数据必须来自同轮调用的其他工具（路线 / 门票 / 美食…）。
 * 入参直接复用 [A2uiSurface]：字段说明就写在 [com.xlin.travel.a2ui.A2uiBlock] 的
 * LLMDescription 上，避免「工具 schema 一份、渲染模型又一份」两处走样。
 *
 * 结果不在这里返回 JSON：界面结构通过 [A2uiRelay] 交给聊天层挂到消息上，
 * 返回给模型的只有一句确认，既省 token 也防止模型把 JSON 抄进正文。
 */
object A2uiRenderTool : Tool<A2uiSurface, String>(
    argsType = typeToken<A2uiSurface>(),
    resultType = typeToken<String>(),
    name = "a2ui_render",
    description = "把这一轮已经查到的景区内容（路线、门票、美食、演出、避坑等）整理成一屏可交互的个性化界面，"
        + "客户端会在回答下方出现「进入个性化界面」入口。"
        + "只在正文写完之后调用一次，调用后立即结束本轮：不要再输出任何正文、总结或邀请语；"
        + "涉及地点时加一块 image（image=map）保证界面有图，块数 4–7；"
        + "正文里绝不能粘贴 JSON、字段名或组件名，也不要复述界面里的条目。",
) {

    override suspend fun execute(args: A2uiSurface): String {
        val surface = args.copy(
            title = args.title.trim(),
            subtitle = args.subtitle.trim(),
            // 空块与超量块在这里就裁掉：超长界面在手机上只能滚，反而失去「一屏看完」的意义
            blocks = args.blocks.filterNot { it.isEmpty }.take(MaxBlocks),
        )
        if (surface.isEmpty) {
            return "（本次没有可展示的界面：请先调用对应的小程序工具拿到真实数据，再按契约重新组织 blocks。）"
        }
        A2uiRelay.publish(surface)
        // 返回值不能再引导模型「总结一句」：那会在入口下方再长出一段正文
        return "界面已展示给用户，本轮到此结束：不要再输出任何正文、总结或邀请语——" +
            "正文已经写过，用户已经在下方看到「进入个性化界面」入口。"
    }

    /** 一屏最多块数：与 [com.xlin.travel.a2ui.A2uiCatalog.contract] 的口径保持一致 */
    private const val MaxBlocks = 8
}
