package com.xlin.travel.videoexplain.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * JVM（桌面调试）端：没有内嵌系统播放器，用封面 + 进度占位。
 * 真正的视频播放只在 Android / iOS 上生效。
 */
@Composable
actual fun ExplainVideoPlayer(playing: Boolean, modifier: Modifier) {
    VideoPlayerPlaceholder(modifier)
}
