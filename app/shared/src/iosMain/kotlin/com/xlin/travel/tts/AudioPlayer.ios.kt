package com.xlin.travel.tts

import com.xlin.travel.io.writeFileBytes
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID

/**
 * iOS 播放：mp3 字节先经 POSIX stdio 写临时文件（iOS 26 Foundation 被 Swift 重写，
 * NSData.writeToFile 在 K/N 不可用），再交 AVAudioPlayer 播放。
 *
 * 播放前把 AVAudioSession 切到 Playback 类别：静音拨键下也能从扬声器出声，
 * 且与录音（Record 类别）互斥的场景由上层「朗读先停录音」顺序保证。
 * 注意：ObjC 对象由 ARC 管理，停止 / 释放只需置空引用；removeItemAtPath 需传 null error。
 */
@OptIn(ExperimentalForeignApi::class)
actual class AudioPlayer actual constructor() {

    private var player: AVAudioPlayer? = null
    private var filePath: String? = null

    actual fun play(bytes: ByteArray) {
        stop()
        if (bytes.isEmpty()) return
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        }
        val path = NSTemporaryDirectory() + "tts_${NSUUID().UUIDString}.mp3"
        if (!writeFileBytes(path, bytes)) return
        val url = NSURL.fileURLWithPath(path) ?: return
        val p = AVAudioPlayer(url, null)
        if (p == null || !p.prepareToPlay() || !p.play()) {
            runCatching { NSFileManager.defaultManager.removeItemAtPath(path, null) }
            return
        }
        player = p
        filePath = path
    }

    actual fun isPlaying(): Boolean = player?.playing == true

    actual fun stop() {
        player?.let { runCatching { it.stop() } }
        player = null
        filePath?.let { path ->
            runCatching { NSFileManager.defaultManager.removeItemAtPath(path, null) }
        }
        filePath = null
    }
}
