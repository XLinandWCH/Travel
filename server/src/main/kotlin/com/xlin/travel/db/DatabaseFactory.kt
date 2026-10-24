package com.xlin.travel.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager

/**
 * MySQL 连接与建表初始化。
 *
 * 云服务器上 MySQL 走本机 SSH 隧道，默认地址 127.0.0.1:3306。
 * 参数优先级：-D 系统属性 > 环境变量 > 默认值，即可用
 *   java -DDB_PASSWORD=*** -jar xxx.jar
 * 或 DB_PASSWORD=*** java -jar xxx.jar 传入；
 * 支持：DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD。
 * 注意：不要把真实密码写进代码默认值，否则会被打进 jar 包。
 */
object DatabaseFactory {

    /** 初始化成功后置为 true，未初始化时登录注册接口返回 503，不影响服务本身启动 */
    var isInitialized: Boolean = false
        private set

    private fun env(key: String, default: String): String =
        System.getProperty(key)?.takeIf { it.isNotBlank() }   // java -DDB_PASSWORD=xxx 优先
            ?: System.getenv(key)?.takeIf { it.isNotBlank() } // 其次读环境变量
            ?: default

    private fun jdbcUrl(database: String): String =
        "jdbc:mysql://${env("DB_HOST", "127.0.0.1")}:${env("DB_PORT", "3306")}/$database" +
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"

    fun init() {
        val dbName = env("DB_NAME", "travel")
        val user = env("DB_USER", "root")
        val password = env("DB_PASSWORD", "")

        // 1. 先连到 MySQL 服务器本身，确保目标数据库存在（utf8mb4）
        Class.forName("com.mysql.cj.jdbc.Driver")
        DriverManager.getConnection(jdbcUrl(""), user, password).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    "CREATE DATABASE IF NOT EXISTS `$dbName` " +
                        "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                )
            }
        }

        // 2. HikariCP 连接池接入 Exposed
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = jdbcUrl(dbName)
            username = user
            this.password = password
            driverClassName = "com.mysql.cj.jdbc.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
        })
        Database.connect(dataSource)

        // 3. 建表：登录服务使用的 users + 暂未接入的 model_api_keys / feedback_tickets
        transaction {
            SchemaUtils.create(Users, ModelApiKeys, FeedbackTickets)
        }
        isInitialized = true
    }
}
