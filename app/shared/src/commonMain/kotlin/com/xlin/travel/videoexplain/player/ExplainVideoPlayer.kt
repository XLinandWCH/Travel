package com.xlin.travel.videoexplain.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 讲解视频资源在 CMP 资源目录里的相对路径。
 *
 * 必须放在 composeResources/files/ 下：Compose Resources 只打包 drawable / font /
 * values / files 四类目录，直接放 composeResources/videos/ 不会进包
 * （Res 也就取不到）。来源文件即用户提供的 videos/video_1.mp4。
 */
const val EXPLAIN_VIDEO_PATH = "files/videos/video_1.mp4"

/**
 * 讲解视频播放器（expect / actual），静音循环播放。
 *
 * 各平台自己从 Compose Resources 取视频，不经调用方传地址：
 * - Android：assets 里的文件复制到 cacheDir，MediaPlayer + TextureView 播放
 *   （TextureView 支持圆角裁剪，VideoView 的 SurfaceView 会突破卡片圆角）；
 * - iOS：读资源字节写进临时目录，AVPlayer + AVPlayerLayer（UIKitView 内嵌），
 *   播完 seek 回 0 实现循环；
 * - JVM / Web：Compose 画布内无法内嵌系统播放器，退化为封面 + 播放进度占位。
 *
 * @param playing 是否播放；false 时暂停，离开组合由 onDispose 释放播放器
 */
@Composable
expect fun ExplainVideoPlayer(
    playing: Boolean,
    modifier: Modifier = Modifier,
)
