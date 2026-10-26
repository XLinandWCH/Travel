package com.xlin.travel.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * 业务表结构（Exposed DSL）。启动时由 DatabaseFactory 统一 SchemaUtils.create（缺表才建、绝不 DROP）。
 *
 * 接入服务：users（登录/注册，含管理员 role/type）、llm_models + voice_models（模型配置下发）、
 * feedback_tickets（用户反馈 + 管理员回复）、broadcast_messages（管理员公告）。
 */

/** 用户信息：用户名 / 手机号 / 邮箱 / 密码哈希 / 使用天数 / 注册时间 / 角色 type */
object Users : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 32).uniqueIndex()
    val phone = varchar("phone", 20).uniqueIndex()
    val email = varchar("email", 128).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)          // BCrypt 哈希，固定 60 位，留余量
    val passwordChangedAt = datetime("password_changed_at").nullable()  // 上次改密时间，NULL 表示从未改过（注册不算改密）
    val avatar = text("avatar").nullable()                    // 头像 base64（PNG，≤512px），NULL 表示未上传，用于跨设备同步；实际列由 DatabaseFactory 迁移升级为 MEDIUMTEXT（TEXT 仅 64KB 存不下）
    val usageDays = integer("usage_days").default(0)          // 累计使用天数
    val userType = varchar("type", 16).nullable()             // 角色："admin" 管理员；其余留空(null)。由管理员在数据库手动设置
    val msgLastRead = datetime("msg_last_read").nullable()    // 消息中心上次查看时间，用于圆点徽标（未读）判定；NULL 表示从未查看
    val registeredAt = datetime("registered_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

/**
 * 模型配置（历史统一表）：模型 id / API_KEY / URL / 系统提示词 / 温度。
 *
 * 现已按用途拆分为 [LlmModels] 与 [VoiceModels] 两张表，本表保留但不再作为下发来源，
 * 仅用于启动时把既有数据迁移到新表（绝不 DROP / 删除，避免误伤历史数据）。
 */
object ModelApiKeys : Table("model_api_keys") {
    val id = long("id").autoIncrement()
    val modelId = varchar("model_id", 64)
    val apiKey = varchar("api_key", 256)
    val url = varchar("url", 256)
    val systemPrompt = text("system_prompt").nullable()      // 系统提示词，NULL 表示未配置
    val temperature = double("temperature").nullable()        // 采样温度，NULL 表示由客户端默认值决定

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("ux_model_id", modelId)
    }
}

/**
 * 大语言（对话）模型配置：由 model_api_keys 拆分而来。
 * 客户端登录后 POST /api/model/config（scope 默认 llm）按 modelId 拉取；modelId 为空取 id 最小为默认模型。
 */
object LlmModels : Table("llm_models") {
    val id = long("id").autoIncrement()
    val modelId = varchar("model_id", 64)                    // 模型名称 / id
    val apiKey = varchar("api_key", 256)                     // 密钥
    val url = varchar("url", 256)                            // 调用端点
    val systemPrompt = text("system_prompt").nullable()      // 系统提示词，NULL 表示未配置
    val temperature = double("temperature").nullable()        // 采样温度，NULL 表示由客户端默认值决定
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }  // 时间

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("ux_llm_model_id", modelId)
    }
}

/**
 * 语音模型配置：id / 名称 / 密钥 / 端点 / 类型 / 时间。
 * [type] 为必填（非空）类型列，取值 ASR（语音识别）/ TTS（语音合成）。
 */
object VoiceModels : Table("voice_models") {
    val id = long("id").autoIncrement()
    val modelId = varchar("model_id", 64)                    // 模型名称 / id
    val apiKey = varchar("api_key", 256)                     // 密钥
    val url = varchar("url", 256)                            // 调用端点（ASR/TTS 接口地址）
    val type = varchar("model_type", 8)                      // 类型：ASR / TTS，必填非空
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }  // 时间

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("ux_voice_model_id", modelId)
    }
}

/**
 * 用户反馈工单：工单号 / 用户名(外键→users) / 内容 / 管理员回复 / 时间。
 * [ticketId] 为展示给用户的唯一编号：# + 7 位随机数（如 #8721392），服务端生成并保证不重复。
 * [username] 外键引用 users.username（该列唯一索引）。管理员回复后填充 reply / repliedBy / repliedAt。
 */
object FeedbackTickets : Table("feedback_tickets") {
    val ticketId = varchar("ticket_id", 32)                   // 工单号（#7位），主键唯一
    val username = varchar("username", 32)                    // 外键→users.username（由 DatabaseFactory 的 ALTER 添加）
    val content = text("content")                             // 用户反馈内容（工单首条消息）
    val reply = text("reply").nullable()                      // 最近一条管理员回复（列表卡片预览用），未回复为 null
    val repliedBy = varchar("replied_by", 32).nullable()      // 最近回复的管理员用户名
    val repliedAt = datetime("replied_at").nullable()         // 最近回复时间
    val status = varchar("status", 16).default("submitted")   // 工单状态：submitted(提交) / in_progress(处理中) / resolved(已解决)
    val rating = integer("rating").nullable()                 // 用户评分 1..5，NULL 表示未评价
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(ticketId)
}

/**
 * 工单回复线程（类似邮件会话）：同一工单内的往来消息，按时间正序拼接在反馈正文之后。
 * [senderRole] 为 'admin'（管理员回复）或 'user'（用户追问）。列表卡片仍用 feedback_tickets.reply 展示最近回复预览。
 */
object FeedbackMessages : Table("feedback_messages") {
    val id = long("id").autoIncrement()
    val ticketId = varchar("ticket_id", 32)                   // 外键→feedback_tickets.ticket_id
    val sender = varchar("sender", 32)                        // 发送者用户名
    val senderRole = varchar("sender_role", 8)               // 'admin' / 'user'
    val content = text("content")                             // 消息正文
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

/**
 * 管理员公告：id / 标题 / 内容 / 发布管理员(外键→users) / 时间。
 * 普通用户在消息中心可看到全部公告；仅管理员可发布。
 */
object BroadcastMessages : Table("broadcast_messages") {
    val id = long("id").autoIncrement()
    val title = varchar("title", 128)                         // 公告标题
    val content = text("content")                             // 公告正文
    val adminUsername = varchar("admin_username", 32)         // 外键→users.username（由 DatabaseFactory 的 ALTER 添加）
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}
