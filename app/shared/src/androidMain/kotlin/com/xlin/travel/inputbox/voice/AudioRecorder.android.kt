package com.xlin.travel.inputbox.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/** 采样率 / 单声道 / 16bit，与 Fun-ASR parameters 约定一致 */
private const val SAMPLE_RATE = 16000

/**
 * Android 录音：AudioRecord 采集 16kHz 单声道 16bit PCM，独立线程读取并累计字节，
 * 同时按每帧 RMS 计算瞬时响度供波形使用；[stop] 时补 WAV 头返回完整 WAV。
 *
 * 无需 Context，权限由 [rememberMicPermission] 先行申请；未授权时 AudioRecord 初始化失败，
 * [start] 直接返回 false。
 */
actual class AudioRecorder actual constructor() {

    @Volatile private var running = false
    @Volatile private var currentLevel = 0f
    private var record: AudioRecord? = null
    private var thread: Thread? = null
    private val pcm = ByteArrayOutputStream()

    actual fun start(): Boolean {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) return false
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf, SAMPLE_RATE), // 至少 1 秒缓冲，降低读空风险
            )
        } catch (_: SecurityException) {
            return false
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return false
        }
        pcm.reset()
        currentLevel = 0f
        record = rec
        running = true
        rec.startRecording()
        thread = Thread { readLoop(rec) }.apply { start() }
        return true
    }

    private fun readLoop(rec: AudioRecord) {
        val buf = ShortArray(1024)
        while (running) {
            val n = rec.read(buf, 0, buf.size)
            if (n > 0) {
                // 累计小端 16bit PCM 字节
                for (i in 0 until n) {
                    pcm.write(buf[i].toInt() and 0xFF)
                    pcm.write((buf[i].toInt() shr 8) and 0xFF)
                }
                // RMS → 归一化响度（轻度增益放大，贴近人眼对波形的预期）
                var sum = 0.0
                for (i in 0 until n) sum += buf[i].toDouble() * buf[i].toDouble()
                val rms = sqrt(sum / n) / Short.MAX_VALUE
                currentLevel = (rms * 4f).toFloat().coerceIn(0f, 1f)
            } else if (n < 0) {
                break
            }
        }
    }

    actual fun level(): Float = currentLevel

    actual fun stop(): ByteArray {
        running = false
        runCatching { thread?.join(200) }
        thread = null
        record?.let {
            runCatching { it.stop() }
            it.release()
        }
        record = null
        currentLevel = 0f
        val data = pcm.toByteArray()
        pcm.reset()
        return if (data.isEmpty()) ByteArray(0) else toWav(data, SAMPLE_RATE)
    }

    actual fun release() {
        stop()
    }
}

/** 为 PCM 数据加 44 字节 WAV 头（RIFF/WAVE，PCM，单声道，16bit） */
private fun toWav(pcm: ByteArray, sampleRate: Int): ByteArray {
    val channels = 1
    val bitsPerSample = 16
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val dataLen = pcm.size
    val out = ByteArrayOutputStream(44 + dataLen)
    out.writeStr("RIFF")
    out.writeInt32(36 + dataLen)
    out.writeStr("WAVE")
    out.writeStr("fmt ")
    out.writeInt32(16)                    // fmt 块大小
    out.writeInt16(1)                     // PCM 格式
    out.writeInt16(channels)
    out.writeInt32(sampleRate)
    out.writeInt32(byteRate)
    out.writeInt16(channels * bitsPerSample / 8) // block align
    out.writeInt16(bitsPerSample)
    out.writeStr("data")
    out.writeInt32(dataLen)
    out.write(pcm)
    return out.toByteArray()
}

private fun ByteArrayOutputStream.writeStr(s: String) {
    for (c in s) write(c.code and 0xFF)
}

private fun ByteArrayOutputStream.writeInt32(v: Int) {
    write(v and 0xFF)
    write((v shr 8) and 0xFF)
    write((v shr 16) and 0xFF)
    write((v shr 24) and 0xFF)
}

private fun ByteArrayOutputStream.writeInt16(v: Int) {
    write(v and 0xFF)
    write((v shr 8) and 0xFF)
}
