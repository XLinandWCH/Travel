package com.xlin.travel.videoexplain

/**
 * 讲解画面的三种状态：
 * [Camera] 摄像头预览（进页面默认）→ [Generating] 生成动画 → [Video] 播放讲解视频。
 */
internal enum class ExplainMode { Camera, Generating, Video }

/** 生成动画时长（毫秒）：走满 3 秒后自动把摄像头画面换成视频 */
internal const val ExplainGeneratingMillis = 3000L

/** 摄像头默认字幕（用户还没说话时展示的讲解词） */
internal const val CameraDefaultSubtitle = "这里是咸安坊：1930 年代的红砖里份，傍晚的斜光打在清水墙上最好看。"

/** 视频默认字幕（生成的讲解视频配套解说词） */
internal const val VideoDefaultSubtitle = "这是为你生成的讲解视频：从江汉路拐进咸安坊，红砖墙、过街楼和晾在天井里的衣服。"

/**
 * 语音里是否在要求「生成视频」。
 *
 * 识别结果常常没有标点、甚至多出语气词，先把空白与标点全部去掉再匹配核心词，
 * 这样「可以帮我生成视频吗？」「帮我生成讲解视频」都能命中。
 */
internal fun asksForExplainVideo(text: String): Boolean {
    val core = text.filter { it.isLetterOrDigit() }
    if (core.isEmpty()) return false
    return core.contains("生成视频") || core.contains("生成讲解视频")
}
