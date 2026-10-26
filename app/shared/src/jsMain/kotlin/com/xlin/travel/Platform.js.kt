// jsMain/kotlin/com/xlin/travel/Platform.js.kt
package com.xlin.travel

class JSPlatform : Platform {
    override val name: String = "Web (Kotlin/JS)"
}

actual fun getPlatform(): Platform = JSPlatform()
