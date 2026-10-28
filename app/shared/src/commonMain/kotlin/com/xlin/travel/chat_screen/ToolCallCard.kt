package com.xlin.travel.chat_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.ToolCallAccent
import com.xlin.travel.color.ToolCallChipBackground
import com.xlin.travel.color.ToolCallTextColor
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.tool_invocation

/**
 * 思考卡片里的工具调用行：以「[tool_invocation 图标] 调用识图工具」的独立蓝色小卡片呈现，
 * 只显示工具名称，不显示入参与返回结果（工具到底在干嘛一目了然）。
 *
 * [item] 进行中时保持普通态即可（外层「思考中…」已在动）；失败时文字转失败提示，
 * 让用户知道这一步没成功。
 */
@Composable
fun ToolCallRow(item: ThinkingItem.ToolCall) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ToolCallChipBackground)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.tool_invocation),
            contentDescription = item.label,
            colorFilter = ColorFilter.tint(ToolCallAccent),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (item.failed) "${item.label}（未完成）" else item.label,
            color = if (item.failed) ToolCallTextColor.copy(alpha = 0.7f) else ToolCallTextColor,
            fontSize = 13.sp,
        )
    }
}
