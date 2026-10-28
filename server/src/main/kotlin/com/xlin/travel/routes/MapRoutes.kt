package com.xlin.travel.routes

import com.xlin.travel.db.DatabaseFactory
import com.xlin.travel.models.ApiError
import com.xlin.travel.models.MapKeysRequest
import com.xlin.travel.services.MapKeyService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * 地图密钥路由：客户端（commonMain 的 MapKeysStore）登录后按平台拉取高德 Key，
 * 不再把密钥打进安装包。仅登录后（携带 /api/auth 签发的 token）可访问，token 无效返回 401。
 */
fun Route.mapRoutes() {
    route("/api/map") {

        post("/keys") {
            if (!DatabaseFactory.isInitialized) return@post databaseUnavailable(call)
            val request = call.receiveOrNull<MapKeysRequest>() ?: return@post badRequest(call)
            when (val result = MapKeyService.getKeys(request.token, request.platform)) {
                is MapKeyService.Result.Success -> call.respond(result.keys)
                is MapKeyService.Result.Failed ->
                    call.respond(HttpStatusCode.fromValue(result.status), ApiError(result.message))
            }
        }
    }
}
