package com.xlin.travel.chat_screen

/**
 * 工具名 → 用户可读的展示名（只展这几个字，不拼参数 / 结果）；未知工具回退为通用文案。
 * 12 个小程序各自对应一个工具，用户在工具卡片上能直接看出 AI 查了哪一类景区数据。
 *
 * 单独成文件：AssistantRound 只负责一轮回复的流程，展示文案放这里更好改。
 */
internal fun toolDisplayName(toolName: String): String = when (toolName) {
    "a2ui_render" -> "生成个性化界面"
    "media_analysis" -> "调用识图工具"
    "document_read" -> "调用阅读工具"
    "route_plan" -> "调用路线规划工具"
    "spot_recommend" -> "调用景点推荐工具"
    "nearby_search" -> "调用周边查询工具"
    "avoid_pit" -> "调用避坑指南工具"
    "show_time" -> "调用表演时间工具"
    "help_consult" -> "调用求助咨询工具"
    "ticket_consult" -> "调用门票咨询工具"
    "food_guide" -> "调用美食攻略工具"
    "traffic_guide" -> "调用交通出行工具"
    "spot_guide" -> "调用景点讲解工具"
    "flow_analysis" -> "调用客流分析工具"
    "check_in" -> "调用打卡分享工具"
    "rag_knowledge" -> "检索里份知识库"
    else -> "调用工具"
}
