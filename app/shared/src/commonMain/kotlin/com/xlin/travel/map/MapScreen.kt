package com.xlin.travel.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.SettingCardBackground
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.TextWhite
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.navigation.PageTopBar
import kotlinx.coroutines.launch

/**
 * 地图功能第一版演示页：验证 MAP 分层跑通「定位」这条最小链路。
 *
 * 页面只做两件事：
 * 1. 拉取服务端下发的当前平台高德密钥（POST /api/map/keys，密钥不落安装包），展示就绪状态；
 * 2. 点击「获取当前位置」→ 先走 [rememberLocationPermission] 申请权限 →
 *    授权通过后调用 [currentLocation]（commonMain expect，各端 actual 走系统定位）展示坐标。
 *
 * 后续接入高德 MapView / 路径规划时，本页面升级为真实地图容器，
 * commonMain 的业务模型（GeoPoint / RoutePoint / Station / TravelRoute）即为数据底座。
 */
@Composable
fun MapScreen(
    repository: ChatRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiScope = rememberCoroutineScope()
    // 挂起函数里取不到 CompositionLocal：组合期捕获平台上下文并注入（仅 Android 定位使用）
    initMapContext(mapPlatformContext())
    var loading by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<LocationInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var keysReady by remember { mutableStateOf(false) }
    var keysError by remember { mutableStateOf<String?>(null) }

    // 进入页面即拉取当前平台密钥（登录后），成功仅记状态、失败展示原因不阻塞定位演示
    LaunchedEffect(repository) {
        runCatching { MapKeysStore.ensure(repository) }
            .onSuccess { keysReady = true }
            .onFailure { keysError = it.message }
    }

    // 定位权限申请（expect/actual），授权回调里再真正发起定位
    val requestPermission = rememberLocationPermission { granted ->
        if (granted) {
            uiScope.launch {
                loading = true
                error = null
                runCatching { currentLocation() }
                    .onSuccess { location = it }
                    .onFailure { error = it.message }
                loading = false
            }
        } else {
            error = "没有定位权限，无法演示定位功能"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding(),
    ) {
        PageTopBar(title = "地图 · 定位", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 密钥状态卡：验证「服务端下发、不硬编码」链路
            InfoCard(
                title = "高德密钥（当前平台：$mapPlatformKey）",
                text = when {
                    keysReady -> "已从服务端拉取成功，后续高德 SDK 初始化直接使用"
                    keysError != null -> "拉取失败：$keysError"
                    else -> "正在拉取…"
                },
            )

            // 定位结果卡
            InfoCard(
                title = "当前位置",
                text = location?.let {
                    "经度 ${fmt(it.point.lng)}\n纬度 ${fmt(it.point.lat)}\n" +
                        "精度约 ${fmt(it.accuracyMeters)} 米"
                } ?: "尚未定位",
            )

            error?.let {
                Text(text = it, color = AccentOrange, fontSize = 13.sp)
            }

            Button(
                onClick = requestPermission,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = TextWhite,
                    )
                }
                Text(if (location == null) "获取当前位置" else "重新定位", fontSize = 15.sp)
            }
        }
    }
}

/** 演示页信息卡：标题 + 正文，与设置页卡片同款底色 */
@Composable
private fun InfoCard(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SettingCardBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(text = text, color = TextGray500, fontSize = 13.sp)
    }
}

/** 坐标 / 精度展示：最多保留 6 位小数，去掉尾零 */
private fun fmt(v: Double): String {
    val s = v.toString()
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}
