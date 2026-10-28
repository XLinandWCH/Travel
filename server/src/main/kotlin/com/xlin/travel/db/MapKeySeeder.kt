package com.xlin.travel.db

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

/**
 * 地图密钥初始化：保证 map 表里 ANDROID / IOS / WEB 各有一条高德 Key 记录。
 *
 * 与 ModelKeySeeder 同样的安全约定：密钥是机密，正式环境通过 JVM 系统属性或环境变量注入轮换：
 *   java -DMAP_ANDROID_KEY=xxx -DMAP_ANDROID_SECURITY_CODE=xxx -DMAP_IOS_KEY=xxx -DMAP_WEB_KEY=xxx -jar server.jar
 *   export MAP_ANDROID_KEY=xxx && java -jar server.jar
 * beta 期用户不会手动注入，故内置一份默认密钥首次启动自动落库；环境变量 / -D 优先级更高。
 * 全程 INSERT-if-absent：已手工在库里改过的记录不会被覆盖（仅显式注入时才轮换）。
 */
object MapKeySeeder {

    private val log = LoggerFactory.getLogger(MapKeySeeder::class.java)

    /** 安卓端 Key（绑定包名 com.xlin.travel + 发布/调试 SHA1） */
    private const val ANDROID_PLATFORM = "ANDROID"
    private const val ANDROID_DEFAULT_KEY = "2e5d261b2471c5062c00ddcf50b76dee"

    /** 安卓 SDK 安全码（与发布版 SHA1 配套的高德校验码，仅 Android 需要） */
    private const val ANDROID_DEFAULT_SECURITY_CODE = "c746340275a45acc5e95d0f9f52eef9b"

    /** iOS 端 Key（绑定 Bundle ID com.xlin.travel.Travel） */
    private const val IOS_PLATFORM = "IOS"
    private const val IOS_DEFAULT_KEY = "2a11f36fe2469c811ea3c924920c8a1a"

    /** Web 端 Key（JS API，安全密钥与 Key 成对下发） */
    private const val WEB_PLATFORM = "WEB"
    private const val WEB_DEFAULT_KEY = "316ce24a307d61da8a722b0b083dd75"

    /** 参数优先级：-D 系统属性 > 环境变量 > 空串（空串表示本次不写入该字段） */
    private fun env(key: String): String =
        System.getProperty(key)?.takeIf { it.isNotBlank() }
            ?: System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: ""

    fun seed() {
        seedPlatform(
            platform = ANDROID_PLATFORM,
            envKeyPrefix = "MAP_ANDROID",
            defaultApiKey = ANDROID_DEFAULT_KEY,
            defaultSecurityCode = ANDROID_DEFAULT_SECURITY_CODE,
        )
        seedPlatform(
            platform = IOS_PLATFORM,
            envKeyPrefix = "MAP_IOS",
            defaultApiKey = IOS_DEFAULT_KEY,
            defaultSecurityCode = null,   // iOS 无安全码概念
        )
        seedPlatform(
            platform = WEB_PLATFORM,
            envKeyPrefix = "MAP_WEB",
            defaultApiKey = WEB_DEFAULT_KEY,
            defaultSecurityCode = null,   // Web 的安全密钥走 securityCode 列可选下发，当前 Web 定位不依赖高德
        )
    }

    /**
     * 单平台落库：缺记录时用注入值（-D/环境变量，见 <前缀>_KEY / <前缀>_SECURITY_CODE）或内置默认补齐；
     * 已有记录只在显式注入时轮换，绝不覆盖手工编辑过的值。
     */
    private fun seedPlatform(platform: String, envKeyPrefix: String, defaultApiKey: String, defaultSecurityCode: String?) {
        val injectedKey = env("${envKeyPrefix}_KEY")
        val injectedCode = env("${envKeyPrefix}_SECURITY_CODE")
        // 局部变量避免与列 apiKey 同名：insert/update  lambda 里同名会解析到表列而非局部变量
        val finalKey = injectedKey.ifBlank { defaultApiKey }
        transaction {
            val row = MapKeys.selectAll().where { MapKeys.platform eq platform }.firstOrNull()
            if (row == null) {
                // 新建记录必须有 Key，否则下发无意义；注入值与默认值都为空则提示后跳过
                if (finalKey.isBlank()) {
                    log.warn("map 无 '$platform' 记录且未注入 ${envKeyPrefix}_KEY，跳过初始化")
                    return@transaction
                }
                MapKeys.insert {
                    it[MapKeys.platform] = platform
                    it[apiKey] = finalKey
                    it[securityCode] = injectedCode.ifBlank { defaultSecurityCode }
                }
                log.info("已写入地图密钥默认记录：$platform")
                return@transaction
            }
            // 已存在：仅在显式注入时轮换，其余一律保留
            val needUpdate = injectedKey.isNotBlank() || injectedCode.isNotBlank()
            if (!needUpdate) return@transaction
            MapKeys.update({ MapKeys.platform eq platform }) {
                if (injectedKey.isNotBlank()) it[apiKey] = injectedKey
                if (injectedCode.isNotBlank()) it[securityCode] = injectedCode
            }
            log.info("已轮换地图密钥：$platform")
        }
    }
}
