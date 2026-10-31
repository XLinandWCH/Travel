package com.xlin.travel.videoexplain.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Web 端：Compose 的 Canvas 画布无法内嵌 <video> 元素，用封面 + 进度占位。
 * 真正的视频播放只在 Android / iOS 上生效。
 */
@Composable
actual fun ExplainVideoPlayer(playing: Boolean, modifier: Modifier) {
    VideoPlayerPlaceholder(modifier)
}
