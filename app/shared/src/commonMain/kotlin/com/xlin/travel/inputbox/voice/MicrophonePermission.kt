package com.xlin.travel.inputbox.voice

import androidx.compose.runtime.Composable

/**
 * 麦克风录音权限（expect / actual）。返回一个 lambda，调用它即检查 / 申请权限：
 * 已授权立即回调 true，否则拉起系统授权框并把用户选择（true / false）回调出去。
 *
 * 回调保证在主线程执行，可安全更新 Compose 状态。
 * - Android：ActivityResultContracts.RequestPermission（RECORD_AUDIO）
 * - iOS：AVAudioSession.requestRecordPermission（需在 Info.plist 声明 NSMicrophoneUsageDescription）
 * - JVM（桌面）：无录音设备接入，直接回调 false
 */
@Composable
expect fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): () -> Unit
