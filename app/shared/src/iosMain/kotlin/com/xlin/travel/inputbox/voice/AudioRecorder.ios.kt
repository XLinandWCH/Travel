package com.xlin.travel.inputbox.voice

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVLinearPCMBitDepthKey
import platform.AVFAudio.AVLinearPCMIsBigEndianKey
import platform.AVFAudio.AVLinearPCMIsFloatKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID

/**
 * iOS 录音：AVAudioRecorder 直接落 16kHz / 单声道 / 16bit 线性 PCM 的 WAV 临时文件，
 * [stop] 时读回字节；[level] 用 averagePower(forChannel:) 的 dB 值归一化，驱动真实波形。
 * 权限由 [rememberMicPermission]（AVAudioSession）先行申请。
 *
 * 注意：本文件仅在 iOS 目标编译，当前开发机（非 Apple 芯片）无法 native 编译 iOS。
 * 若 AVAudioRecorder 初始化 / 属性方法签名在真机 SDK 上有出入，请对照 Xcode 报错微调
 * （与 ImagePicker.ios.kt 同样的处理方式）。
 */
@OptIn(ExperimentalForeignApi::class)
actual class AudioRecorder actual constructor() {

    private var recorder: AVAudioRecorder? = null
    private var filePath: String? = null

    actual fun start(): Boolean {
        val path = NSTemporaryDirectory() + "rec_${NSUUID().UUIDString}.wav"
        val url = NSURL.fileURLWithPath(path)
        // kAudioFormatLinearPCM = 'lpcm' (0x6C70636D)，输出未压缩 PCM 便于加 WAV 头
        val settings = mapOf(
            AVFormatIDKey to 1819304813L,
            AVSampleRateKey to 16000.0,
            AVNumberOfChannelsKey to 1L,
            AVLinearPCMBitDepthKey to 16L,
            AVLinearPCMIsFloatKey to false,
            AVLinearPCMIsBigEndianKey to false,
        )
        val rec = AVAudioRecorder(url, settings, null) ?: return false
        rec.settings = settings
        rec.meteringEnabled = true
        if (!rec.prepareToRecord() || !rec.record()) {
            rec.dispose()
            return false
        }
        recorder = rec
        filePath = path
        return true
    }

    actual fun level(): Float {
        val rec = recorder ?: return 0f
        rec.updateMeters()
        // averagePower 范围约 -160..0 dB；以 -50dB 为静音基线线性归一化
        val db = rec.averagePowerForChannel(0u)
        return ((db + 50f) / 50f).coerceIn(0f, 1f)
    }

    actual fun stop(): ByteArray {
        val rec = recorder ?: return ByteArray(0)
        rec.stop()
        val bytes = filePath?.let { NSData.dataWithContentsOfFile(it)?.toByteArray() }.orEmpty()
        rec.dispose()
        recorder = null
        filePath = null
        return bytes
    }

    actual fun release() {
        recorder?.let {
            runCatching { it.stop() }
            it.dispose()
        }
        filePath?.let { runCatching { NSFileManager.defaultManager.removeItemAtPath(it) } }
        recorder = null
        filePath = null
    }
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) getBytes(bytes.refTo(0), length)
    return bytes
}
