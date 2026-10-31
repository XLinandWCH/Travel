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
        + "只在正文写完之后调用一次，块数 3–6；不要在正文里粘贴 JSON 或重复界面内容。",
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
        return "已生成个性化界面「${surface.title}」，共 ${surface.blocks.size} 块。" +
            "用户可在正文下方点「进入个性化界面」查看；请不要再复述块内文字，" +
            "用一两句话总结并邀请用户点开或继续追问。"
    }

    /** 一屏最多块数：与 [com.xlin.travel.a2ui.A2uiCatalog.contract] 的口径保持一致 */
    private const val MaxBlocks = 8
}
