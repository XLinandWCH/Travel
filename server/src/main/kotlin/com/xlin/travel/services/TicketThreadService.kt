package com.xlin.travel.services

import com.xlin.travel.db.BroadcastMessages
import com.xlin.travel.db.FeedbackMessages
import com.xlin.travel.db.FeedbackTickets
import com.xlin.travel.db.Users
import com.xlin.travel.models.FeedbackDetailResponse
import com.xlin.travel.models.FeedbackMessageDto
import com.xlin.travel.models.UnreadResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

/**
 * 工单「线程」相关业务：详情（含回复线程）、用户追问、管理员改状态、用户确认解决并评分、
 * 未读徽标计数与已读标记。与 [FeedbackService] 共用 feedback_tickets / feedback_messages /
 * broadcast_messages / users 四张表，结果类型也复用 [FeedbackService.FeedbackResult]。
 */
object TicketThreadService {

    private const val MAX_CONTENT_LENGTH = 2000

    /** 工单详情：仅工单归属者或管理员可看；返回原始反馈 + 线程消息 + 状态 / 评分 */
    fun detail(token: String, ticketId: String): FeedbackService.FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackService.FeedbackResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                val me = userRow(userId) ?: return@transaction FeedbackService.FeedbackResult.Failed(404, "用户不存在")
                val ticket = FeedbackTickets.selectAll()
                    .where { FeedbackTickets.ticketId eq ticketId }.firstOrNull()
                    ?: return@transaction FeedbackService.FeedbackResult.Failed(404, "工单不存在：$ticketId")
                val mine = ticket[FeedbackTickets.username] == me[Users.username]
                if (!mine && me[Users.userType] != "admin") {
                    return@transaction FeedbackService.FeedbackResult.Failed(403, "无权查看该工单")
                }
                val messages = FeedbackMessages.selectAll()
                    .where { FeedbackMessages.ticketId eq ticketId }
                    .orderBy(FeedbackMessages.createdAt, SortOrder.ASC)
                    .map { it.toThreadDto() }
                val t = ticket
                FeedbackService.FeedbackResult.Detail(
                    FeedbackDetailResponse(
                        ticketId = t[FeedbackTickets.ticketId],
                        username = t[FeedbackTickets.username],
                        content = t[FeedbackTickets.content],
                        createdAt = t[FeedbackTickets.createdAt].toString(),
                        status = t[FeedbackTickets.status],
                        rating = t[FeedbackTickets.rating],
                        mine = mine,
                        messages = messages,
                    )
                )
            }
        } catch (e: Exception) {
            FeedbackService.FeedbackResult.Failed(500, "获取工单详情失败：${e.message}")
        }
    }

    /** 管理员设置工单状态（提交 / 处理中 / 已解决） */
    fun adminStatus(token: String, ticketId: String, status: String): FeedbackService.FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackService.FeedbackResult.Failed(401, "登录已失效，请重新登录")
        if (status !in FeedbackStatus.ALL) {
            return FeedbackService.FeedbackResult.Failed(400, "非法的工单状态")
        }
        return try {
            transaction {
                val me = userRow(userId) ?: return@transaction FeedbackService.FeedbackResult.Failed(404, "用户不存在")
                if (me[Users.userType] != "admin") return@transaction FeedbackService.FeedbackResult.Failed(403, "仅管理员可修改工单状态")
                if (FeedbackTickets.selectAll().where { FeedbackTickets.ticketId eq ticketId }.firstOrNull() == null) {
                    return@transaction FeedbackService.FeedbackResult.Failed(404, "工单不存在：$ticketId")
                }
                FeedbackTickets.update({ FeedbackTickets.ticketId eq ticketId }) { it[FeedbackTickets.status] = status }
                FeedbackService.FeedbackResult.Done("工单状态已更新")
            }
        } catch (e: Exception) {
            FeedbackService.FeedbackResult.Failed(500, "更新状态失败：${e.message}")
        }
    }

    /** 普通用户在自己的工单内追问：写入线程消息，并把工单退回处理中 */
    fun userReply(token: String, ticketId: String, raw: String): FeedbackService.FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackService.FeedbackResult.Failed(401, "登录已失效，请重新登录")
        val content = raw.trim()
        if (content.isEmpty()) return FeedbackService.FeedbackResult.Failed(400, "内容不能为空")
        if (content.length > MAX_CONTENT_LENGTH) return FeedbackService.FeedbackResult.Failed(400, "内容过长，请精简")
        return try {
            transaction {
                val me = userRow(userId) ?: return@transaction FeedbackService.FeedbackResult.Failed(404, "用户不存在")
                val ticket = FeedbackTickets.selectAll().where { FeedbackTickets.ticketId eq ticketId }.firstOrNull()
                    ?: return@transaction FeedbackService.FeedbackResult.Failed(404, "工单不存在：$ticketId")
                if (ticket[FeedbackTickets.username] != me[Users.username]) {
                    return@transaction FeedbackService.FeedbackResult.Failed(403, "只能在自己的工单内追问")
                }
                FeedbackMessages.insert {
                    it[FeedbackMessages.ticketId] = ticketId
                    it[sender] = me[Users.username]
                    it[senderRole] = FeedbackStatus.ROLE_USER
                    it[FeedbackMessages.content] = content
                }
                FeedbackTickets.update({ FeedbackTickets.ticketId eq ticketId }) {
                    it[FeedbackTickets.status] = FeedbackStatus.IN_PROGRESS
                }
                FeedbackService.FeedbackResult.Done("已发送")
            }
        } catch (e: Exception) {
            FeedbackService.FeedbackResult.Failed(500, "发送失败：${e.message}")
        }
    }

    /** 用户确认是否解决并评分：resolved=true 标已解决、false 退回处理中；rating 1..5 时记录 */
    fun resolve(token: String, ticketId: String, resolved: Boolean, rating: Int): FeedbackService.FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackService.FeedbackResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                val me = userRow(userId) ?: return@transaction FeedbackService.FeedbackResult.Failed(404, "用户不存在")
                val ticket = FeedbackTickets.selectAll().where { FeedbackTickets.ticketId eq ticketId }.firstOrNull()
                    ?: return@transaction FeedbackService.FeedbackResult.Failed(404, "工单不存在：$ticketId")
                if (ticket[FeedbackTickets.username] != me[Users.username]) {
                    return@transaction FeedbackService.FeedbackResult.Failed(403, "只能处理自己的工单")
                }
                val newRating = rating.coerceIn(0, 5)
                FeedbackTickets.update({ FeedbackTickets.ticketId eq ticketId }) {
                    it[FeedbackTickets.status] = if (resolved) FeedbackStatus.RESOLVED else FeedbackStatus.IN_PROGRESS
                    if (newRating in 1..5) it[FeedbackTickets.rating] = newRating
                }
                FeedbackService.FeedbackResult.Done(if (resolved) "已标记为解决" else "已标记为未解决")
            }
        } catch (e: Exception) {
            FeedbackService.FeedbackResult.Failed(500, "操作失败：${e.message}")
        }
    }

    /**
     * 未读徽标计数：管理员看「新工单 + 新公告」，普通用户看「自己工单的最新回复 + 新公告」，
     * 均以 users.msg_last_read 为水位线；从未查看（NULL）时按全量计。
     */
    fun unread(token: String): FeedbackService.FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackService.FeedbackResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                val me = userRow(userId) ?: return@transaction FeedbackService.FeedbackResult.Failed(404, "用户不存在")
                val since = me[Users.msgLastRead]
                val admin = me[Users.userType] == "admin"
                val name = me[Users.username]
                // 公告：since 为 NULL 时计全部，否则计 since 之后发布的
                val broadcastQuery = if (since == null) {
                    BroadcastMessages.selectAll()
                } else {
                    val watermark = since
                    BroadcastMessages.selectAll().where { BroadcastMessages.createdAt greater watermark }
                }
                // 工单：管理员计新工单，普通用户计自己工单被回复的
                val ticketQuery = when {
                    admin && since == null -> FeedbackTickets.selectAll()
                    admin -> {
                        val watermark = since!!
                        FeedbackTickets.selectAll().where { FeedbackTickets.createdAt greater watermark }
                    }
                    since == null -> FeedbackTickets.selectAll().where {
                        (FeedbackTickets.username eq name) and FeedbackTickets.repliedAt.isNotNull()
                    }
                    else -> {
                        val watermark = since
                        FeedbackTickets.selectAll().where {
                            (FeedbackTickets.username eq name) and (FeedbackTickets.repliedAt greater watermark)
                        }
                    }
                }
                FeedbackService.FeedbackResult.Count(UnreadResponse(countRows(broadcastQuery) + countRows(ticketQuery)))
            }
        } catch (e: Exception) {
            FeedbackService.FeedbackResult.Failed(500, "获取未读消息失败：${e.message}")
        }
    }

    /** 标记消息中心为已读：把水位线推到当前时间 */
    fun markRead(token: String): FeedbackService.FeedbackResult {
        val userId = UserService.verifyToken(token)
            ?: return FeedbackService.FeedbackResult.Failed(401, "登录已失效，请重新登录")
        return try {
            transaction {
                Users.update({ Users.id eq userId }) { it[Users.msgLastRead] = LocalDateTime.now() }
                FeedbackService.FeedbackResult.Done("已读")
            }
        } catch (e: Exception) {
            FeedbackService.FeedbackResult.Failed(500, "标记已读失败：${e.message}")
        }
    }

    // ==================== 内部工具 ====================

    private fun userRow(userId: Long): ResultRow? =
        Users.selectAll().where { Users.id eq userId }.firstOrNull()

    private fun ResultRow.toThreadDto() = FeedbackMessageDto(
        sender = this[FeedbackMessages.sender],
        senderRole = this[FeedbackMessages.senderRole],
        content = this[FeedbackMessages.content],
        createdAt = this[FeedbackMessages.createdAt].toString(),
    )

    /** 逐行计数（beta 数据量小，避免依赖 Query.count 扩展） */
    private fun countRows(query: Query): Int {
        var n = 0
        for (row in query) n++
        return n
    }
}
