package com.xlin.travel.services

import com.xlin.travel.db.CustomerChat
import com.xlin.travel.db.Users
import com.xlin.travel.models.CustomerConversationDto
import com.xlin.travel.models.CustomerConversationsResponse
import com.xlin.travel.models.CustomerMessageDto
import com.xlin.travel.models.CustomerMessagesResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 客服聊天业务（单表 [CustomerChat]，会话以客户用户名为归属键）：
 * - 普通用户：拉取 / 发送自己的消息；
 * - 管理员（users.type='admin'）：查看全部会话、按 username 拉取并回复客户消息。
 *
 * 全部接口以登录 token 鉴权；管理员接口额外校验角色。
 */
object CustomerServiceService {

    /** 全局通知的会话归属键：管理员向它发送的内容对所有用户可见（一视同仁） */
    const val GLOBAL_USERNAME = "__global__"

    private const val MAX_CONTENT_LENGTH = 2000
    private const val MAX_IMAGE_LENGTH = 4 * 1024 * 1024   // 图片 base64 上限约 4MB

    sealed interface CustomerServiceResult {
        data class Messages(val data: CustomerMessagesResponse) : CustomerServiceResult
        data class Conversations(val data: CustomerConversationsResponse) : CustomerServiceResult
        data class Done(val message: String) : CustomerServiceResult
        data class Failed(val status: Int, val message: String) : CustomerServiceResult
    }

    private fun usernameOf(userId: Long): String? = transaction {
        Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.username)
    }

    private fun isAdmin(userId: Long): Boolean = transaction {
        Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.userType) == "admin"
    }

    private fun ResultRow.toDto() = CustomerMessageDto(
        senderRole = this[CustomerChat.senderRole],
        content = this[CustomerChat.content],
        imageData = this[CustomerChat.imageData],
        createdAt = this[CustomerChat.createdAt].toString(),
    )

    /** 内容与图片校验，通过返回 null，否则返回失败结果 */
    private fun validate(content: String, imageData: String?): CustomerServiceResult.Failed? {
        if (content.isEmpty() && imageData.isNullOrEmpty()) return CustomerServiceResult.Failed(400, "消息内容不能为空")
        if (content.length > MAX_CONTENT_LENGTH) return CustomerServiceResult.Failed(400, "消息内容过长，请精简")
        if (!imageData.isNullOrEmpty() && imageData.length > MAX_IMAGE_LENGTH) {
            return CustomerServiceResult.Failed(400, "图片过大，请选择较小的图片")
        }
        return null
    }

    /** 拉取消息：普通用户恒为自己；管理员需指定 username */
    fun messages(token: String, username: String?): CustomerServiceResult {
        val userId = UserService.verifyToken(token)
            ?: return CustomerServiceResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                val me = usernameOf(userId) ?: return@transaction CustomerServiceResult.Failed(404, "用户不存在")
                val admin = isAdmin(userId)
                val rows = if (admin) {
                    val target = username?.takeIf { it.isNotBlank() }
                        ?: return@transaction CustomerServiceResult.Failed(400, "请指定要查看的客户")
                    CustomerChat.selectAll().where { CustomerChat.username eq target }
                } else {
                    // 普通用户：自己的消息 + 全局通知，一视同仁
                    CustomerChat.selectAll().where {
                        (CustomerChat.username eq me) or (CustomerChat.username eq GLOBAL_USERNAME)
                    }
                }
                val messages = rows
                    .orderBy(CustomerChat.createdAt to SortOrder.ASC, CustomerChat.id to SortOrder.ASC)
                    .map { it.toDto() }
                CustomerServiceResult.Messages(CustomerMessagesResponse(messages))
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed(500, "获取消息失败：${e.message}")
        }
    }

    /** 用户发送消息（含可选图片） */
    fun send(token: String, rawContent: String, imageData: String?): CustomerServiceResult {
        val userId = UserService.verifyToken(token)
            ?: return CustomerServiceResult.Failed(401, "登录已失效，请重新登录")
        val content = rawContent.trim()
        validate(content, imageData)?.let { return it }
        return try {
            transaction {
                val me = usernameOf(userId) ?: return@transaction CustomerServiceResult.Failed(404, "用户不存在")
                CustomerChat.insert {
                    it[CustomerChat.username] = me
                    it[CustomerChat.senderRole] = "user"
                    it[CustomerChat.content] = content
                    it[CustomerChat.imageData] = imageData
                }
                CustomerServiceResult.Done("消息已发送")
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed(500, "发送消息失败：${e.message}")
        }
    }

    /** 管理员回复指定客户（含可选图片） */
    fun adminReply(token: String, targetUsername: String, rawContent: String, imageData: String?): CustomerServiceResult {
        val userId = UserService.verifyToken(token)
            ?: return CustomerServiceResult.Failed(401, "登录已失效，请重新登录")
        val content = rawContent.trim()
        validate(content, imageData)?.let { return it }
        val target = targetUsername.trim()
        if (target.isEmpty()) return CustomerServiceResult.Failed(400, "请指定要回复的客户")
        return try {
            transaction {
                if (!isAdmin(userId)) return@transaction CustomerServiceResult.Failed(403, "仅管理员可回复")
                if (target != GLOBAL_USERNAME) {
                    val exists = Users.selectAll().where { Users.username eq target }.any()
                    if (!exists) return@transaction CustomerServiceResult.Failed(404, "客户不存在")
                }
                CustomerChat.insert {
                    it[CustomerChat.username] = target
                    it[CustomerChat.senderRole] = "admin"
                    it[CustomerChat.content] = content
                    it[CustomerChat.imageData] = imageData
                }
                CustomerServiceResult.Done("回复已发送")
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed(500, "回复消息失败：${e.message}")
        }
    }

    /** 管理员会话列表：每个客户取最新一条消息作摘要，按最近活跃排序 */
    fun conversations(token: String): CustomerServiceResult {
        val userId = UserService.verifyToken(token)
            ?: return CustomerServiceResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                if (!isAdmin(userId)) return@transaction CustomerServiceResult.Failed(403, "仅管理员可查看会话列表")
                val rows = CustomerChat.selectAll()
                    .orderBy(CustomerChat.createdAt to SortOrder.DESC, CustomerChat.id to SortOrder.DESC)
                    .toList()
                var globalLast: CustomerConversationDto? = null
                val seen = LinkedHashMap<String, CustomerConversationDto>()
                for (row in rows) {
                    val u = row[CustomerChat.username]
                    val text = row[CustomerChat.content]
                    val summary = when {
                        text.isNotBlank() -> text
                        !row[CustomerChat.imageData].isNullOrEmpty() -> "图片消息"
                        else -> ""
                    }
                    val dto = CustomerConversationDto(u, summary, row[CustomerChat.createdAt].toString())
                    if (u == GLOBAL_USERNAME) {
                        if (globalLast == null) globalLast = dto
                    } else if (!seen.containsKey(u)) {
                        seen[u] = dto
                    }
                }
                // 全局通知置顶（即使还没有内容也展示，供管理员发布第一条通知）
                val global = globalLast ?: CustomerConversationDto(GLOBAL_USERNAME, null, null)
                CustomerServiceResult.Conversations(
                    CustomerConversationsResponse(listOf(global) + seen.values.toList())
                )
            }
        } catch (e: Exception) {
            CustomerServiceResult.Failed(500, "获取会话列表失败：${e.message}")
        }
    }
}