package com.xlin.travel.agent

import com.xlin.travel.db.currentEpochMillis

/**
 * AI 请求的统一报错文案。
 *
 * 项目里所有走网络的 AI 能力（对话流式、语音识别 ASR、语音合成 TTS、识图）失败时，
 * 都经这里整理成一句用户能看懂的原因，来源分两类：
 * 1. 服务端 / 模型端已经写明原因的（如「登录已失效，请重新登录」），按原话展示；
 * 2. 网络层原生异常（连接被拒、DNS 失败、超时、证书错误、HTTP 状态码…）没有可读文案，
 *    按关键字归类翻译成中文提示。
 *
 * 另外，[SERVER_EXPIRED_AT_MILLIS]（后端停止维护的时刻）之后，任何 AI 报错都要在原因之后
 * 附上一段自部署说明，引导用户按项目根目录的《后端部署教程.md》把服务端跑起来。
 * 时间判断用设备本地时区的 Unix 毫秒，与配置缓存 [ModelConfigStore] 用的是同一个时钟。
 */

/** 后端服务停止维护的时刻：2026-11-19 14:40（北京时间 UTC+8）对应的 Unix 毫秒 */
const val SERVER_EXPIRED_AT_MILLIS = 1_795_070_400_000L

/** 到期后附在报错文案末尾的说明，指向随源码一起发布的《后端部署教程.md》 */
const val SERVER_EXPIRED_NOTICE =
    "服务器已到期，作者已不再进行服务器维护工作，请自行本地部署后端服务，后端部署服务教程：后端部署教程.md"

/** 设备当前时间是否已过后端停止维护的时刻 */
fun isServerExpired(): Boolean = currentEpochMillis() >= SERVER_EXPIRED_AT_MILLIS

/**
 * AI 请求失败的完整文案：「<action>失败：<原因>」；到期后另起一行附自部署说明。
 *
 * @param t 捕获到的异常，可为 null（此时原因写作「未知错误」）
 * @param action 请求类型的中文名：请求 / 语音识别 / 语音合成 / 识图
 */
fun aiErrorText(t: Throwable?, action: String = "请求"): String =
    withServerExpiryNotice("${action}失败：${describeAiError(t)}")

/** 已经在文案里写好原因时，只在到期后追加自部署说明；未到期原样返回 */
fun withServerExpiryNotice(reason: String): String =
    if (isServerExpired()) "$reason\n\n$SERVER_EXPIRED_NOTICE" else reason

/**
 * 把异常翻译成一句可读原因。
 *
 * 先按关键字归类（这些是各平台网络栈抛出的英文异常信息，用户看不懂），
 * 归不进去的再原样返回：服务端下发的提示本来就是中文说明；
 * 完全没有消息时退回异常类名或「未知错误」，并截断超长内容，避免撑爆气泡。
 */
fun describeAiError(t: Throwable?): String {
    val raw = t?.message?.trim().orEmpty().ifEmpty { t?.toString()?.trim().orEmpty() }
    if (raw.isEmpty()) return "未知错误"

    val lowered = raw.lowercase()
    categoryOf(lowered)?.let { return it }

    // 「服务异常（500）」这类只带状态码的包装：把括号里的三位码单独判一遍
    httpCodeOf(raw)?.let { return statusReason(it) }

    return raw.take(MaxReasonChars)
}

/**
 * 关键字 → 归类提示：命中顺序即优先级，越具体的错误排在越前面。
 * 只认网络栈的英文短语与状态码文字，不认裸数字（IP、端口里也可能出现三位数），
 * 中文提示留给下面的状态码解析与原样返回处理。
 */
private fun categoryOf(lowered: String): String? = when {
    containsAny(lowered, "connection refused", "failed to connect", "econnrefused",
        "no route to host", "network is unreachable", "connection abort", "connection reset") ->
        "无法连接到服务器（服务可能没启动，或地址、端口填错了）"

    containsAny(lowered, "unknownhost", "unable to resolve host", "name or service not known",
        "nodename nor servname", "getaddrinfo") ->
        "服务器地址解析失败，请检查网络或地址是否填错"

    containsAny(lowered, "timeout", "timed out") ->
        "请求超时，服务器响应太慢，请稍后重试"

    containsAny(lowered, "ssl", "certificate", "trust anchor", "handshake", "pkix") ->
        "HTTPS 证书校验失败，请检查系统时间是否正确、服务端证书是否有效"

    containsAny(lowered, "unauthorized") ->
        "身份验证失败（登录已过期或模型密钥不对），请重新登录后再试"

    containsAny(lowered, "invalid api-key", "invalid_api_key", "incorrect api key", "invalid api key") ->
        "模型密钥无效或已过期，请更新服务端的模型配置（llm_models / voice_models 表）"

    containsAny(lowered, "arrearage", "overdue", "arrears") ->
        "模型账号欠费，请先充值再试"

    containsAny(lowered, "forbidden") ->
        "服务器拒绝这次请求（权限不足或密钥不匹配）"

    containsAny(lowered, "not found") ->
        "请求的接口或模型不存在，请检查服务端配置"

    containsAny(lowered, "too many requests", "rate limit", "quota", "insufficient") ->
        "请求过于频繁或模型额度已用尽，请稍后再试"

    containsAny(lowered, "bad gateway", "service unavailable") ->
        "服务端或模型服务暂时不可用，请稍后重试"

    containsAny(lowered, "internal server error") ->
        "服务端内部错误，请稍后重试"

    containsAny(lowered, "json parse", "failed to parse", "decoding", "serializ", "deserializ") ->
        "服务端返回的内容无法解析，请确认服务端版本与客户端是否匹配"

    containsAny(lowered, "content policy", "data inspection", "inappropriate", "content_filter") ->
        "这段内容触发了模型安全审核，请换一种说法再试"

    containsAny(lowered, "not supported", "unsupported") ->
        "服务端配置的模型不支持这个能力（如图片输入），请换模型或调整服务端配置"

    else -> null
}

/** 错误信息里的 HTTP 状态码：兼容「（500）」「status=503」「code: 429」等写法，取不到返回 null */
private fun httpCodeOf(raw: String): Int? {
    val lowered = raw.lowercase()
    return sequenceOf(BracketCode, StatusKeywordCode)
        .mapNotNull { it.find(lowered)?.groupValues?.get(1)?.toIntOrNull() }
        .firstOrNull()
}

private val BracketCode = Regex("（(\\d{3})）")
private val StatusKeywordCode = Regex("(?:status|code)[^0-9]{0,10}(\\d{3})(?!\\d)")

private fun statusReason(code: Int): String = when {
    code == 401 || code == 403 -> "身份验证失败（登录已过期或模型密钥不对），请重新登录后再试"
    code == 404 -> "请求的接口或模型不存在，请检查服务端配置"
    code == 429 -> "请求过于频繁或模型额度已用尽，请稍后再试"
    code in 500..599 -> "服务端或模型服务暂时不可用，请稍后重试"
    else -> "服务异常（$code）"
}

private fun containsAny(text: String, vararg keys: String): Boolean = keys.any { text.contains(it) }

/** 原样展示的错误信息截断长度，避免超长堆栈把气泡撑满 */
private const val MaxReasonChars = 200
