package com.xlin.travel

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello, Ktor!", response.bodyAsText())
    }

    /**
     * 模型配置接口的冒烟检查：路由已注册，且数据库未初始化时统一给 503（测试进程内不连 MySQL）。
     * token 鉴权在 ModelConfigService 里，需要真库才能验证。
     */
    @Test
    fun testModelConfigNeedsDatabase() = testApplication {
        application {
            module()
        }
        val response = client.post("/api/model/config") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"any"}""")
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }
}