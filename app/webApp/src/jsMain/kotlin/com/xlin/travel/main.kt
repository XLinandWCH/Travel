package com.xlin.travel

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // 浏览器端 skiko 依赖页面的字体环境光栅化文字；Firefox 等浏览器对系统中文字体是懒加载的，
    // 首帧若抢在字体就绪前绘制会显示豆腐块。这里先预热含中文回退的泛型字体，
    // 再等 document.fonts.ready 完成后才挂载 Compose（index.html 的加载动画正好覆盖这段时间）。
    val fonts: dynamic = js("typeof document.fonts !== 'undefined' ? document.fonts : null")
    if (fonts == null) {
        startApp()
    } else {
        fonts.load("16px sans-serif", "中文")
        fonts.load("16px serif", "中文")
        fonts.load("16px monospace", "中文")
        fonts.ready.then { startApp() }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun startApp() {
    ComposeViewport(document.body!!) {
        App()
    }
    // 挂载后移除 index.html 里的加载动画
    document.getElementById("loading")?.let { it.parentNode?.removeChild(it) }
}
