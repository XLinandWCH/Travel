package com.xlin.travel.a2ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.xlin.travel.a2ui.blocks.A2uiHeaderBlock
import com.xlin.travel.navigation.PageTopBar

/**
 * 个性化界面宿主页：AI 编排的那一屏在这里落地。
 *
 * 页面本身不含任何业务逻辑，只做三件事：
 * 1. 顶栏（标题就是 AI 给这屏起的名字，返回回聊天）；
 * 2. 若首块不是 hero / header，用 surface.title 补一个页头，保证「打开就知道这是什么」；
 * 3. 交给 [A2uiBlocks] 渲染，actionBar 的按钮回调由调用方接回聊天。
 */
@Composable
fun A2uiSurfaceScreen(
    surface: A2uiSurface?,
    onBack: () -> Unit,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = surface?.takeIf { !it.isEmpty }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(A2uiColor.page)
            .safeContentPadding(),
    ) {
        PageTopBar(
            title = content?.title?.takeIf { it.isNotBlank() } ?: "个性化界面",
            onBack = onBack,
        )
        if (content == null) {
            A2uiEmptyState(modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = A2uiSpace.lg, vertical = A2uiSpace.sm),
                verticalArrangement = Arrangement.spacedBy(A2uiSpace.lg),
            ) {
                val firstType = content.blocks.firstOrNull()?.let { A2uiCatalog.canonicalType(it.type) }
                if (firstType != A2uiType.Hero && firstType != A2uiType.Header) {
                    A2uiHeaderBlock(
                        A2uiBlock(
                            type = A2uiType.Header,
                            title = content.title,
                            subtitle = content.subtitle,
                        ),
                    )
                }
                A2uiBlocks(blocks = content.blocks, onAction = onAction)
                Text(
                    text = "以上界面由 AI 依据工具结果编排 · 景区数据为示例",
                    color = A2uiColor.textTertiary,
                    fontSize = A2uiText.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = A2uiSpace.sm),
                )
            }
        }
    }
}

/** 空态：surface 没内容（模型没调 a2ui_render 或参数全空）时的兜底，不留白屏 */
@Composable
private fun A2uiEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
            modifier = Modifier.padding(horizontal = A2uiSpace.xl),
        ) {
            Text(
                text = "这次回答没有生成界面",
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.title,
            )
            Text(
                text = "回到聊天继续追问，AI 会重新为你排一屏。",
                color = A2uiColor.textTertiary,
                fontSize = A2uiText.caption,
                textAlign = TextAlign.Center,
            )
        }
    }
}
