package com.xlin.travel.routes

import com.xlin.travel.db.DatabaseFactory
import com.xlin.travel.models.ApiError
import com.xlin.travel.models.ModelConfigRequest
import com.xlin.travel.services.ModelConfigService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * 模型配置路由：客户端用它替换掉原先硬编码在 AI_Agent.{android,ios,jvm}.kt 里的
 * API_KEY / baseUrl / 模型 id / 系统提示词 / 温度。
 *
 * 仅登录后（携带 /api/auth 签发的 token）可访问，token 无效返回 401。
 */
fun Route.modelConfigRoutes() {
    route("/api/model") {

        post("/config") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<ModelConfigRequest>() ?: return@post badRequest(call)
            when (val result = ModelConfigService.getConfig(request.token, request.modelId, request.scope)) {
                is ModelConfigService.ConfigResult.Success -> call.respond(result.config)
                is ModelConfigService.ConfigResult.Failed ->
                    call.respond(HttpStatusCode.fromValue(result.status), ApiError(result.message))
            }
        }
    }
}
