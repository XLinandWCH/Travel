package com.xlin.travel.services

import com.xlin.travel.db.CustomerServiceMessages
import com.xlin.travel.db.CustomerServiceSessions
import com.xlin.travel.db.Users
import com.xlin.travel.models.CustomerServiceCreateSessionResponse
import com.xlin.travel.models.CustomerServiceMessageDto
import com.xlin.travel.models.CustomerServiceSessionDto
import com.xlin.travel.models.CustomerServiceSessionListResponse
import com.xlin.travel.models.CustomerServiceSessionMessagesResponse
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * 客服会话业务：
 * - 普通用户：创建会话、发送消息、拉取会话列表和消息；
 * - 管理员（users.type='admin'）：查看全部会话、回复消息。
 *
 * 全部接口都以登录 token 鉴权；管理员接口额外校验角色。
 */
object CustomerServiceService {

    private const val MAX_CONTENT_LENGTH = 2000
    private const val MAX_SESSION_ATTEMPTS = 64

    sealed interface CustomerServiceResult {
        data class SessionList(val data: CustomerServiceSessionListResponse) : CustomerServiceResult
        data class SessionMessages(val data: CustomerServiceSessionMessagesResponse) : CustomerServiceResult
        data class Created(val data: CustomerServiceCreateSessionResponse) : CustomerServiceResult
        data class Done(val message: String) : CustomerServiceResult
        data class Failed(val status: Int, val message: String) : CustomerServiceResult
    }

    /** 生成唯一会话ID */
    private fun nextSessionId(): String {
        repeat(MAX_SESSION_ATTEMPTS) {
            val id = "#" + Random.nextInt(1000000, 10000000)
            if (!transaction { CustomerServiceSessions.selectAll().where { CustomerServiceSessions.sessionId eq id }.any() }) {
                return id
            }
        }
        throw RuntimeException("无法生成唯一会话ID")
    }

    /** 获取用户名 */
    private fun usernameOf(userId: Long): String? = transaction {
        Users.selectAll()
            .where { Users.id eq userId }
            .firstOrNull()
            ?.get(Users.username)
    }

    /** 检查是否为管理员 */
    private fun isAdmin(userId: Long): Boolean = transaction {
        Users.selectAll()
            .where { Users.id eq userId }
            .firstOrNull()
            ?.get(Users.userType) == "admin"
    }

