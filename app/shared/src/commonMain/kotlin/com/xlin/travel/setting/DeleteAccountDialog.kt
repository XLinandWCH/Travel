package com.xlin.travel.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.TextGray300
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.color.SubmitDisabledBackground
import com.xlin.travel.color.SubmitDisabledText
import kotlinx.coroutines.delay

/**
 * 注销账号二次确认弹窗：
 * - 正文告知“注销将永久删除本地全部数据，且无法恢复”；
 * - 右下角「取消」「确认」两个按钮；
 * - 「确认」先经历 3 秒灰色倒计时（按钮上实时显示剩余秒数），倒计时结束才可点击。
 */
@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var remaining by remember { mutableIntStateOf(3) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }
    val ready = remaining == 0

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SettingCardBackground)
                .padding(20.dp),
        ) {
            Text(
                text = "注销账号",
                color = FormErrorText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "注销账号将永久删除该账号在本地的全部数据（对话历史、设置等），且无法恢复。" +
                    "服务端也会注销你的所有个人信息。确定要继续吗？",
                color = TextGray300,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "取消",
                    color = TextGray500,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
                Spacer(Modifier.height(0.dp))
                // 确认按钮：倒计时期间灰色不可点，并在按钮上显示剩余秒数
                Text(
                    text = if (ready) "确认" else "确认（$remaining）",
                    color = if (ready) TextWhite else SubmitDisabledText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .alpha(if (ready) 1f else 0.85f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (ready) FormErrorText else SubmitDisabledBackground)
                        .clickable(enabled = ready, onClick = onConfirm)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}
