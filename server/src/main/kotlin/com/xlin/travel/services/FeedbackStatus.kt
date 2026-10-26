package com.xlin.travel.services

/**
 * 反馈工单状态与线程角色的字符串常量（数据库以 varchar 存储，前后端共用同一取值）。
 * - 状态：submitted(提交) → in_progress(处理中) → resolved(已解决)；
 * - 线程消息角色：admin(管理员回复) / user(用户追问)。
 */
object FeedbackStatus {
    const val SUBMITTED = "submitted"
    const val IN_PROGRESS = "in_progress"
    const val RESOLVED = "resolved"

    const val ROLE_ADMIN = "admin"
    const val ROLE_USER = "user"

    /** 合法状态集合，管理员改状态时校验 */
    val ALL = setOf(SUBMITTED, IN_PROGRESS, RESOLVED)
}
