package com.xlin.travel.videoexplain

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.TextGray400
import com.xlin.travel.videoexplain.camera.CameraPreview
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.camera

/**
 * 摄像头画面：接真机摄像头（Android CameraX），上面叠 [REC 时间码] [设备胶囊] [对焦提示] 这些
 * 通话式遮罩，形成「视频通话已接通」的观感。
 *
 * 画面本身由 [CameraPreview] 的 actual 提供：Android 是 CameraX 实时预览，
 * 拿不到权限 / 没相机时降级为取景占位（CameraFallback）；iOS / JVM / Web 暂用占位。
 * 关闭摄像头按钮时切到深色关闭态，再次点开重新申请画面。
 * 占位画面的剪影与网格绘制在 CameraPainting.kt。
 */

/** 画面内文字固定用近白：摄像头画面本身是深色，不能跟随主题反转成黑字 */
private val LiveText = Color(0xFFF2F3F5)
private val LiveTextDim = Color(0xFFB9B4AF)

@Composable
internal fun CameraStage(cameraOn: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3A2A1E), Color(0xFF1B140F), Color(0xFF120D0A))
                )
            ),
    ) {
        if (cameraOn) {
            // 真机预览（Android）/ 取景占位（其它平台）
            CameraPreview(cameraOn = true, modifier = Modifier.fillMaxSize())
            CallBadge(Modifier.align(Alignment.TopStart).padding(14.dp))
            CameraChip(
                text = "后置摄像头 · HD",
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
            )
            FocusHint(Modifier.align(Alignment.BottomEnd).padding(16.dp))
        } else {
            CameraOff(Modifier.align(Alignment.Center))
        }
    }
}

/** 左上角状态徽标：橙点呼吸 + 通话计时（真机画面上不写 REC，避免误以为在录像） */
@Composable
private fun CallBadge(modifier: Modifier = Modifier) {
    var seconds by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            seconds++
        }
    }
    val transition = rememberInfiniteTransition(label = "call")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
        label = "call-alpha",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(AccentOrange.copy(alpha = alpha)),
        )
        Spacer(Modifier.width(6.dp))
        Text("通话中", color = LiveText, fontSize = 11.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = "00:" + (seconds % 60).toString().padStart(2, '0'),
            color = LiveTextDim,
            fontSize = 11.sp,
        )
    }
}

/** 右上角设备信息胶囊 */
@Composable
private fun CameraChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = LiveTextDim,
        fontSize = 11.sp,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

/** 右下角对焦提示：小圆点 + 文案 */
@Composable
private fun FocusHint(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(LiveText.copy(alpha = 0.7f)),
        )
        Text("已对焦", color = LiveTextDim, fontSize = 11.sp)
    }
}

/** 摄像头关闭态：与深色画面形成对比的灰字提示 */
@Composable
private fun CameraOff(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.camera),
            contentDescription = null,
            colorFilter = ColorFilter.tint(TextGray400),
            modifier = Modifier.size(36.dp),
        )
        Text("摄像头已关闭", color = TextGray400, fontSize = 13.sp)
    }
}
