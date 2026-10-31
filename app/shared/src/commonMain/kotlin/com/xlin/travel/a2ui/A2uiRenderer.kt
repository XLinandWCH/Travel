package com.xlin.travel.a2ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xlin.travel.a2ui.blocks.A2uiActionBarBlock
import com.xlin.travel.a2ui.blocks.A2uiBulletsBlock
import com.xlin.travel.a2ui.blocks.A2uiCardListBlock
import com.xlin.travel.a2ui.blocks.A2uiChipsBlock
import com.xlin.travel.a2ui.blocks.A2uiDividerBlock
import com.xlin.travel.a2ui.blocks.A2uiHeaderBlock
import com.xlin.travel.a2ui.blocks.A2uiHeroBlock
import com.xlin.travel.a2ui.blocks.A2uiKeyValueBlock
import com.xlin.travel.a2ui.blocks.A2uiNoticeBlock
import com.xlin.travel.a2ui.blocks.A2uiParagraphBlock
import com.xlin.travel.a2ui.blocks.A2uiProgressBlock
import com.xlin.travel.a2ui.blocks.A2uiQuoteBlock
import com.xlin.travel.a2ui.blocks.A2uiRatingBlock
import com.xlin.travel.a2ui.blocks.A2uiSpacerBlock
import com.xlin.travel.a2ui.blocks.A2uiStatRowBlock
import com.xlin.travel.a2ui.blocks.A2uiStepsBlock
import com.xlin.travel.a2ui.blocks.A2uiTimelineBlock

/**
 * A2UI 渲染器：把模型下发的 block 列表映射到本地 Compose 组件。
 *
 * 这里是「AI 永远碰不到 UI 代码」的边界——模型只会得到 17 个 type 名字，
 * 具体长什么样完全由客户端决定；因此换皮肤、改字号都只改这一层与 [A2uiDesign]。
 *
 * 两个容错约定：
 * 1. 空块直接跳过（模型常留占位块，屏幕上不能出现空白卡片）；
 * 2. 未知 type 退化为 paragraph，绝不因为模型写错一个词就整屏白掉。
 */
@Composable
fun A2uiBlocks(
    blocks: List<A2uiBlock>,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(A2uiSpace.lg),
    ) {
        blocks.forEach { block ->
            if (!block.isEmpty) A2uiBlockView(block = block, onAction = onAction)
        }
    }
}

/** 单个块分发：别名先归一化，再落到具体组件 */
@Composable
fun A2uiBlockView(block: A2uiBlock, onAction: (String) -> Unit) {
    when (A2uiCatalog.canonicalType(block.type)) {
        A2uiType.Header -> A2uiHeaderBlock(block)
        A2uiType.Hero -> A2uiHeroBlock(block)
        A2uiType.Bullets -> A2uiBulletsBlock(block)
        A2uiType.CardList -> A2uiCardListBlock(block)
        A2uiType.KeyValue -> A2uiKeyValueBlock(block)
        A2uiType.Chips -> A2uiChipsBlock(block)
        A2uiType.Steps -> A2uiStepsBlock(block)
        A2uiType.Timeline -> A2uiTimelineBlock(block)
        A2uiType.StatRow -> A2uiStatRowBlock(block)
        A2uiType.Notice -> A2uiNoticeBlock(block)
        A2uiType.Quote -> A2uiQuoteBlock(block)
        A2uiType.Divider -> A2uiDividerBlock()
        A2uiType.Progress -> A2uiProgressBlock(block)
        A2uiType.Rating -> A2uiRatingBlock(block)
        A2uiType.ActionBar -> A2uiActionBarBlock(block = block, onAction = onAction)
        A2uiType.Spacer -> A2uiSpacerBlock(block)
        else -> A2uiParagraphBlock(block)
    }
}
