package com.xlin.travel.videoexplain

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.IconButtonBackground
import com.xlin.travel.color.TextGray400
import com.xlin.travel.color.TextWhite
import com.xlin.travel.color.WhiteBtnBackground
import com.xlin.travel.color.WhiteBtnIcon
import com.xlin.travel.db.ChatRepository
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.camera
import travel.app.shared.generated.resources.close_path
import travel.app.shared.generated.resources.microphone
import travel.app.shared.generated.resources.phone_number
import travel.app.shared.generated.resources.subtitle

/**
 * 视频讲解（模拟通话页）：参考 UI设计/视频讲解参考UI.html 的版式，
 * 配色全部换成项目自己的 token（聊天背景 / 卡片底 / 强调橙），不引入新色相。
 *
 * 画面区有三种状态（见 [ExplainMode]）：
 * 1. 进页面默认摄像头预览；
 * 2. 点麦克风说话，识别结果里带「生成视频」时进入 3 秒生成动画；
 * 3. 动画走满自动把画面换成讲解视频（video_1.mp4，静音循环）。
 *
 * 不接真实摄像头：画面是 Canvas 拼的取景预览（见 [CameraStage]），
 * 麦克风走的是与输入框同一套录音 + Fun-ASR-Flash 识别。
 */
@Composable
fun VideoExplainScreen(
    onClose: () -> Unit,
    repository: ChatRepository,
    modifier: Modifier = Modifier,
) {
    // 摄像头默认打开；两个开关只影响视觉，不申请摄像头权限
    var cameraOn by remember { mutableStateOf(true) }
    var subtitleOn by remember { mutableStateOf(true) }
    // 画面状态：摄像头 → 生成动画 → 视频
    var mode by remember { mutableStateOf(ExplainMode.Camera) }
    // 语音面板是否展开；heard 是本次识别到的原话，兼作字幕
    var listening by remember { mutableStateOf(false) }
    var heard by remember { mutableStateOf<String?>(null) }

    // 生成动画走满 3 秒后自动切到视频，并确保字幕打开
    LaunchedEffect(mode) {
        if (mode == ExplainMode.Generating) {
            delay(ExplainGeneratingMillis)
            mode = ExplainMode.Video
            subtitleOn = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding()
            .padding(horizontal = 16.dp),
    ) {
        VideoTopBar(onClose = onClose)

        ExplainStage(
            mode = mode,
            cameraOn = cameraOn,
            subtitleOn = subtitleOn,
            // 用户说过话就用原话当字幕，否则按当前画面给固定解说词
            subtitle = heard?.let { "「$it」" }
                ?: if (mode == ExplainMode.Video) VideoDefaultSubtitle else CameraDefaultSubtitle,
            listening = listening,
            repository = repository,
            onRecognized = { text ->
                heard = text
                listening = false
                // 说了「可以帮我生成视频吗」这类话：进入 3 秒生成动画
                if (asksForExplainVideo(text)) mode = ExplainMode.Generating
            },
            onCloseListening = { listening = false },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 12.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StageButton(
                icon = Res.drawable.camera,
                active = cameraOn && mode == ExplainMode.Camera,
                description = if (cameraOn && mode == ExplainMode.Camera) "关闭摄像头" else "打开摄像头",
                onClick = {
                    if (mode == ExplainMode.Camera) {
                        cameraOn = !cameraOn
                    } else {
                        // 正在生成 / 正在播视频时点摄像头：切回摄像头画面并打开
                        mode = ExplainMode.Camera
                        cameraOn = true
                        listening = false
                    }
                },
            )
            StageButton(
                icon = Res.drawable.microphone,
                active = listening,
                description = if (listening) "关闭语音" else "语音说一句",
                onClick = {
                    // 生成动画进行中不打断，避免动画与语音面板叠在一起
                    if (mode != ExplainMode.Generating) listening = !listening
                },
            )
            StageButton(
                icon = Res.drawable.subtitle,
                active = subtitleOn,
                description = if (subtitleOn) "关闭字幕" else "打开字幕",
                onClick = { subtitleOn = !subtitleOn },
            )
            // 挂断：强调橙实底，点它等同退出本页
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(AccentOrange)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.phone_number),
                    contentDescription = "结束视频讲解",
                    colorFilter = ColorFilter.tint(TextWhite),
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Text(
            text = "内容由 AI 生成",
            color = TextGray400,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
        )
    }
}

/** 顶栏：左侧关闭、居中「视频讲解」胶囊；右侧留空让胶囊保持居中 */
@Composable
private fun VideoTopBar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IconButtonBackground)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.close_path),
                contentDescription = "退出视频讲解",
                colorFilter = ColorFilter.tint(TextWhite),
                modifier = Modifier.size(20.dp),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(20.dp))
                .background(IconButtonBackground)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulsingDot(size = 8.dp)
            Spacer(Modifier.width(8.dp))
            Text(text = "视频讲解", color = TextWhite, fontSize = 15.sp)
        }
    }
}

/** 底部圆形开关：开启用主题反转底 + 深图标，关闭退回卡片底 + 灰图标 */
@Composable
private fun StageButton(
    icon: DrawableResource,
    active: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(if (active) WhiteBtnBackground else IconButtonBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(if (active) WhiteBtnIcon else TextGray400),
            modifier = Modifier.size(26.dp),
        )
    }
}
