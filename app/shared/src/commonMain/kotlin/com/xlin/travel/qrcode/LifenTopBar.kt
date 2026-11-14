package com.xlin.travel.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.back

/**
 * 里份旅游线专属顶栏：暖色固定配色（不随黑白主题）。
 * 标题前立一条红砖色短竖条，取意老里份的门牌 / 印章；
 * 返回按钮为纸色圆角块 + 深棕墨箭头。
 */
@Composable
fun LifenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LifenChip)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.back),
                contentDescription = "返回",
                colorFilter = ColorFilter.tint(LifenInk),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        // 红砖门牌竖条
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LifenBrick),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = LifenInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}