package com.xlin.travel.chat_screen

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 聊天列表的「智能吸底」控制器（由 [rememberChatScrollFollow] 创建）。
 *
 * 判定分两层，缺一不可：
 * 1. **意图** [followBottom]：手指往下拖列表（翻更早的内容）立刻打断跟随；
 *    视口重新贴到底部 / 发新消息 / 切会话恢复跟随；
 * 2. **可见性** [buttonVisible]：底部还有没露出来的内容
 *    （[LazyListState.canScrollForward]）且不处于跟随中才浮现按钮。
 *
 * 旧实现只用第 1 层，按钮的显示完全依赖「观察到视口上移」这一个手工判据：
 * 快滑时相邻帧的 index/offset 可能被合并成一次发射、流式期间自动下滚又会把视口
 * 重新钳回底部（`canScrollForward` 变 false 就把意图抹回 true），
 * 于是出现「明明往上翻了，按钮却没出现」。现在第 2 层直接读列表自己的布局状态兜底：
 * 空闲态只要底部还有内容没露出来就一定显示；流式跟随中则不显示，避免逐字增量导致闪烁。
 */
@Stable
class ChatScrollFollow internal constructor(
    val listState: LazyListState,
    private val streaming: () -> Boolean,
    private val scope: CoroutineScope,
) {
    /** 是否自动跟随底部：用户往上翻时置 false，回到底部 / 新消息 / 切会话置 true */
    var followBottom: Boolean by mutableStateOf(true)
        internal set

    /** 底部是否还有没露出来的内容 */
    private val canGoDown = derivedStateOf { listState.canScrollForward }

    /** 「回到底部」按钮是否该出现 */
    val buttonVisible: Boolean
        get() = canGoDown.value && (!followBottom || !streaming())

    /** 点按钮：一路滚到真正的底部并恢复吸底跟随 */
    fun scrollToBottom() {
        scope.launch {
            followBottom = true
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last >= 0) listState.scrollToItem(last, BottomScrollOffset)
        }
    }

    /**
     * 挂在 LazyColumn 上：按下后累加手指竖向位移，超过系统触摸阈值且方向是「往下拖」
     * （往上翻更早的内容）就把 [followBottom] 关掉。
     * 只读 `position` 与 `previousPosition` 的差值、不消费事件，列表滚动完全不受影响。
     */
    fun intentModifier(): Modifier = Modifier.pointerInput(listState) {
        val slop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalY = 0f
            while (true) {
                val change = awaitPointerEvent()
                    .changes
                    .firstOrNull { it.id == down.id && it.pressed }
                    ?: break
                totalY += change.position.y - change.previousPosition.y
                if (totalY > slop) {
                    followBottom = false
                    break
                }
            }
        }
    }
}

/** 吸底滚动用的超大偏移量：LazyList 会自行钳制到内容真正的末尾（末条底部对齐） */
internal const val BottomScrollOffset = 1_000_000

/**
 * 创建并驱动 [ChatScrollFollow]：滚动跟随的逻辑都在这里，
 * [ChatScreen] 只需把 [ChatScrollFollow.intentModifier] 挂到列表、
 * 把 [ChatScrollFollow.buttonVisible] 传给按钮。
 */
@Composable
internal fun rememberChatScrollFollow(
    listState: LazyListState,
    chatState: ChatState,
): ChatScrollFollow {
    val scope = rememberCoroutineScope()
    val follow = remember(listState, scope) {
        ChatScrollFollow(listState, streaming = { chatState.isStreaming }, scope = scope)
    }

    val lastIndex = chatState.messages.lastIndex
    val lastMessage = chatState.messages.lastOrNull()

    // 视口「上移」必定是用户手动滑（程序滚动只往下），因此据此打断跟随；
    // 不再能向前滚（canScrollForward=false）视为已回到底部，恢复跟随
    LaunchedEffect(listState) {
        var prevIndex = -1
        var prevOffset = -1
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.canScrollForward,
            )
        }.collect { (index, offset, canForward) ->
            val scrolledUp = prevIndex >= 0 &&
                (index < prevIndex || (index == prevIndex && offset < prevOffset))
            if (!canForward) follow.followBottom = true
            else if (scrolledUp) follow.followBottom = false
            prevIndex = index
            prevOffset = offset
        }
    }

    // 新消息 / 切换会话：无条件回到并跟随底部
    LaunchedEffect(chatState.conversationId, chatState.messages.size) {
        follow.followBottom = true
    }

    // 跟随状态下，流式增量（思考 / 正文变长）到达时贴到「真正的底部」：
    // scrollToItem 默认只把末条顶部对齐，长回答会留下一半在屏幕外；
    // 传一个超大 scrollOffset 让列表自行钳制到内容末尾，所有会话都一致生效
    LaunchedEffect(
        follow.followBottom,
        chatState.conversationId,
        chatState.messages.size,
        lastMessage?.content?.length,
        lastMessage?.reasoning?.length,
    ) {
        if (follow.followBottom && lastIndex >= 0) {
            listState.scrollToItem(lastIndex, BottomScrollOffset)
        }
    }

    return follow
}
