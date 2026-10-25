package com.xlin.travel.inputbox.voice

import com.xlin.travel.io.readFileBytes
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVLinearPCMBitDepthKey
import platform.AVFAudio.AVLinearPCMIsBigEndianKey
import platform.AVFAudio.AVLinearPCMIsFloatKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID

/**
 * iOS 录音：AVAudioRecorder 直接落 16kHz / 单声道 / 16bit 线性 PCM 的 WAV 临时文件，
 * [stop] 时用 POSIX stdio 读回字节（iOS 26 的 Foundation 已被 Swift 重写，
 * NSData.dataWithContentsOfFile / getBytes 在 Kotlin/Native 中不可解析）；
 * [level] 用 averagePowerForChannel 的 dB 值归一化，驱动真实波形。
 * 权限由 [rememberMicPermission]（AVAudioSession）先行申请。
 *
 * iOS 26 / Kotlin 2.4 interop 注意（CI 实测）：
 * - 构造器 settings 参数类型是 Map<Any?, *>，Kotlin Map 对键不变，mapOf 字面量
 *   必须显式标注类型，否则 "None of the following candidates is applicable"；
 * - ObjC 对象由 ARC 管理，没有 dispose()，置空引用即可；
 * - removeItemAtPath 需要额外的 error 实参（传 null）。
 */
@OptIn(ExperimentalForeignApi::class)
actual class AudioRecorder actual constructor() {

    private var recorder: AVAudioRecorder? = null
    private var filePath: String? = null

    actual fun start(): Boolean {
        val path = NSTemporaryDirectory() + "rec_${NSUUID().UUIDString}.wav"
        val url = NSURL.fileURLWithPath(path) ?: return false
        // kAudioFormatLinearPCM = 'lpcm' (0x6C70636D)，输出未压缩 PCM 便于加 WAV 头
        val settings: Map<Any?, *> = mapOf<Any?, Any?>(
            AVFormatIDKey to 1819304813L,
            AVSampleRateKey to 16000.0,
            AVNumberOfChannelsKey to 1L,
            AVLinearPCMBitDepthKey to 16L,
            AVLinearPCMIsFloatKey to false,
            AVLinearPCMIsBigEndianKey to false,
        )
        val rec = AVAudioRecorder(url, settings, null) ?: return false
        rec.meteringEnabled = true
        if (!rec.prepareToRecord() || !rec.record()) {
            recorder = null
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
        // 注意：common/native stdlib 没有 ByteArray?.orEmpty() 特化重载，用 ?: 代替
        val bytes = filePath?.let { readFileBytes(it) } ?: ByteArray(0)
        recorder = null
        filePath = null
        return bytes
    }

    actual fun release() {
        recorder?.let { runCatching { it.stop() } }
        recorder = null
        filePath?.let { path ->
            runCatching { NSFileManager.defaultManager.removeItemAtPath(path, null) }
        }
        filePath = null
    }
}
