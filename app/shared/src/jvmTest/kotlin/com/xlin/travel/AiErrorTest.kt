package com.xlin.travel

import com.xlin.travel.agent.SERVER_EXPIRED_AT_MILLIS
import com.xlin.travel.agent.SERVER_EXPIRED_NOTICE
import com.xlin.travel.agent.aiErrorText
import com.xlin.travel.agent.describeAiError
import com.xlin.travel.agent.isServerExpired
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AI 报错文案：网络层异常要被翻译成可读原因，服务端已经写明的原因不能被改写，
 * 到期时刻（2026-11-19 14:40）之后才附加自部署说明。
 */
class AiErrorTest {

    @Test
    fun networkFailuresBecomeReadableReasons() {
        // 各平台连接失败的说法不同，但都归到「连不上服务器」
        assertEquals(
            "无法连接到服务器（服务可能没启动，或地址、端口填错了）",
            describeAiError(Exception("Failed to connect to /193.112.23.199:8080")),
        )
        assertEquals(
            "请求超时，服务器响应太慢，请稍后重试",
            describeAiError(Exception("java.net.SocketTimeoutException: Read timed out")),
        )
        // 括号里的状态码单独判一遍（配置接口的兜底文案就是这种写法）
        assertEquals(
            "身份验证失败（登录已过期或模型密钥不对），请重新登录后再试",
            describeAiError(IllegalStateException("服务异常（401）")),
        )
    }

    @Test
    fun serverWrittenReasonIsKeptAsIs() {
        // 服务端写的是人话，不能再被归类成通用提示丢掉细节
        assertEquals("登录已失效，请重新登录", describeAiError(Exception("登录已失效，请重新登录")))
        assertEquals("未知错误", describeAiError(null))
    }

    @Test
    fun errorTextCarriesActionPrefix() {
        val text = aiErrorText(Exception("Connection refused"), "语音识别")
        assertTrue(text.startsWith("语音识别失败：无法连接到服务器"), text)
        // 说明只在到期后出现，未到期保持原来的单行提示
        assertEquals(isServerExpired(), text.contains(SERVER_EXPIRED_NOTICE))
    }

    @Test
    fun expiryDeadlineIsBeijingTime() {
        // 常量必须等于 2026-11-19 14:40（UTC+8），改一处就要跟着改教程里的说法
        assertEquals(
            OffsetDateTime.parse("2026-11-19T14:40+08:00").toInstant().toEpochMilli(),
            SERVER_EXPIRED_AT_MILLIS,
        )
        assertTrue(SERVER_EXPIRED_NOTICE.endsWith("后端部署服务教程：后端部署教程.md"))
    }
}
