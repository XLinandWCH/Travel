package com.xlin.travel

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform