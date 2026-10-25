package com.xlin.travel.inputbox.voice

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 语音输入的阶段状态机 */
enum class VoicePhase {
    /** 刚打开，正在申请权限 / 准备录音 */
    Starting,

    /** 正在录音，波形随真实音量起伏 */
    Recording,

    /** 已停止录音，正在调用 ASR 识别 */
    Transcribing,
}

/**
 * 语音输入面板状态：驱动阶段、波形样本与错误提示。
 *
 * [levels] 是一段滚动的时间序列响度（0f..1f），每根柱子对应一个采样，
 * 由录音器实时写入：静音时贴近基线、说话时才鼓起——不是随机动画，
 * 严格反映用户当前有没有在说话（对齐大厂实时波形的设计习惯）。
 */
@Stable
class VoiceInputState {

    /** 波形柱子数量（滚动窗口长度） */
    val barCount = 28

    var phase: VoicePhase by mutableStateOf(VoicePhase.Starting)
        internal set

    /** 识别 / 权限失败时展示给用户的原因；null 表示无错误 */
    var error: String? by mutableStateOf(null)
        internal set

    /** 松手后是否直接把识别结果发送出去；false 则仅回填到输入框 */
    var autoSend: Boolean by mutableStateOf(true)

    /** 上一次识别得到的文本（供面板短暂回显，可选） */
    var recognizedText: String by mutableStateOf("")
        internal set

    /** 滚动波形样本，初始给一条较低的基线，避免刚打开时是死平一线 */
    val levels = mutableStateListOf<Float>().apply { repeat(barCount) { add(0.06f) } }

    /** 录音器轮询到新响度时调用：右侧入队、超窗从左侧丢弃，形成移动波形 */
    fun pushLevel(level: Float) {
        levels.add(level.coerceIn(0.04f, 1f))
        while (levels.size > barCount) levels.removeAt(0)
    }

    /** 复位为静音基线，用于开始新一段录音 */
    fun resetLevels() {
        repeat(levels.size) { levels[it] = 0.06f }
    }
}
