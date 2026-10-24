package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xlin.travel.chat_screen.ChatToastState
import com.xlin.travel.chat_screen.TopToast
import com.xlin.travel.chat_screen.rememberClipboardWriter
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.navigation.PageTopBar
import com.xlin.travel.sheet.MultilineInputField
import com.xlin.travel.sheet.SheetSubmitButton
import com.xlin.travel.sheet.TravelBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 帮助中心子页面（与「账号控制」「主题颜色」一样是独立页面）：
 * 常见问题手风琴 + 底部「意见反馈」入口，意见反馈本身仍是真正的底部面板。
 */
@Composable
fun HelpCenterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toast = remember { ChatToastState() }
    val writeClipboard = rememberClipboardWriter()
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf<String?>(null) }
    var feedbackOpen by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    fun submitFeedback() {
        // 账号体系与上报接口接入前，草稿先在本地汇总，后续可直接把内容发给后端
        draft = ""
        feedbackOpen = false
        // 面板是独立窗口层，会遮住页面顶部的提示，因此等它收起后再弹
        scope.launch {
            delay(SheetDismissMillis)
            toast.show("感谢您的反馈")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = "帮助中心", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            HelpFaqs.items.forEachIndexed { index, item ->
                if (index > 0) FaqDivider()
                FaqRow(
                    item = item,
                    expanded = expanded == item.question,
                    onToggle = { expanded = if (expanded == item.question) null else item.question },
                    onCopy = { text ->
                        writeClipboard(text)
                        toast.show("邮箱已复制")
                    },
                )
            }
        }

        SheetSubmitButton(enabled = true, text = "意见反馈", onClick = { feedbackOpen = true })
    }

    // 意见反馈：真正的底部面板（Material3 ModalBottomSheet）
    TravelBottomSheet(
        visible = feedbackOpen,
        onDismiss = { feedbackOpen = false },
        title = "意见反馈",
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            MultilineInputField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = HelpFaqs.FeedbackPlaceholder,
                minHeight = 160.dp,
                maxHeight = 300.dp,
            )
        }
        SheetSubmitButton(enabled = draft.isNotBlank(), onClick = ::submitFeedback)
    }

    TopToast(toast, modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}

/** 底部面板的收起动画时长 */
private const val SheetDismissMillis = 300L
