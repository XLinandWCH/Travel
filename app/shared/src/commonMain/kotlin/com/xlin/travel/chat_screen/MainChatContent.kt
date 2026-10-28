package com.xlin.travel.chat_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.xlin.travel.color.MenuButtonBackground
import com.xlin.travel.color.TextWhite
import com.xlin.travel.color.TopBarEdgeFadeBrush
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.inputbox.ChatInputBox
import com.xlin.travel.inputbox.ChatInputState
import com.xlin.travel.miniprogram.MiniProgramQuickBar
import com.xlin.travel.setting.UnreadBadgeDot
import com.xlin.travel.tts.TtsController
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.close_reading
import travel.app.shared.generated.resources.menu
import travel.app.shared.generated.resources.start_reading

/**
 * 聊天页：消息区铺满，顶栏半透明悬浮其上，底部为小程序快捷条 + 固定输入框。
 *
 * 根节点统一监听「点击空白处」：Compose 不会自动取消文字选区、也不会因点击容器外
 * 而丢失输入框焦点（框架特性），这里用未被子级消费的按下事件作判据，
 * 一次点击同时清空 [SelectionRegistry] 里的全部选区并退出输入焦点（收起键盘）。
 */
@Composable
internal fun MainChatContent(
    inputState: ChatInputState,
    chatState: ChatState,
    repository: ChatRepository,
    onMenuClick: () -> Unit,
    onSend: (String) -> Unit,
    hasUnreadMessage: Boolean = false,
    onOpenMiniProgram: (String) -> Unit = {},
) {
    val selectionRegistry = rememberSelectionRegistry()
    val focusManager = LocalFocusManager.current
    val uiScope = rememberCoroutineScope()
    // 朗读总控：动作条的手动朗读 + 顶栏的自动朗读开关共用一套状态
    val tts = remember { TtsController(repository) }
    LaunchedEffect(Unit) { tts.loadPreferences() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()   // 内容避开状态栏 / 导航栏，不再与其重叠
            .imePadding()           // 键盘弹出时输入框上移
            .pointerInput(selectionRegistry) {
                // Main 路径子级先处理：按钮 / 文本框 / 选区都会消费按下事件，
                // 只有落在空白处的按下才会 isConsumed == false
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!down.isConsumed) {
                        selectionRegistry.clearAll()
                        focusManager.clearFocus(force = true)
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalSelectionRegistry provides selectionRegistry) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // 聊天消息铺满整个区域（顶部留出浮层高度，由 ChatScreen 的内容内边距处理）
                ChatScreen(
                    state = chatState,
                    inputState = inputState,
                    tts = tts,
                    modifier = Modifier.fillMaxSize(),
                )

                // 顶部区域半透明，避免整条 fillMaxWidth 的实色条大面积遮挡聊天内容
                MainTopBar(
                    onMenuClick = onMenuClick,
                    hasUnreadMessage = hasUnreadMessage,
                    autoReadEnabled = tts.autoReadEnabled,
                    onToggleAutoRead = { uiScope.launch { tts.toggleAutoRead() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TopBarEdgeFadeBrush),
                )
            }

            // 小程序快捷条：紧贴输入框上方，左侧横向滚动的快捷胶囊撑满剩余宽度，
            // 右侧固定圆形菜单按钮，点击弹出全部小程序的面板
            MiniProgramQuickBar(
                onOpenProgram = onOpenMiniProgram,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            )

            // 输入框
            ChatInputBox(
                state = inputState,
                onSend = onSend,
                isStreaming = chatState.isStreaming,
                onStop = chatState::stopStreaming,
                repository = repository,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** 主内容顶栏：左侧菜单按钮（右上角可叠加未读圆点徽标）用于打开抽屉；
 * 最右侧通过 Spacer 分栏放「自动朗读」开关（close_reading 关 / start_reading 开），
 * 开启后每轮 AI 回复生成完自动朗读正文（不含思考内容） */
@Composable
private fun MainTopBar(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasUnreadMessage: Boolean = false,
    autoReadEnabled: Boolean = false,
    onToggleAutoRead: () -> Unit = {},
) {
    Row(
        modifier = modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MenuButtonBackground)
                .clickable(onClick = onMenuClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.menu),
                contentDescription = "打开菜单",
                // menu.xml 自身是白色填充，这里必须跟随主题着色，否则浅色主题下白图标落在浅底上看不见
                colorFilter = ColorFilter.tint(TextWhite),
                modifier = Modifier.size(22.dp),
            )
            UnreadBadgeDot(
                visible = hasUnreadMessage,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        // 左右分栏：开关推到行尾，图标颜色与菜单按钮同款
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MenuButtonBackground)
                .clickable(onClick = onToggleAutoRead),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(
                    if (autoReadEnabled) Res.drawable.start_reading else Res.drawable.close_reading,
                ),
                contentDescription = if (autoReadEnabled) "自动朗读：开" else "自动朗读：关",
                colorFilter = ColorFilter.tint(TextWhite),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
