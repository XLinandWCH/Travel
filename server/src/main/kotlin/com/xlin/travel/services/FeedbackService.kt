package com.xlin.travel.services

import com.xlin.travel.db.BroadcastMessages
import com.xlin.travel.db.FeedbackTickets
import com.xlin.travel.db.Users
import com.xlin.travel.models.BroadcastDto
import com.xlin.travel.models.BroadcastRequest
import com.xlin.travel.models.FeedbackDto
import com.xlin.travel.models.MessageListResponse
import com.xlin.travel.models.SubmitFeedbackResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * 反馈 / 消息业务：
 * - 普通用户：提交反馈（服务端生成唯一 #7 位工单号）、拉取「自己的反馈回复 + 管理员公告」；
 * - 管理员（users.type='admin'）：查看全部反馈、按工单号逐条回复、发布公告。
 *
 * 全部接口都以登录 token 鉴权；管理员接口额外校验角色。绝不 DROP，只读写 feedback_tickets / broadcast_messages。
 */
object FeedbackService {

    /** 反馈正文最大长度，避免超长文本撑爆数据库 */
    private const val MAX_CONTENT_LENGTH = 2000

    /** 工单号生成最大重试次数（7 位空间约 900 万，正常一次即命中） */
    private const val MAX_TICKET_ATTEMPTS = 64

    sealed interface FeedbackResult {
        data class List(val data: MessageListResponse) : FeedbackResult
        data class Submitted(val data: SubmitFeedbackResponse) : FeedbackResult
        data class Done(val message: String) : FeedbackResult
        data class Failed(val status: Int, val message: String) : FeedbackResult
    }

