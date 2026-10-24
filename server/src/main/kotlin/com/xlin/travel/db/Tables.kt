package com.xlin.travel.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * 业务表结构（Exposed DSL）。启动时由 DatabaseFactory 统一 SchemaUtils.create。
 *
 * 当前接入服务的只有 users 表（登录 / 注册）；
 * model_api_keys 与 feedback_tickets 仅先建表，对应服务后续再接入。
 */

/** 用户信息：用户名 / 手机号 / 邮箱 / 密码哈希 / 使用天数 / 注册时间 */
object Users : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 32).uniqueIndex()
    val phone = varchar("phone", 20).uniqueIndex()
    val email = varchar("email", 128).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)          // BCrypt 哈希，固定 60 位，留余量
    val usageDays = integer("usage_days").default(0)          // 累计使用天数
    val registeredAt = datetime("registered_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

/** 模型密钥：模型 id / API_KEY / URL（暂未接入服务） */
object ModelApiKeys : Table("model_api_keys") {
    val id = long("id").autoIncrement()
    val modelId = varchar("model_id", 64)
    val apiKey = varchar("api_key", 256)
    val url = varchar("url", 256)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("ux_model_id", modelId)
    }
}

/** 用户反馈：工单ID / 用户名 / 内容（暂未接入服务） */
object FeedbackTickets : Table("feedback_tickets") {
    val ticketId = varchar("ticket_id", 32)
    val username = varchar("username", 32)
    val content = text("content")
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(ticketId)
}
