package com.xlin.travel

import com.xlin.travel.db.DatabaseFactory
import com.xlin.travel.db.MapKeySeeder
import com.xlin.travel.db.ModelKeySeeder
import com.xlin.travel.routes.authRoutes
import com.xlin.travel.routes.feedbackRoutes
import com.xlin.travel.routes.llmProxyRoutes
import com.xlin.travel.routes.mapRoutes
import com.xlin.travel.routes.modelConfigRoutes
import com.xlin.travel.routes.webConsoleRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main() {
    // 数据库初始化失败不阻断服务启动：登录 / 注册接口会返回 503，便于先排查配置
    runCatching {
        DatabaseFactory.init()
        // 保证 model_api_keys 有默认模型记录（密钥靠 MODEL_API_KEY 注入，不写在代码里）
        ModelKeySeeder.seed()
        // 保证 map 表有高德密钥记录（ANDROID / IOS / WEB 各一行，可用 MAP_*_KEY 注入轮换）
        MapKeySeeder.seed()
    }.onFailure {
        LoggerFactory.getLogger("Application").error("MySQL 初始化失败（登录/注册接口暂不可用）", it)
    }

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    // 开发阶段放开跨域，正式客户端（Ktor）不需要，可按需收紧；
    // Authorization 供网页版 LLM 代理携带，必须允许跨域预检通过
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    routing {
        get("/") {
            call.respondText(sayHello("Ktor"))
        }
        // 账号服务（注册 / 登录）、模型配置下发、地图密钥下发、反馈 / 消息服务接入数据库
        authRoutes()
        modelConfigRoutes()
        mapRoutes()
        feedbackRoutes()
        // 网页版大模型反向代理（浏览器直连模型服务会被 CORS 拦截）
        llmProxyRoutes()
        // 网页版静态托管（/web，产物目录由 TRAVEL_WEB_DIST 指定）
        webConsoleRoutes()
    }
}
