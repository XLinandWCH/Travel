package com.xlin.travel.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.io.File

/**
 * 网页版静态托管：把 :app:webApp:jsProductionWebpack 的产物目录挂到 /web。
 *
 * 目录由环境变量 TRAVEL_WEB_DIST 指定（默认 /opt/travel/web），与 API 同源部署时
 * 浏览器不会拦混合内容 / 跨域 cookie，教师端只需打开 http://服务器IP:8080/web/。
 */
fun Route.webConsoleRoutes() {
    val distDir = File(System.getenv("TRAVEL_WEB_DIST") ?: "/opt/travel/web")
    val index = File(distDir, "index.html")

    route("/web") {
        get {
            if (index.isFile) call.respondFile(index)
            else call.respondText("网页版未部署：请把 webApp 构建产物放到 ${distDir.absolutePath}")
        }
        // 其余子路径按文件名在产物目录内查找，限定在 distDir 内防路径穿越
        get("/{file...}") {
            val parts = call.parameters.getAll("file").orEmpty().filter { it.isNotBlank() && it != ".." }
            // /web/（带尾斜杠）的空路径同样返回入口 index，浏览器收藏/输入的都可用
            if (parts.isEmpty()) {
                return@get if (index.isFile) call.respondFile(index)
                else call.respondText("网页版未部署：请把 webApp 构建产物放到 ${distDir.absolutePath}")
            }
            val target = parts.fold(distDir) { dir, name -> File(dir, name) }
            if (!target.canonicalPath.startsWith(distDir.canonicalPath)) {
                return@get call.respond(HttpStatusCode.Forbidden)
            }
            if (target.isFile) call.respondFile(target)
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}
