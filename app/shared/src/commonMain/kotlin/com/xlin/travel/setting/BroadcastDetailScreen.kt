package com.xlin.travel.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.markdown.MarkdownText
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.navigation.PageTopBar
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.admin

/**
 * 公告详情页（消息中心点击某条公告进入，普通用户与管理员通用）：
 * 顶部「公告」标题 + 返回，正文区展示发布人 / 时间与 Markdown 渲染后的完整内容。
 */
@Composable
fun BroadcastDetailScreen(
    item: BroadcastDto,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = "公告", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.admin),
                    contentDescription = "管理员",
                    colorFilter = ColorFilter.tint(AccentOrange),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(text = item.adminUsername, color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(12.dp))
                Text(text = formatBroadcastTime(item.createdAt), color = TextGray500, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.title,
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
            )
            Spacer(Modifier.height(14.dp))
            MarkdownText(markdown = item.content)
        }
    }
}

/** 服务端 LocalDateTime 字符串（2026-10-25T12:30:00.123）转展示格式，精确到分钟 */
private fun formatBroadcastTime(raw: String): String = raw.replace('T', ' ').take(16)
