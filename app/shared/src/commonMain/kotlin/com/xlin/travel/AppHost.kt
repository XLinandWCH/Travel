package com.xlin.travel

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.xlin.travel.chat_screen.ChatMessage
import com.xlin.travel.chat_screen.ChatState
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.rememberChatRepository
import com.xlin.travel.drawer.ChatHistoryState
import com.xlin.travel.inputbox.ChatInputState
import com.xlin.travel.login.UserSession
import com.xlin.travel.setting.BroadcastDto
import com.xlin.travel.setting.LegalDoc
import com.xlin.travel.setting.MessageBadgeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 应用级共享状态：各页面路由分支都要读写的会话 / 导航 / 徽标状态集中在这里，
 * 使 [App] 只负责装配与生命周期 effects，具体页面渲染拆到 App*Routes.kt。
 */
@Stable
internal class AppHost(
    val repository: ChatRepository,
    val inputState: ChatInputState,
    val chatState: ChatState,
    val history: ChatHistoryState,
    val session: UserSession,
    val badge: MessageBadgeState,
    val drawerState: DrawerState,
    val scope: CoroutineScope,
) {
    /** 当前顶层页面 */
    var screen by mutableStateOf(AppScreen.Guest)

    /** 启动时先不渲染任何业务页面，等数据库异步恢复完登录态再决定去向，避免已登录用户看到未登入界面一闪而过 */
    var appReady by mutableStateOf(false)

    /** 从消息中心点击某条反馈时记住工单号，供详情页拉取线程 */
    var detailTicketId by mutableStateOf("")

    /** 从消息中心点击某条公告时记住它，供公告详情页展示（列表已带回完整内容，无需再拉取） */
    var selectedBroadcast by mutableStateOf<BroadcastDto?>(null)

    /** 从聊天页输入框上方的小程序快捷条（或弹出的全部面板）点某个小程序时记住名字，供占位页作标题 */
    var currentProgram by mutableStateOf("")

    /**
     * 从聊天页点「进入个性化界面」时记住这条回复（而不是拷贝一份 surface）。
     * 界面可能还在生成：持有消息本体，新块到达时页面能直接重组出来。
     */
    var currentSurfaceMessage by mutableStateOf<ChatMessage?>(null)

    /**
     * 个性化界面的返回目标：从聊天页进入回聊天页，从历史页进入回历史页。
     * 顶栏返回按钮与系统返回键都读它，避免从历史页点进去后被弹回聊天。
     */
    var surfaceBackTarget by mutableStateOf(AppScreen.Chat)

    /** 尚未开发的设置条目统一进入「XXX 即将上线」占位页 */
    var comingSoonTitle by mutableStateOf("")

    /** 服务条款 / 隐私政策等法务文档页要展示的文档 */
    var legalDoc by mutableStateOf(LegalDoc.Terms)

    /** 抽屉内入口统一：先关抽屉再切页面 */
    fun openFromDrawer(target: AppScreen) {
        scope.launch { drawerState.close() }
        screen = target
    }
}

/** 创建并记住应用级状态（组合内各 remember 的结果在重组间保持稳定） */
@Composable
internal fun rememberAppHost(): AppHost {
    val repository = rememberChatRepository()
    val inputState = remember { ChatInputState() }
    val history = remember(repository) { ChatHistoryState(repository) }
    val chatState = remember(repository) {
        ChatState(repository) { id ->
            history.select(id)
            history.refresh()
        }
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val session = remember { UserSession() }
    val badge = remember { MessageBadgeState() }
    return remember(repository) {
        AppHost(repository, inputState, chatState, history, session, badge, drawerState, scope)
    }
}
