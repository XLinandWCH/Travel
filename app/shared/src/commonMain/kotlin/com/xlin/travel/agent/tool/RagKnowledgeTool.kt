package com.xlin.travel.agent.tool

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.xlin.travel.agent.tool.kb.LifenKnowledge
import kotlinx.serialization.Serializable

/**
 * RAG 知识库检索工具：名字里带 RAG，实现上是「目录 + 关键词段落」的假检索。
 *
 * 为什么不做真 RAG：知识库是随包发布的二十几章说明文档，规模固定、更新随发版，
 * 引入向量库（切块、嵌入、存储、跨端一致性）成本远高于收益；
 * 真正要解决的是上下文爆炸，所以先用目录收敛范围，再只取命中段落。
 */
object RagKnowledgeTool : Tool<RagKnowledgeTool.Args, String>(
    argsType = typeToken<Args>(),
    resultType = typeToken<String>(),
    name = "rag_knowledge",
    description = "检索随包发布的《武汉里份》知识库（离线写死，不是实时数据）：里份历史、建筑特色、"
        + "咸安坊／同兴里／泰兴里／延庆里等具体里份、游玩攻略、推荐路线、交通、美食、摄影、名人故居、保护与改造。"
        + "用法固定两步：先不带参数调用一次拿到章节目录，再按目录带 chapter（章节号如 04）+ query 取具体段落；"
        + "只有需要完整背景时才留空 query 读整章（会截断）。用户问武汉里份相关内容时先查这里，再结合其它工具作答。",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("章节号或标题，如 “04” / “04-咸安坊” / “美食推荐”；留空返回章节目录")
        val chapter: String = "",
        @property:LLMDescription("要检索的具体问题或关键词，如 “咸安坊 门票 开放时间”；留空返回整章（可能截断）")
        val query: String = "",
    )

    override suspend fun execute(args: Args): String =
        LifenKnowledge.answer(args.chapter, args.query)
}
