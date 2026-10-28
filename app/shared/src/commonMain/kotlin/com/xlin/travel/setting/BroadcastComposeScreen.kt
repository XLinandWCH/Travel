package com.xlin.travel.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.chat_screen.ChatToastState
import com.xlin.travel.markdown.MarkdownText
import com.xlin.travel.chat_screen.TopToast
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.IconButtonBackground
import com.xlin.travel.color.SubmitDisabledBackground
import com.xlin.travel.color.SubmitDisabledText
import com.xlin.travel.color.SubmitReadyBackground
import com.xlin.travel.color.SubmitReadyText
import com.xlin.travel.color.TextWhite
import com.xlin.travel.login.UserSession
import com.xlin.travel.navigation.AppBackHandler
import com.xlin.travel.sheet.MultilineInputField
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.back

/**
 * 公告撰写页（管理员从消息中心「公告」页签右下角「+」进入）：
 * 顶部一行含「返回」+「预览」+「发布」，正文支持 Markdown。
 * 预览切换后以 MarkdownText 渲染标题与正文；预览态下「返回」按钮与系统返回键先退回编辑，再次才离开。
 * 发布经 /api/feedback/admin/broadcast 落库，成功回调 [onPublished] 返回。
 */
@Composable
fun BroadcastComposeScreen(
    session: UserSession,
    onBack: () -> Unit,
    onPublished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toast = remember { ChatToastState() }
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf(false) }
    var publishing by remember { mutableStateOf(false) }
    val canPublish = title.isNotBlank() && body.isNotBlank() && !publishing

    // 预览态下系统返回键先退回编辑，而不是直接离开页面（非预览态 enabled=false，交给页面级返回）
    AppBackHandler(enabled = preview) { preview = false }

    fun publish() {
        if (!canPublish) return
        publishing = true
        scope.launch {
            when (val result = MessageApi.broadcast(session.token, title.trim(), body.trim())) {
                is MessageMutationResult.Done -> onPublished()
                is MessageMutationResult.Failed -> {
                    toast.show(result.message)
                    publishing = false
                }
                is MessageMutationResult.Submitted -> onPublished()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // 顶栏：返回 + 标题 + 预览 / 发布（与消息中心其它子页顶栏同尺寸配色）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IconButtonBackground)
                    .clickable(onClick = { if (preview) preview = false else onBack() }),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.back),
                    contentDescription = "返回",
                    colorFilter = ColorFilter.tint(TextWhite),
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "撰写公告",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            // 预览开关：进入预览态时用反转实底高亮
            ComposePillButton(
                text = "预览",
                active = preview,
                onClick = { preview = !preview },
            )
            Spacer(Modifier.width(8.dp))
            ComposePillButton(
                text = if (publishing) "发布中…" else "发布",
                active = canPublish,
                enabled = canPublish,
                onClick = ::publish,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (preview) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    MarkdownText(markdown = body)
                }
            } else {
                MultilineInputField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "公告标题",
                    minHeight = 44.dp,
                    maxHeight = 80.dp,
                )
                Spacer(Modifier.height(12.dp))
                // 正文编辑器占满剩余高度，一打开就是大书写区（不再局限于 minHeight 的小预览）
                MultilineInputField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = "公告正文（支持 Markdown 语法）",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    minHeight = 200.dp,
                    maxHeight = 600.dp,
                )
            }
        }
      }

      TopToast(toast, modifier = Modifier.fillMaxSize())
    }
}

/**
 * 顶栏胶囊按钮：双主题反转配色。
 * [active] 为真用实底（SubmitReady*），否则用图标按钮底 + 白字；[enabled] 为假时降到禁用灰且不响应点击。
 */
@Composable
private fun ComposePillButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val background = when {
        !enabled -> SubmitDisabledBackground
        active -> SubmitReadyBackground
        else -> IconButtonBackground
    }
    val contentColor = when {
        !enabled -> SubmitDisabledText
        active -> SubmitReadyText
        else -> TextWhite
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
