package com.xlin.travel.videoexplain.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

// 与 ExplainStage 外层卡片一致的圆角，让相机画面也守住圆角
private val PreviewCorner = 24.dp

/**
 * Android 真机摄像头预览：CameraX [PreviewView] + 生命周期绑定。
 *
 * 流程：进页即申请 CAMERA 权限 → 授权后 bindToLifecycle（后置摄像头优先，
 * 设备无后置时回退前置）→ 画面中心裁剪铺满并按卡片圆角裁切。权限被拒 /
 * 无相机 / 绑定失败时降级为 [CameraFallback] 并写明原因；[cameraOn] 变 false
 * 或离开页面时解绑，避免摄像头指示灯长亮。
 */
@Composable
actual fun CameraPreview(cameraOn: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var note by remember { mutableStateOf<String?>(null) }
    var bound by remember { mutableStateOf(false) }
    val providerRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // COMPATIBLE 走 TextureView：兼容性更好，也能配合 outlines 裁圆角；
            // 未提供画面时保持透明，露出底下的取景占位
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            setBackgroundColor(AndroidColor.TRANSPARENT)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok ->
        granted = ok
        note = if (ok) null else "未获得相机权限，可在系统设置里开启"
    }

    LaunchedEffect(cameraOn) {
        if (cameraOn && !granted) launcher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(density) {
        val radius = with(density) { PreviewCorner.toPx() }
        previewView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        previewView.clipToOutline = true
    }

    LaunchedEffect(granted, cameraOn) {
        if (!granted || !cameraOn) {
            providerRef.value?.let { runCatching { it.unbindAll() } }
            bound = false
            return@LaunchedEffect
        }
        val provider = runCatching { awaitCameraProvider(context) }.getOrNull()
        if (provider == null) {
            note = "相机不可用，显示取景占位"
            bound = false
            return@LaunchedEffect
        }
        providerRef.value = provider
        val preview = Preview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
        }
        val result = runCatching {
            provider.unbindAll()
            // 讲解的是街景：后置镜头为主，个别设备没有后置时再回退到前置
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        }.recoverCatching {
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
        }
        bound = result.isSuccess
        if (result.isSuccess) note = null else note = "无法打开摄像头，显示取景占位"
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { providerRef.value?.unbindAll() }
            providerRef.value = null
        }
    }

    Box(modifier.fillMaxSize().background(Color(0xFF120D0A))) {
        // 未拿到画面时露出的底图；相机开始出图后自然被遮住
        if (!bound) CameraFallback(note = note, modifier = Modifier.fillMaxSize())
        // PreviewView 始终保持 attach：先挂窗口再绑 use case，避免 surface 请求落空
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/** CameraX 的 provider 是 ListenableFuture，转成 suspend 等待初始化完成 */
private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { provider ->
                        if (cont.isActive) cont.resumeWith(Result.success(provider))
                    }
                    .onFailure { error -> if (cont.isActive) cont.resumeWithException(error) }
            },
            ContextCompat.getMainExecutor(context),
        )
        cont.invokeOnCancellation { future.cancel(true) }
    }
