package com.xlin.travel.inputbox.voice

/**
 * 平台录音器（expect / actual），与 image_picker 采用相同的组织方式。
 *
 * 采集 16kHz / 单声道 / 16bit PCM，[stop] 时封装为标准 WAV 字节返回，
 * 供 [SpeechToTextClient] 以 Base64 Data URI 提交给 Fun-ASR-Flash。
 *
 * 生命周期：[start] 开始采集 → 采集期间高频轮询 [level] 取实时响度驱动波形
 *  → [stop] 停止并返回 WAV 字节 → [release] 释放资源（取消时也要调用）。
 *
 * [start] 返回 false 表示权限缺失或硬件不可用；调用方应先走麦克风权限流程。
 * Android 用 AudioRecord 读 PCM 并计算 RMS，iOS 用 AVAudioRecorder 落 WAV 并读平均功率。
 */
expect class AudioRecorder() {
    /** 开始录音，成功返回 true；无权限 / 设备不可用返回 false */
    fun start(): Boolean

    /** 当前瞬时响度，归一化到 0f（静音）.. 1f（很大声）；未在录音时返回 0f */
    fun level(): Float

    /** 停止录音并返回 WAV 字节（16kHz 单声道 16bit）；无数据时返回空数组 */
    fun stop(): ByteArray

    /** 释放底层资源，可重复调用 */
    fun release()
}
