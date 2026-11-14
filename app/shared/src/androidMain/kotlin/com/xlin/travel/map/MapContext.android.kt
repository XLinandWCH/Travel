package com.xlin.travel.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** 组合期捕获 Android Context */
@Composable
actual fun mapPlatformContext(): Any? = LocalContext.current

/** 保存 Context 供挂起定位使用（LocationManager 需要） */
actual fun initMapContext(context: Any?) {
    androidMapContext = context as? Context
}

/** 由 [initMapContext] 写入，[locationProvider] 读取；同一进程内单值即可 */
internal var androidMapContext: Context? = null
