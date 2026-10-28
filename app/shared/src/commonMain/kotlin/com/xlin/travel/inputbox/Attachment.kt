package com.xlin.travel.inputbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AttachmentHintText
import com.xlin.travel.color.AttachmentTileBackground
import com.xlin.travel.color.AttachmentTileIcon
import com.xlin.travel.color.AttachmentTilePressed
import com.xlin.travel.color.AttachmentTileText
import com.xlin.travel.color.FileChipBackground
import com.xlin.travel.color.FileChipText
import com.xlin.travel.color.RemoveButtonBackground
import com.xlin.travel.image_picker.FileIcons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.camera
import travel.app.shared.generated.resources.close_path
import travel.app.shared.generated.resources.file
import travel.app.shared.generated.resources.image

/**
 * 「+」展开后的附件动作面板：拍照 / 相册 / 本地文件 三个等宽瓷砖，
 * 对应 UI设计/输入框文件参考UI初稿.html 的 action-grid。黑白配色，按压态轻微加深。
 */
@Composable
fun AttachmentActionsRow(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onFile: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AttachmentTile(
            icon = Res.drawable.camera,
            label = "拍照",
            onClick = onCamera,
            modifier = Modifier.weight(1f),
        )
        AttachmentTile(
            icon = Res.drawable.image,
            label = "相册",
            onClick = onGallery,
            modifier = Modifier.weight(1f),
        )
        AttachmentTile(
            icon = Res.drawable.file,
            label = "本地文件",
            onClick = onFile,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 单个动作瓷砖：图标在上、文字在下，圆角矩形底，按压换深色 */
@Composable
private fun AttachmentTile(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) AttachmentTilePressed else AttachmentTileBackground)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(AttachmentTileIcon),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(label, color = AttachmentTileText, fontSize = 14.sp)
    }
}

/** 上部图片预览：展示真实图片 + 右上角移除按钮（使用 close_path.xml） */
@Composable
fun ImagePreview(image: ImageBitmap, onRemove: () -> Unit) {
    Box {
        Image(
            bitmap = image,
            contentDescription = "已选择的图片",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        // 右上角删除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(RemoveButtonBackground)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.close_path),
                contentDescription = "移除图片",
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** 已选文档的文件条幅：文件图标 + 文件名（过长省略）+ 右侧移除按钮 */
@Composable
fun FileChip(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FileChipBackground)
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(FileIcons.iconForName(name)),
            contentDescription = "已选择的文件",
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = name,
            color = FileChipText,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.close_path),
                contentDescription = "移除文件",
                colorFilter = ColorFilter.tint(FileChipText),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** 附件瞬时提示横幅（如「不支持的文件格式」）：仅在传入非空文案时由上层驱动显示 */
@Composable
fun AttachmentHintBanner(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = message,
            color = AttachmentHintText,
            fontSize = 12.5.sp,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        )
    }
}
