package com.xlin.travel.chat_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.FileChipBackground
import com.xlin.travel.color.FileChipText
import com.xlin.travel.color.UserBubbleBackground
import com.xlin.travel.color.UserMessageText
import com.xlin.travel.image_picker.FileIcons
import org.jetbrains.compose.resources.painterResource

/**
 * 用户消息：靠右对齐。
 * 若附带了图片，则缩略图在上、提示词气泡在下（与输入框预览图同大小，均为 64.dp）。
 * 长按整个内容区触发 [onLongPress]，由上层在气泡右下角展开 [menuContent]（上下文菜单）。
 */
@Composable
fun UserMessageBubble(
    message: ChatMessage,
    onLongPress: () -> Unit,
    menuContent: @Composable () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress,
                ),
            ) {
                // 图片缩略图：尺寸与输入框预览一致，右上角圆角方形
                message.imageBitmap?.let { image ->
                    Image(
                        bitmap = image,
                        contentDescription = "用户发送的图片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }

                // 发送的文件：与图片同样落在提示词上方——类型彩色图标（不调配色）+ 文件名（过长省略）
                message.documentName?.let { name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(FileChipBackground)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Image(
                            painter = painterResource(FileIcons.iconForName(name)),
                            contentDescription = "用户发送的文件",
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = name,
                            color = FileChipText,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp),
                        )
                    }
                }

                // 提示词气泡：无文字时（仅发图）不渲染空气泡
                if (message.content.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(UserBubbleBackground)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = message.content,
                            color = UserMessageText,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
        }
        // 长按弹出的上下文菜单锚点：DropdownMenu 优先把自身起点对齐「父级」起点，
        // 父级若是撑满整行的 Box，菜单就会跑到左边；这里改用零尺寸、贴在气泡右下角的锚点，
        // 菜单就贴齐用户消息的右侧并从气泡下方展开（下方放不下时框架自动翻到上方）
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            menuContent()
        }
    }
}

/**
 * AI message：左侧展示，思考区为可折叠的「深度思考」卡片（ReasoningSection），
 * 正文按 Markdown 渲染。正文输出结束后（[showActions] 为 true）在下方从左到右排开五个动作按钮（含朗读）。
 *
 * [isThinking] 为 true 表示这条消息正处于思考阶段（流式中且正文还没开始），
 * 思考区标题显示跳动的「思考中…」并默认展开；思考结束后自动收起为「已深度思考」。
 */
@Composable
fun AssistantMessageBlock(
    message: ChatMessage,
    showActions: Boolean,
    canRegenerate: Boolean,
    isThinking: Boolean,
    isReading: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onRegenerate: () -> Unit,
    onRead: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 思考区：思考中 / 已深度思考两种标题态，点击可展开收起
        ReasoningSection(message = message, isThinking = isThinking)

        if (message.content.isNotEmpty()) {
            if (message.reasoning.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
            }
            // 正文用强调橙半透明选区色，长按 / 拖选即可复制
            SelectionBox {
                MarkdownText(message.content)
            }

            if (showActions) {
                Spacer(Modifier.height(6.dp))
                MessageActionRow(
                    message = message,
                    canRegenerate = canRegenerate,
                    isReading = isReading,
                    onLike = onLike,
                    onDislike = onDislike,
                    onRegenerate = onRegenerate,
                    onRead = onRead,
                    onCopy = onCopy,
                )
            }
        }
    }
}
