package com.xlin.travel.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 定位能力的进程级单点状态（应用不再有专门的地图页面）：
 * 启动时自动申请权限并静默定位一次，结果缓存于 [lastLocation] 供后续业务消费
 * （地图工具 / 周边查询 / 高德 SDK 接入等）；
 * 用户手动关闭定位（权限被拒）时 [needEnableReminder] 置真，由聊天页顶部弹提醒，
 * 引导去系统设置打开，不跳转专门页面。
 */
object LocationTracker {

    /** 最近一次成功定位的结果；null = 本次启动还没定位成功 */
    var lastLocation: LocationInfo? by mutableStateOf(null)
        private set

    /** 需要提醒用户打开定位：自启申请权限被拒（含桌面端无定位能力） */
    var needEnableReminder: Boolean by mutableStateOf(false)
        private set

    /**
     * 应用自启入口（[com.xlin.travel.App] 的 appReady 后执行一次）：
     * 已有定位结果则跳过；未决定时经 [rememberLocationPermission] 拉起系统授权框，
     * 授权通过静默定位一次，被拒绝则置 [needEnableReminder] 交给界面提醒。
     */
    @Composable
    fun StartupEffect() {
        if (lastLocation != null) return
        val requestPermission = rememberLocationPermission { granted ->
            if (granted) startLocating() else needEnableReminder = true
        }
        LaunchedEffect(requestPermission) { requestPermission() }
    }

    /** 主动刷新定位（如后续工具调用需要新鲜位置时）：不弹权限框，直接走系统定位 */
    fun refresh() = startLocating()

    /** 挂起定位放到独立作用域里跑：权限回调可能同步触发，不能阻塞组合线程 */
    private fun startLocating() {
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { currentLocation() }
                .onSuccess {
                    lastLocation = it
                    needEnableReminder = false
                }
            // 定位服务关闭 / 超时等失败静默：只在权限被拒时提醒，避免打断操作
        }
    }
}