    /** 创建会话 */
    fun createSession(token: String): CustomerServiceResult {
        val userId = UserService.verifyToken(token)
            ?: return CustomerServiceResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                val username = usernameOf(userId)
                    ?: return@transaction CustomerServiceResult.Failed(404, "用户不存在")
                val sessionId = nextSessionId()
                CustomerServiceSessions.insert {
                    it[CustomerServiceSessions.sessionId] = sessionId
                    it[CustomerServiceSessions.username] = username
                }
                CustomerServiceResult.Created(CustomerServiceCreateSessionResponse("会话已创建", sessionId))
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed(500, "创建会话失败：${e.message}")
        }
    }

    /** 用户发送消息 */
    fun sendMessage(token: String, sessionId: String, rawContent: String): CustomerServiceResult {
        val userId = UserService.verifyToken(token)
            ?: return CustomerServiceResult.Failed(401, "登录已失效，请重新登录")
        val content = rawContent.trim()
        if (content.isEmpty()) return CustomerServiceResult.Failed(400, "消息内容不能为空")
        if (content.length > MAX_CONTENT_LENGTH) return CustomerServiceResult.Failed(400, "消息内容过长，请精简")
        return try {
            transaction {
                val username = usernameOf(userId)
                    ?: return@transaction CustomerServiceResult.Failed(404, "用户不存在")
                val sessionExists = CustomerServiceSessions.selectAll()
                    .where {(CustomerServiceSessions.sessionId eq sessionId) and (CustomerServiceSessions.username eq username)}
                    .any()
                if (!sessionExists) return@transaction CustomerServiceResult.Failed(404, "会话不存在")
                CustomerServiceMessages.insert {
                    it[CustomerServiceMessages.sessionId] = sessionId
                    it[CustomerServiceMessages.sender] = username
                    it[CustomerServiceMessages.senderRole] = "user"
                    it[CustomerServiceMessages.content] = content
                }
                CustomerServiceResult.Done("消息已发送")
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed(500, "发送消息失败：${e.message}")
        }
    }

    /** 管理员回复消息 */
    fun adminReply(token: String, sessionId: String, rawContent: String): CustomerServiceResult {
        val userId = UserService.verifyToken(token)
            ?: return CustomerServiceResult.Failed(401, "登录已失效，请重新登录")
        val content = rawContent.trim()
        if (content.isEmpty()) return CustomerServiceResult.Failed(400, "回复内容不能为空")
        if (content.length > MAX_CONTENT_LENGTH) return CustomerServiceResult.Failed(400, "回复内容过长，请精简")
        return try {
            transaction {
                if (!isAdmin(userId)) return@transaction CustomerServiceResult.Failed(403, "仅管理员可回复")
                val adminUsername = usernameOf(userId)
                    ?: return@transaction CustomerServiceResult.Failed(404, "管理员不存在")
                val sessionExists = CustomerServiceSessions.selectAll()
                    .where { CustomerServiceSessions.sessionId eq sessionId }
                    .any()
                if (!sessionExists) return@transaction CustomerServiceResult.Failed(404, "会话不存在")
                CustomerServiceMessages.insert {
                    it[CustomerServiceMessages.sessionId] = sessionId
                    it[CustomerServiceMessages.sender] = adminUsername
                    it[CustomerServiceMessages.senderRole] = "admin"
                    it[CustomerServiceMessages.content] = content
                }
                CustomerServiceResult.Done("回复已发送")
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed(500, "回复消息失败：${e.message}")
        }
    }

    /** 获取会话消息列表 */
    fun getSessionMessages(token: String, sessionId: String): CustomerServiceResult {
        val userId = UserService.verifyToken(token)
            ?: return CustomerServiceResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                val username = usernameOf(userId)
                    ?: return@transaction CustomerServiceResult.Failed(404, "用户不存在")
                val isAdminUser = isAdmin(userId)
                val sessionExists = if (isAdminUser) {
                    CustomerServiceSessions.selectAll()
                        .where { CustomerServiceSessions.sessionId eq sessionId }
                        .any()
                } else {
                    CustomerServiceSessions.selectAll()
                        .where {(CustomerServiceSessions.sessionId eq sessionId) and (CustomerServiceSessions.username eq username)}
                        .any()
                }
                if (!sessionExists) return@transaction CustomerServiceResult.Failed(404, "会话不存在")
                val messages = CustomerServiceMessages.selectAll()
                    .where { CustomerServiceMessages.sessionId eq sessionId }
                    .orderBy(CustomerServiceMessages.createdAt, SortOrder.ASC)
                    .map { row ->
                        CustomerServiceMessageDto(
                            sender = row[CustomerServiceMessages.sender],
                            senderRole = row[CustomerServiceMessages.senderRole],
                            content = row[CustomerServiceMessages.content],
                            createdAt = row[CustomerServiceMessages.createdAt].toString()
                        )
                    }
                CustomerServiceResult.SessionMessages(CustomerServiceSessionMessagesResponse(sessionId, messages))
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed(500, "获取消息失败：${e.message}")
        }
    }

    /** 用户会话列表 */
    fun userSessionList(token: String): CustomerServiceResult {
        val userId = UserService.verifyToken(token)
            ?: return CustomerServiceResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                val username = usernameOf(userId)
                    ?: return@transaction CustomerServiceResult.Failed(404, "用户不存在")
                val sessions = CustomerServiceSessions.selectAll()
                    .where { CustomerServiceSessions.username eq username }
                    .orderBy(CustomerServiceSessions.createdAt, SortOrder.DESC)
                    .map { row ->
                        val sessionId = row[CustomerServiceSessions.sessionId]
                        val lastMsg = CustomerServiceMessages.selectAll()
                            .where { CustomerServiceMessages.sessionId eq sessionId }
                            .orderBy(CustomerServiceMessages.createdAt, SortOrder.DESC)
                            .firstOrNull()
                        CustomerServiceSessionDto(
                            sessionId = sessionId,
                            username = row[CustomerServiceSessions.username],
                            lastMessage = lastMsg?.get(CustomerServiceMessages.content),
                            lastMessageAt = lastMsg?.get(CustomerServiceMessages.createdAt)?.toString(),
                            createdAt = row[CustomerServiceSessions.createdAt].toString()
                        )
                    }
                CustomerServiceResult.SessionList(CustomerServiceSessionListResponse(sessions))
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed(500, "获取会话列表失败：${e.message}")
        }
    }

    /** 管理员会话列表 */
    fun adminSessionList(token: String): CustomerServiceResult {
        val userId = UserService.verifyToken(token)
            ?: return CustomerServiceResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                if (!isAdmin(userId)) return@transaction CustomerServiceResult.Failed(403, "仅管理员可查看会话列表")
                val sessions = CustomerServiceSessions.selectAll()
                    .orderBy(CustomerServiceSessions.createdAt, SortOrder.DESC)
                    .map { row ->
                        val sessionId = row[CustomerServiceSessions.sessionId]
                        val lastMsg = CustomerServiceMessages.selectAll()
                            .where { CustomerServiceMessages.sessionId eq sessionId }
                            .orderBy(CustomerServiceMessages.createdAt, SortOrder.DESC)
                            .firstOrNull()
                        CustomerServiceSessionDto(
                            sessionId = sessionId,
                            username = row[CustomerServiceSessions.username],
                            lastMessage = lastMsg?.get(CustomerServiceMessages.content),
                            lastMessageAt = lastMsg?.get(CustomerServiceMessages.createdAt)?.toString(),
                            createdAt = row[CustomerServiceSessions.createdAt].toString()
                        )
                    }
                CustomerServiceResult.SessionList(CustomerServiceSessionListResponse(sessions))
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed(500, "获取会话列表失败：${e.message}")
        }
    }
}
