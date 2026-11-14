package com.xlin.travel.a2ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 个性化界面的「生成中」表现：
 * - [A2uiRevealBlocks]：块依次淡入上滑，生成中到达的新块也自动接上；
 * - [A2uiBuildingHint]：顶部进度条，告诉用户已经排到第几块；
 * - [A2uiBuildingState]：一块都还没到时的空态，避免白屏。
 *
 * 单独成文件是为了让宿主页 [A2uiSurfaceScreen] 只关心「布局」本身。
 */

/** 逐块入场：打开时块依次淡入上滑，生成中到达的新块也会自动接上 */
@Composable
internal fun A2uiRevealBlocks(blocks: List<A2uiBlock>, onAction: (String) -> Unit) {
    var revealed by remember { mutableIntStateOf(0) }
    LaunchedEffect(blocks.size) {
        while (revealed < blocks.size) {
            delay(RevealStepMillis)
            revealed += 1
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.module)) {
        blocks.forEachIndexed { index, block ->
            AnimatedVisibility(
                visible = index < revealed,
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 8 },
            ) {
                if (!block.isEmpty) A2uiBlockView(block = block, onAction = onAction)
            }
        }
    }
}

/** 生成中的进度条：告诉用户「还在写、已经排到第几块」 */
@Composable
internal fun A2uiBuildingHint(blockCount: Int) {
    val shape = RoundedCornerShape(A2uiRadius.block)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(A2uiColor.surfaceSunken)
            .border(1.dp, A2uiColor.outline, shape)
            .padding(horizontal = A2uiSpace.md, vertical = A2uiSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = A2uiColor.accent,
            strokeWidth = 2.dp,
        )
        Text("AI 正在编排界面…", color = A2uiColor.textSecondary, fontSize = A2uiText.caption)
        Spacer(Modifier.weight(1f))
        if (blockCount > 0) {
            Text("已排好 $blockCount 块", color = A2uiColor.textMeta, fontSize = A2uiText.label)
        }
    }
}

/** 生成中的空态：一块都还没到时也不留白屏 */
@Composable
internal fun A2uiBuildingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
            modifier = Modifier.padding(horizontal = A2uiSpace.xl),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = A2uiColor.accent,
                strokeWidth = 2.dp,
            )
            Text("正在为你编排这一屏", color = A2uiColor.textPrimary, fontSize = A2uiText.title)
            Text(
                "界面会一块块出现，不用等它全部写完",
                color = A2uiColor.textTertiary,
                fontSize = A2uiText.caption,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 逐块入场的节奏：7 块也只要半秒走完，不拖时间 */
private const val RevealStepMillis = 70L
