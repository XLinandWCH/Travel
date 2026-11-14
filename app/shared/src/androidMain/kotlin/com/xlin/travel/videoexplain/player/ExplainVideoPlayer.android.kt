package com.xlin.travel.videoexplain.player

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/** Compose Resources 在 Android 端的打包位置（build 产物里已确认） */
private const val ASSET_VIDEO =
    "composeResources/travel.app.shared.generated.resources/files/videos/video_1.mp4"

/** 复制到缓存目录后的文件名，带版本后缀便于以后换视频时失效旧缓存 */
private const val CACHE_NAME = "explain_video_v1.mp4"

/** 小于这个体积视为半途复制失败，重新复制 */
private const val MIN_VALID_BYTES = 1_000_000L

/**
 * Android 端播放：MediaPlayer + TextureView。
 * 用 TextureView 而不是 VideoView（SurfaceView）是因为 SurfaceView 不遵守
 * 外层 24dp 圆角裁剪，四角会露出黑色直角。
 * 画面按中心裁剪（cover）铺满，不拉伸变形。
 */
@Composable
actual fun ExplainVideoPlayer(playing: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember { MediaPlayer() }
    var videoPath by remember { mutableStateOf<String?>(null) }
    var surface by remember { mutableStateOf<Surface?>(null) }
    var prepared by remember { mutableStateOf(false) }
    var videoWidth by remember { mutableStateOf(0) }
    var videoHeight by remember { mutableStateOf(0) }

    // 首次进入把 assets 里的视频复制到 cacheDir：MediaPlayer 不能直接播 assets 内部的路径
    LaunchedEffect(Unit) {
        videoPath = withContext(Dispatchers.IO) { ensureCachedVideo(context) }
    }
    DisposableEffect(player) {
        onDispose {
            runCatching { player.setSurface(null) }
            runCatching { player.release() }
        }
    }
    // 缓存文件与 Surface 都就绪后只 prepare 一次；之后靠 playing 切播放 / 暂停
    LaunchedEffect(videoPath, surface) {
        val path = videoPath ?: return@LaunchedEffect
        val target = surface ?: return@LaunchedEffect
        if (prepared) return@LaunchedEffect
        runCatching {
            player.setSurface(target)
            player.setDataSource(path)
            player.isLooping = true
            player.setVolume(0f, 0f)
            player.setOnVideoSizeChangedListener { _, width, height ->
                videoWidth = width
                videoHeight = height
            }
            player.setOnPreparedListener {
                prepared = true
                if (playing) runCatching { it.start() }
            }
            player.prepareAsync()
        }
    }
    LaunchedEffect(playing, prepared) {
        if (!prepared) return@LaunchedEffect
        runCatching { if (playing) player.start() else player.pause() }
    }

    // 视频还在复制（约几百毫秒）时先铺讲解画面同款深色渐变，不闪黑
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF2A1D14), Color(0xFF120D0A)))
        ),
    ) {
        if (videoPath != null) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                st: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                surface = Surface(st)
                                applyCenterCrop(this@apply, videoWidth, videoHeight)
                            }

                            override fun onSurfaceTextureSizeChanged(
                                st: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                applyCenterCrop(this@apply, videoWidth, videoHeight)
                            }

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                surface = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 中心裁剪：按较大的缩放比铺满，多出的部分裁掉，保持视频比例不变形 */
private fun applyCenterCrop(view: TextureView, videoWidth: Int, videoHeight: Int) {
    if (videoWidth <= 0 || videoHeight <= 0) return
    val viewWidth = view.width.toFloat()
    val viewHeight = view.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f) return
    val scale = max(viewWidth / videoWidth, viewHeight / videoHeight)
    view.setTransform(
        Matrix().apply { setScale(scale, scale, viewWidth / 2f, viewHeight / 2f) }
    )
}

/** 复制一次即可：缓存已存在且体积正常就直接复用，避免每次进页面都拷 30MB */
private fun ensureCachedVideo(context: Context): String? = runCatching {
    val target = File(context.cacheDir, CACHE_NAME)
    if (target.length() > MIN_VALID_BYTES) return@runCatching target.absolutePath
    context.assets.open(ASSET_VIDEO).use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    }
    target.absolutePath
}.getOrNull()
