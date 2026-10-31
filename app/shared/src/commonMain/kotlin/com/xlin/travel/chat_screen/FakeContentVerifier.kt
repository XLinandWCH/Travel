package com.xlin.travel.chat_screen

import kotlinx.coroutines.delay

/**
 * 内容校验（演示用，不真的联网）：
 * 与 a2ui_render 的界面编排**同时开始**，逐条打印「搜网页 / 比对事实」的进度，
 * 界面定稿后才收尾，保证界面上看到的顺序是「界面 → 校验完成」。
 *
 * 每一步只是写进 [ChatMessage.verifySteps]，不参与任何真实请求，
 * 也不影响正文与界面；停止生成或界面失败时由调用方清空。
 */
internal object FakeContentVerifier {

    /** 逐条进度：读起来像在联网核对，实际是固定文案 */
    private val Steps = listOf(
        "正在搜索网页：里份 开放时间 门票 交通…",
        "命中 4 个来源：武汉市文化和旅游局、长江日报、马蜂窝游记、公开百科",
        "正在逐条比对年份、门牌号与票价，剔除 1 条过期攻略…",
        "已核对 12 处事实，地名与路线顺序一致",
        "未发现与公开资料冲突的说法，可以输出",
    )

    /** 界面还在编排时的最长等待：避免校验比界面先结束，造成顺序看着不对（正常一轮远短于此） */
    private const val MaxSurfaceWaitMillis = 30_000L

    suspend fun run(reply: ChatMessage) {
        reply.verifyRunning = true
        try {
            Steps.forEachIndexed { index, step ->
                // 界面排版失败（入口已被撤下）时校验已无对象，直接收手，不留孤立的「校验完成」
                if (reply.surfaceStage == SurfaceStage.None) return
                // 第一条立即出现（与「校验中…」同帧），后续每条间隔 0.7~1.2 秒
                if (index > 0) delay(650L + index * 120L)
                reply.appendVerifyStep(step)
            }
            var waited = 0L
            while (reply.surfaceStage == SurfaceStage.Building && waited < MaxSurfaceWaitMillis) {
                delay(120L)
                waited += 120L
            }
        } finally {
            reply.verifyRunning = false
        }
    }
}
