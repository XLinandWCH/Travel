package com.xlin.travel.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 景点详情页（当前为"开发中.."占位）：
 * 各线路的景点按钮都先进入这里，顶部展示对应介绍图，下方留白显示"开发中.."；
 * 后续给某个景点补充真实内容时，在路由处改跳新页面即可。
 * 固定暖色（[LifenColors]），不随黑白主题切换。
 */
@Composable
fun TravelDetailScreen(
    title: String,
    image: DrawableResource?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LifenPageBrush)
            .safeContentPadding(),
    ) {
        LifenTopBar(title = title, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (image != null) {
                Image(
                    painter = painterResource(image),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "开发中..",
                    color = LifenInkSoft,
                    fontSize = 18.sp,
                )
                Text(
                    text = "「$title」的更多精彩内容即将呈现",
                    color = LifenInkSoft.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}