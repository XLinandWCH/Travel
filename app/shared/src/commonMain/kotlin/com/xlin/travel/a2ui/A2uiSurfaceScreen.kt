package com.xlin.travel.a2ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xlin.travel.a2ui.blocks.A2uiActionBarBlock
import com.xlin.travel.a2ui.blocks.A2uiHeaderBlock
import com.xlin.travel.navigation.PageTopBar

/**
 * 个性化界面宿主页：AI 编排的那一屏在这里落地。
 *
 * 页面结构按「大模块」划分，避免所有内容糊成一列文字：
 * 1. 顶栏（标题就是 AI 给这屏起的名字，返回回聊天）；
 * 2. 滚动区：页头 → 图片 → 内容模块 → 提示 → 免责说明，模块之间留 22dp；
 * 3. 固定底栏：actionBar 的操作按钮吸附在底部，不随内容滚走。
 *
 * [building] 为 true 表示界面还在生成：顶部给一条「正在编排」进度，块逐块淡入上滑，
 * 后面的块到一块显示一块——用户点进来就能看到制作过程，而不是白屏等。
 *
 * 两个客户端兜底：
 * - 首块不是 hero / header 时，用 surface.title 补一个页头；
 * - 整屏没有图片块时，自动补一张「里份通用地图」，保证每屏至少有一张图。
 */
@Composable
fun A2uiSurfaceScreen(
    surface: A2uiSurface?,
    onBack: () -> Unit,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
    building: Boolean = false,
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
            if (building) A2uiBuildingState(modifier = Modifier.weight(1f))
            else A2uiEmptyState(modifier = Modifier.weight(1f))
        } else {
            val actionBlocks = content.blocks.filter {
                A2uiCatalog.canonicalType(it.type) == A2uiType.ActionBar
            }
            val actions = actionBlocks.flatMap { it.actions }.take(3).let { list ->
                // 模型忘了给 primary 时，把第一个按钮提升为主按钮，底部才有主次之分
                if (list.any { it.style.equals("primary", ignoreCase = true) }) list
                else list.mapIndexed { index, action ->
                    if (index == 0) action.copy(style = "primary") else action
                }
            }
            val blocks = remember(content, building) { content.blocks.toContentBlocks(autoImage = !building) }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = A2uiSpace.lg, vertical = A2uiSpace.md),
                verticalArrangement = Arrangement.spacedBy(A2uiSpace.module),
            ) {
                val firstType = blocks.firstOrNull()?.let { A2uiCatalog.canonicalType(it.type) }
                val needsHeader = firstType != A2uiType.Hero && firstType != A2uiType.Header
                if (needsHeader && content.title.isNotBlank()) {
                    A2uiHeaderBlock(
                        A2uiBlock(
                            type = A2uiType.Header,
                            title = content.title,
                            subtitle = content.subtitle,
                        ),
                    )
                }
                if (building) A2uiBuildingHint(blockCount = blocks.size)
                A2uiRevealBlocks(blocks = blocks, onAction = onAction)
                if (!building) {
                    Text(
                        text = "以上界面由 AI 依据工具结果编排 · 景区数据为示例",
                        color = A2uiColor.textTertiary,
                        fontSize = A2uiText.label,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = A2uiSpace.sm),
                    )
                }
            }

            if (actions.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().background(A2uiColor.surface)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(A2uiColor.divider),
                    )
                    Box(modifier = Modifier.padding(horizontal = A2uiSpace.lg, vertical = A2uiSpace.md)) {
                        A2uiActionBarBlock(
                            block = A2uiBlock(type = A2uiType.ActionBar, actions = actions),
                            onAction = onAction,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 正文块：去掉底部操作栏（它固定吸附在页面底部）。
 * [autoImage] 为 true（生成完成后）时，缺图就补一张里份通用地图；
 * 生成中不补，否则模型后面真给了图片块，屏幕上会先出现一张假地图再被换掉。
 */
private fun List<A2uiBlock>.toContentBlocks(autoImage: Boolean): List<A2uiBlock> {
    val withoutActions = filter { block ->
        val type = A2uiCatalog.canonicalType(block.type)
        // 模型给了图片名但本地无对应资源时，这张空图卡直接丢掉，后面会统一补地图
        type != A2uiType.ActionBar &&
            (type != A2uiType.Image || A2uiImages.painterFor(block.image) != null)
    }
    val hasImage = withoutActions.any { A2uiCatalog.canonicalType(it.type) == A2uiType.Image }
    if (hasImage || !autoImage) return withoutActions
    val map = A2uiBlock(
        type = A2uiType.Image,
        image = "map",
        title = "里份分布示意图",
        text = "武汉历史城区街巷与传统里份保护图，深色块为主要里份集中区域。",
    )
    // 插在页头之后：先交代「这是什么」，再给全景图，最后才是具体内容
    val insertAt = if (withoutActions.isEmpty()) 0 else 1
    return withoutActions.toMutableList().apply { add(insertAt, map) }
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
