package com.xlin.travel.inputbox.voice

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS 麦克风权限：AVAudioSession 查询当前授权态，未决定时弹系统授权框。
 * 需在 iosApp/Info.plist 声明 NSMicrophoneUsageDescription，否则申请会崩溃。
 * 系统授权回调可能不在主线程，这里 dispatch 回主线程再更新 Compose 状态。
 *
 * 注意：仅在 iOS 目标编译；真机 SDK 上方法名若有出入，按 Xcode 报错微调。
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): () -> Unit = {
    val session = AVAudioSession.sharedInstance()
    when (session.recordPermission) {
        AVAudioSessionRecordPermissionGranted -> onResult(true)
        AVAudioSessionRecordPermissionDenied -> onResult(false)
        else -> session.requestRecordPermission { granted ->
            dispatch_async(dispatch_get_main_queue()) { onResult(granted) }
        }
    }
}
