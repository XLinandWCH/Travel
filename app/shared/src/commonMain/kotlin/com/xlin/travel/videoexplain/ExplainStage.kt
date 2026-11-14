package com.xlin.travel.videoexplain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.IconButtonBackground
import com.xlin.travel.color.TextWhite
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.videoexplain.player.ExplainVideoPlayer

/**
 * 讲解画面舞台：按 [ExplainMode] 换成摄像头预览 / 生成动画 / 讲解视频，
 * 上面再叠状态胶囊、字幕条与语音面板。
 *
 * 单独成文件，避免主页面超过项目约定的 250 行。
 */
@Composable
internal fun ExplainStage(
    mode: ExplainMode,
    cameraOn: Boolean,
    subtitleOn: Boolean,
    subtitle: String,
    listening: Boolean,
    repository: ChatRepository,
    onRecognized: (String) -> Unit,
    onCloseListening: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ChatBackground),
    ) {
        when (mode) {
            ExplainMode.Camera -> CameraStage(
                cameraOn = cameraOn,
                modifier = Modifier.fillMaxSize(),
            )
            ExplainMode.Generating -> GeneratingStage(
                modifier = Modifier.fillMaxSize(),
            )
            ExplainMode.Video -> ExplainVideoPlayer(
                playing = true,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 视频模式左上角给一颗状态胶囊；摄像头模式自带 REC 徽标，生成模式由动画层给标题
        if (mode == ExplainMode.Video) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(IconButtonBackground.copy(alpha = 0.72f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulsingDot(size = 9.dp)
                Spacer(Modifier.width(8.dp))
                Text("正在播放讲解视频", color = TextWhite, fontSize = 13.sp)
            }
        }

        // 字幕：聆听时先让位给语音面板，避免两块文字叠在底部
        if (!listening) {
            AnimatedVisibility(
                visible = subtitleOn,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = subtitle,
                    color = TextWhite,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ChatBackground.copy(alpha = 0.72f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        // 语音面板：从底部浮起，尺寸与图标与输入框语音模式完全一致
        AnimatedVisibility(
            visible = listening,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp),
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut() + slideOutVertically { it / 3 },
        ) {
            ExplainVoicePanel(
                repository = repository,
                onRecognized = onRecognized,
                onClose = onCloseListening,
            )
        }
    }
}

/** 呼吸圆点：表示正在讲 / 正在播，颜色固定为强调橙 */
@Composable
internal fun PulsingDot(size: Dp) {
    val transition = rememberInfiniteTransition(label = "explain-dot")
    val scale by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot-scale",
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(AccentOrange),
    )
}
