package com.xlin.travel.videoexplain.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.UIKit.UIView
import travel.app.shared.generated.resources.Res

/** 临时目录里的缓存文件名：带版本后缀，换视频时改后缀即可失效旧缓存 */
private const val CACHE_NAME = "explain_video_v1.mp4"

/**
 * iOS 端播放：AVPlayer + AVPlayerLayer 内嵌在 UIKitView 里。
 * Compose Resources 没有直接给文件路径的稳定接口，这里读字节写进临时目录再交给 AVPlayer；
 * 播放结束通过通知 seek 回 0，实现静音循环。
 */
@Composable
actual fun ExplainVideoPlayer(playing: Boolean, modifier: Modifier) {
    var path by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        path = runCatching { cachedVideoPath() }.getOrNull()
    }
    path?.let { ExplainPlayer(path = it, playing = playing, modifier = modifier) }
}

/** 首次进入把视频字节落成临时文件，之后直接复用 */
private suspend fun cachedVideoPath(): String {
    val path = NSTemporaryDirectory() + CACHE_NAME
    if (NSFileManager.defaultManager.fileExistsAtPath(path)) return path
    val bytes = Res.readBytes(EXPLAIN_VIDEO_PATH)
    val data = bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
    data.writeToFile(path, atomically = true)
    return path
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun ExplainPlayer(path: String, playing: Boolean, modifier: Modifier) {
    // 与 Android 一致：静音播放，避免一进页就突然出声
    val player = remember(path) {
        AVPlayer(uRL = NSURL.fileURLWithPath(path)).apply { volume = 0f }
    }

    DisposableEffect(player) {
        val center = NSNotificationCenter.defaultCenter
        // 播放结束就回到起点重新播，做无缝循环
        val token = center.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = player.currentItem,
            queue = NSOperationQueue.mainQueue,
        ) {
            player.seekToTime(CMTimeMake(value = 0, timescale = 1))
            player.play()
        }
        onDispose {
            center.removeObserver(token)
            player.pause()
            player.replaceCurrentItemWithPlayerItem(null)
        }
    }
    LaunchedEffect(playing) {
        if (playing) player.play() else player.pause()
    }

    UIKitView(
        factory = { PlayerLayerView(player) },
        modifier = modifier,
    )
}

/** 让 playerLayer 始终铺满自身 bounds，避免旋转 / 尺寸变化后画面停在旧大小 */
@OptIn(ExperimentalForeignApi::class)
private class PlayerLayerView(player: AVPlayer) :
    UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {

    private val playerLayer = AVPlayerLayer.playerLayerWithPlayer(player)

    init {
        playerLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        playerLayer.frame = bounds
        CATransaction.commit()
    }
}
