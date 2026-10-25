package com.xlin.travel.chat_screen

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionState
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import com.xlin.travel.color.TextSelectionBackground
import com.xlin.travel.color.TextSelectionHandle

/**
 * 页面内文字选区的登记处：
 * Compose 没有内建「点击容器外区域取消选择」的行为（点别处选区不会消失，这是框架特性），
 * 每个 [SelectionBox] 把自己的 [SelectionState] 注册到这里，页面根节点观察到
 * 「未被任何子级消费的按下事件」（即点击空白处）时调用 [clearAll] 统一清空。
 */
@Stable
class SelectionRegistry {
    internal val states = mutableStateListOf<SelectionState>()

    /** 清空全页选区（无选区时调用无副作用） */
    fun clearAll() {
        states.forEach { it.clear() }
    }
}

@Composable
fun rememberSelectionRegistry(): SelectionRegistry = remember { SelectionRegistry() }

/** 当前页面的 [SelectionRegistry]；未在 Provider 内的 SelectionBox 使用各自的默认选区状态 */
val LocalSelectionRegistry = staticCompositionLocalOf<SelectionRegistry?> { null }

/**
 * AI 输出文本（正文 + 思考内容）的可选中容器：长按（移动端）/ 鼠标拖选后可复制。
 * 选区高亮与拖拽手柄统一用强调橙半透明档（TextSelectionBackground / TextSelectionHandle），
 * 只包裹文本、不包裹 clickable 按钮 —— SelectionContainer 会拦截子级手势，
 * 标题折叠、动作按钮等交互必须留在容器外面。
 */
@Composable
fun SelectionBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val selectionColors = TextSelectionColors(
        handleColor = TextSelectionHandle,
        backgroundColor = TextSelectionBackground,
    )
    val registry = LocalSelectionRegistry.current
    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        if (registry == null) {
            SelectionContainer(modifier = modifier, content = content)
        } else {
            // 显式持有 SelectionState 并登记，让页面根部的「点空白清空」能触及这块选区
            val state = rememberSelectionState()
            DisposableEffect(state, registry) {
                registry.states.add(state)
                onDispose { registry.states.remove(state) }
            }
            SelectionContainer(state = state, modifier = modifier, content = content)
        }
    }
}
