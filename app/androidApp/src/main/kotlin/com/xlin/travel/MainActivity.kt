package com.xlin.travel

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.xlin.travel.color.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 系统栏透明（内容画到状态栏/导航栏之下），初始按深色主题给白色图标，
        // 之后由 SyncSystemBarWithTheme 跟随应用主题偏好实时切换深浅
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            SyncSystemBarWithTheme()
            App()
        }
    }
}

/**
 * 让状态栏时间/电量、导航栏图标的深浅跟随 [AppTheme.isDark]：
 * 浅色主题 → 黑色图标；深色主题 → 白色图标。
 * 用强制 dark 样式初始化后不再受系统深色切换干扰，图标完全由这里控制。
 */
@Composable
private fun SyncSystemBarWithTheme() {
    // 读取快照状态：主题偏好或系统深浅变化时此处重组，LaunchedEffect 重新执行
    val isDark = AppTheme.isDark
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as? ComponentActivity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}