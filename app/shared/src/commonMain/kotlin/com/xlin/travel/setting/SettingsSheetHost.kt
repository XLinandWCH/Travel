package com.xlin.travel.setting

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.xlin.travel.chat_screen.ChatToastState
import com.xlin.travel.chat_screen.TopToast
import com.xlin.travel.chat_screen.rememberClipboardWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 设置页面板总控：托管「服务条款 / 隐私政策 / 帮助中心 / 意见反馈」四个底部面板，
 * 并提供顶部提示胶囊。
 *
 * 面板本身是 ModalBottomSheet（独立窗口层），所以这里的组合位置不影响显示层级；
 * 顶部提示画在页面层，会被打开中的面板遮住，因此需要等面板收起后再显示。
 */
@Composable
internal fun SettingsSheetHost(state: SettingsSheetState) {
    val toast = remember { ChatToastState() }
    val writeClipboard = rememberClipboardWriter()
    val scope = rememberCoroutineScope()

    DocumentSheet(
        state = state,
        sheet = SettingsSheet.Terms,
        title = "服务条款（暂行版）",
        file = "files/terms.md",
    )
    DocumentSheet(
        state = state,
        sheet = SettingsSheet.Privacy,
        title = "隐私政策（暂行版）",
        file = "files/privacy.md",
    )
    HelpSheet(
        state = state,
        onCopy = { text -> writeClipboard(text) },
        onFeedback = { state.show(SettingsSheet.Feedback) },
    )
    FeedbackInputSheet(
        state = state,
        onSubmit = {
            state.submitFeedback()
            // 等面板的收起动画播完，提示才不会被面板窗口挡住
            scope.launch {
                delay(SheetDismissMillis)
                toast.show("感谢您的反馈")
            }
        },
    )

    TopToast(toast, modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}

/** 面板收起动画时长 */
private const val SheetDismissMillis = 300L
