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

        // 3. 建表：users + 模型配置 llm_models / voice_models
        //    + 反馈工单 feedback_tickets + 管理员公告 broadcast_messages。
        //    历史统一表 model_api_keys 不再创建（旧库中已存在则原样保留、不引用、不 DROP）。
        //    SchemaUtils.create 是“缺表才建、存在即跳过”，绝不会 DROP / 重建已有表与数据。
        transaction {
            SchemaUtils.create(Users, LlmModels, VoiceModels, BroadcastMessages, FeedbackTickets)
        }

        // 4. 增量迁移：为已存在的旧表补上新列 / 外键
        //    （MySQL 无 ADD COLUMN / ADD CONSTRAINT IF NOT EXISTS，已存在时报错即被 runCatching 忽略）
        listOf(
            "ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL",
            "ALTER TABLE users ADD COLUMN avatar TEXT NULL",
            "ALTER TABLE users ADD COLUMN `type` VARCHAR(16) NULL",
            "ALTER TABLE feedback_tickets ADD COLUMN reply TEXT NULL",
            "ALTER TABLE feedback_tickets ADD COLUMN replied_by VARCHAR(32) NULL",
            "ALTER TABLE feedback_tickets ADD COLUMN replied_at DATETIME NULL",
            "ALTER TABLE feedback_tickets ADD CONSTRAINT fk_fb_user FOREIGN KEY (username) REFERENCES users(username)",
        ).forEach { ddl ->
            runCatching {
                dataSource.connection.use { conn ->
                    conn.createStatement().use { stmt -> stmt.execute(ddl) }
                }
            }
        }
        isInitialized = true
    }
}
