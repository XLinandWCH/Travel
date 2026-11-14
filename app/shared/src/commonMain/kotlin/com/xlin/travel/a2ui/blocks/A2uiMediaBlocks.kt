package com.xlin.travel.a2ui.blocks

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xlin.travel.a2ui.A2uiBlock
import com.xlin.travel.a2ui.A2uiColor
import com.xlin.travel.a2ui.A2uiGlyph
import com.xlin.travel.a2ui.A2uiImages
import com.xlin.travel.a2ui.A2uiRadius
import com.xlin.travel.a2ui.A2uiSpace
import com.xlin.travel.a2ui.A2uiText
import org.jetbrains.compose.resources.painterResource

/**
 * 图片类组件：image。
 *
 * 一屏里至少有一张图，界面就不会是「一坨文字」。图片名由模型给语义值（map / scenic …），
 * 客户端映射到本地资源；认不出就退化成带图标的中性占位卡，绝不出现破图。
 * 图注（title / text）放在图下方，不当浮层压在图上，避免遮住地图细节。
 */
@Composable
internal fun A2uiImageBlock(block: A2uiBlock) {
    val painter = A2uiImages.painterFor(block.image)
    Column(verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(A2uiImageHeight)
                .clip(RoundedCornerShape(A2uiRadius.card))
                .background(A2uiColor.surfaceSunken)
                .border(1.dp, A2uiColor.outline, RoundedCornerShape(A2uiRadius.card)),
            contentAlignment = Alignment.Center,
        ) {
            if (painter != null) {
                // Fit 而不是 Crop：地图类图要能看全，裁掉一半就失去意义
                Image(
                    painter = painterResource(painter),
                    contentDescription = block.title.ifBlank { "示意图" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(A2uiSpace.sm),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(A2uiSpace.sm),
                ) {
                    A2uiGlyph(icon = block.icon.ifBlank { "nearby" }, size = 28.dp)
                    Text(
                        text = "暂无示意图",
                        color = A2uiColor.textTertiary,
                        fontSize = A2uiText.caption,
                    )
                }
            }
        }
        if (block.title.isNotBlank()) {
            Text(
                text = block.title,
                color = A2uiColor.textPrimary,
                fontSize = A2uiText.body,
                lineHeight = A2uiText.bodyLine,
                fontWeight = FontWeight.Medium,
            )
        }
        if (block.text.isNotBlank()) {
            Text(
                text = block.text,
                color = A2uiColor.textMeta,
                fontSize = A2uiText.caption,
                lineHeight = A2uiText.captionLine,
            )
        }
    }
}

/** 图片卡固定高度：足够看清整体轮廓，又不至于把下面的内容顶出首屏 */
private val A2uiImageHeight = 208.dp