    /** 提交反馈：鉴权 → 生成唯一工单号 → 写入 feedback_tickets */
    fun submit(token: String, rawContent: String): FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackResult.Failed(401, "登录已失效，请重新登录")
        val content = rawContent.trim()
        if (content.isEmpty()) return FeedbackResult.Failed(400, "反馈内容不能为空")
        if (content.length > MAX_CONTENT_LENGTH) return FeedbackResult.Failed(400, "反馈内容过长，请精简")
        return try {
            transaction {
                val username = usernameOf(userId)
                    ?: return@transaction FeedbackResult.Failed(404, "用户不存在")
                val ticket = nextTicketId()
                FeedbackTickets.insert {
                    it[ticketId] = ticket
                    it[FeedbackTickets.username] = username
                    it[FeedbackTickets.content] = content
                }
                FeedbackResult.Submitted(SubmitFeedbackResponse("反馈已提交，工单号 $ticket", ticket))
            }
        } catch (e: Exception) {
            FeedbackResult.Failed(500, "提交反馈失败：${e.message}")
        }
    }

    /** 普通用户收件箱：自己的反馈 + 全部公告 */
    fun inbox(token: String): FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                val username = usernameOf(userId)
                    ?: return@transaction FeedbackResult.Failed(404, "用户不存在")
                val feedbacks = FeedbackTickets.selectAll()
                    .where { FeedbackTickets.username eq username }
                    .orderBy(FeedbackTickets.createdAt, SortOrder.DESC)
                    .map { it.toFeedbackDto() }
                FeedbackResult.List(MessageListResponse(feedbacks, allBroadcasts()))
            }
        } catch (e: Exception) {
            FeedbackResult.Failed(500, "获取消息失败：${e.message}")
        }
    }

    /** 管理员：全部反馈 + 全部公告 */
    fun adminList(token: String): FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                if (!isAdmin(userId)) return@transaction FeedbackResult.Failed(403, "仅管理员可查看反馈列表")
                val feedbacks = FeedbackTickets.selectAll()
                    .orderBy(FeedbackTickets.createdAt, SortOrder.DESC)
                    .map { it.toFeedbackDto() }
                FeedbackResult.List(MessageListResponse(feedbacks, allBroadcasts()))
            }
        } catch (e: Exception) {
            FeedbackResult.Failed(500, "获取反馈列表失败：${e.message}")
        }
    }

    /** 管理员回复指定工单 */
    fun reply(token: String, ticketId: String, rawReply: String): FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackResult.Failed(401, "登录已失效，请重新登录")
        val reply = rawReply.trim()
        if (reply.isEmpty()) return FeedbackResult.Failed(400, "回复内容不能为空")
        if (reply.length > MAX_CONTENT_LENGTH) return FeedbackResult.Failed(400, "回复内容过长，请精简")
        return try {
            transaction {
                val admin = Users.selectAll().where { Users.id eq userId }.firstOrNull()
                    ?: return@transaction FeedbackResult.Failed(404, "用户不存在")
                if (admin[Users.userType] != "admin") return@transaction FeedbackResult.Failed(403, "仅管理员可回复反馈")
                if (FeedbackTickets.selectAll().where { FeedbackTickets.ticketId eq ticketId }.firstOrNull() == null) {
                    return@transaction FeedbackResult.Failed(404, "工单不存在：$ticketId")
                }
                FeedbackTickets.update({ FeedbackTickets.ticketId eq ticketId }) {
                    // 局部参数 reply 与列同名，左侧用限定列名避免遮蔽
                    it[FeedbackTickets.reply] = reply
                    it[FeedbackTickets.repliedBy] = admin[Users.username]
                    it[FeedbackTickets.repliedAt] = LocalDateTime.now()
                }
                FeedbackResult.Done("已回复工单 $ticketId")
            }
        } catch (e: Exception) {
            FeedbackResult.Failed(500, "回复失败：${e.message}")
        }
    }

    /** 管理员发布公告 */
    fun broadcast(token: String, request: BroadcastRequest): FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackResult.Failed(401, "登录已失效，请重新登录")
        val title = request.title.trim()
        val content = request.content.trim()
        if (title.isEmpty() || content.isEmpty()) return FeedbackResult.Failed(400, "标题与正文不能为空")
        return try {
            transaction {
                if (!isAdmin(userId)) return@transaction FeedbackResult.Failed(403, "仅管理员可发布公告")
                BroadcastMessages.insert {
                    it[BroadcastMessages.title] = title
                    it[BroadcastMessages.content] = content
                    it[adminUsername] = usernameOf(userId)!!
                }
                FeedbackResult.Done("公告已发布")
            }
        } catch (e: Exception) {
            FeedbackResult.Failed(500, "发布失败：${e.message}")
        }
    }

    // ==================== 内部工具 ====================

    private fun usernameOf(userId: Long): String? =
        Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.username)

    private fun isAdmin(userId: Long): Boolean =
        Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.userType) == "admin"

    private fun allBroadcasts(): List<BroadcastDto> =
        BroadcastMessages.selectAll()
            .orderBy(BroadcastMessages.id, SortOrder.DESC)
            .map {
                BroadcastDto(
                    id = it[BroadcastMessages.id],
                    title = it[BroadcastMessages.title],
                    content = it[BroadcastMessages.content],
                    adminUsername = it[BroadcastMessages.adminUsername],
                    createdAt = it[BroadcastMessages.createdAt].toString(),
                )
            }

    /** 生成唯一工单号：# + 7 位随机数，冲突则重试 */
    private fun nextTicketId(): String {
        repeat(MAX_TICKET_ATTEMPTS) {
            val candidate = "#${Random.nextInt(1_000_000, 10_000_000)}"
            val taken = FeedbackTickets.selectAll().where { FeedbackTickets.ticketId eq candidate }.firstOrNull()
            if (taken == null) return candidate
        }
        error("工单号生成失败，请重试")
    }

    private fun ResultRow.toFeedbackDto() = FeedbackDto(
        ticketId = this[FeedbackTickets.ticketId],
        username = this[FeedbackTickets.username],
        content = this[FeedbackTickets.content],
        createdAt = this[FeedbackTickets.createdAt].toString(),
        reply = this[FeedbackTickets.reply],
        repliedAt = this[FeedbackTickets.repliedAt]?.toString(),
    )
}
