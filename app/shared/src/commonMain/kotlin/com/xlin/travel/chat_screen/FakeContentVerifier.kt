package com.xlin.travel.chat_screen

import kotlinx.coroutines.delay

/**
 * 内容校验（演示用，不真的联网）：
 * 与 a2ui_render 的界面编排**同时开始**，逐条打印「搜网页 → 命中来源 → 比对事实」的进度，
 * 界面定稿后才收尾，保证界面上看到的顺序是「界面 → 校验完成」。
 *
 * 命中来源不是固定四条：按本轮问题与界面标题里的关键词，从来源表里挑相关的官网，
 * 最多 7 条；一条都没匹配上时退回几个常用站点，不会出现「命中 0 个来源」。
 * 每一步只是写进 [ChatMessage.verifySteps]，不参与任何真实请求，也不影响正文与界面。
 */
internal object FakeContentVerifier {

    /** 一条模拟来源：官网名 + 站点标识 + 用来匹配本轮内容的关键词 */
    private data class Source(val name: String, val site: String, val tags: List<String>)

    private val Sources = listOf(
        Source("武汉市文化和旅游局", "wlj.wuhan.gov.cn", listOf("文旅", "景点", "门票", "开放", "预约", "演出", "展览")),
        Source("武汉市人民政府 · 城市概况", "wuhan.gov.cn", listOf("历史", "里份", "老建筑", "租界", "汉口", "沿革", "简介")),
        Source("湖北省文化和旅游厅", "wlt.hubei.gov.cn", listOf("景区", "文旅", "活动", "线路")),
        Source("长江网", "cjn.cn", listOf("新闻", "改造", "保护", "更新", "规划", "建设")),
        Source("中国天气网", "weather.com.cn", listOf("天气", "气温", "下雨", "阴晴", "季节")),
        Source("武汉地铁", "whrt.gov.cn", listOf("地铁", "交通", "换乘", "公交", "出行")),
        Source("大众点评", "dianping.com", listOf("美食", "小吃", "过早", "热干面", "餐厅", "吃")),
        Source("马蜂窝", "mafengwo.cn", listOf("攻略", "路线", "行程", "游记", "打卡")),
        Source("携程旅行", "ctrip.com", listOf("酒店", "住宿", "机票", "行程")),
        Source("小红书", "xiaohongshu.com", listOf("拍照", "摄影", "机位", "探店", "穿搭")),
        Source("哔哩哔哩", "bilibili.com", listOf("视频", "讲解", "纪录", "探店")),
        Source("湖北省博物馆", "hbsbwg.net", listOf("文物", "编钟", "展品", "博物馆")),
    )

    /** 一条都没匹配上时至少给这几条，读起来才不像「什么都没搜到」 */
    private val FallbackNames = listOf("武汉市文化和旅游局", "武汉市人民政府 · 城市概况", "长江网", "马蜂窝")

    private const val MinSources = 4
    private const val MaxSources = 7

    /** 界面还在编排时的最长等待（正常一轮远短于此） */
    private const val MaxSurfaceWaitMillis = 30_000L

    suspend fun run(reply: ChatMessage, prompt: String) {
        reply.verifyRunning = true
        try {
            reply.appendVerifyStep("正在搜索网页：${searchHint(prompt)}…")
            delay(520L)
            // 界面标题要等 a2ui_render 参数流出一点才有值，这里再取一次当关键词
            val sources = pickSources(prompt + " " + reply.content + " " + reply.surface?.title.orEmpty())
            sources.forEach { source ->
                // 界面排版失败（入口已撤下）时校验已无对象，直接收手
                if (reply.surfaceStage == SurfaceStage.None) return
                delay(300L)
                reply.appendVerifyStep("命中 ${source.name}（${source.site}）")
            }
            delay(520L)
            reply.appendVerifyStep("正在比对年份、门牌号与票价，剔除 1 条过期攻略…")
            delay(560L)
            reply.appendVerifyStep("已核对 ${10 + sources.size * 2} 处事实，地名与路线顺序一致")
            delay(520L)
            reply.appendVerifyStep("未发现与公开资料冲突的说法，可以输出")
            waitForSurface(reply)
        } finally {
            reply.verifyRunning = false
        }
    }

    /** 本轮关键词摘要：去掉 @提及与换行后截断，避免整句问题塞进一行 */
    private fun searchHint(prompt: String): String {
        val cleaned = prompt.replace("@", "").replace(Regex("\\s+"), " ").trim()
        return if (cleaned.isEmpty()) "武汉里份 游玩攻略" else cleaned.take(20)
    }

    /** 按关键词挑来源：命中越多越优先，太少时用兜底站点补齐 */
    private fun pickSources(text: String): List<Source> {
        val picked = Sources.filter { source -> source.tags.any { text.contains(it) } }.toMutableList()
        for (name in FallbackNames) {
            if (picked.size >= MinSources) break
            Sources.firstOrNull { it.name == name && it !in picked }?.let { picked.add(it) }
        }
        return picked.take(MaxSources)
    }

    /** 界面定稿（不再处于 Building）才算校验完成，避免「界面还在编排、校验已完成」的顺序错位 */
    private suspend fun waitForSurface(reply: ChatMessage) {
        var waited = 0L
        while (reply.surfaceStage == SurfaceStage.Building && waited < MaxSurfaceWaitMillis) {
            delay(120L)
            waited += 120L
        }
    }
}
