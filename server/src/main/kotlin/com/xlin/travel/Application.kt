package com.xlin.travel

import com.xlin.travel.db.DatabaseFactory
import com.xlin.travel.db.ModelKeySeeder
import com.xlin.travel.routes.authRoutes
import com.xlin.travel.routes.feedbackRoutes
import com.xlin.travel.routes.modelConfigRoutes
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
    // 开发阶段放开跨域，正式客户端（Ktor）不需要，可按需收紧
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }
    routing {
        get("/") {
            call.respondText(sayHello("Ktor"))
        }
        // 账号服务（注册 / 登录）、模型配置下发、反馈 / 消息服务接入数据库
        authRoutes()
        modelConfigRoutes()
        feedbackRoutes()
    }
}
