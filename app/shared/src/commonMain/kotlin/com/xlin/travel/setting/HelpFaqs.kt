package com.xlin.travel.setting

import org.jetbrains.compose.resources.DrawableResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.email
import travel.app.shared.generated.resources.safe
import travel.app.shared.generated.resources.user

/** 帮助中心页面的常见问题条目 */
internal data class FaqItem(
    val icon: DrawableResource,
    val question: String,
    val answer: String,
    /** 非空时在答案下方显示「复制」按钮（联系邮箱这类需要外发的信息） */
    val copyValue: String? = null,
)

/**
 * 基础 FAQ 文案：内容口径与 composeResources/files 下的《服务条款》《隐私政策》保持一致，
 * 详细条款以对应的文档子页面原文为准。
 */
internal object HelpFaqs {

    /** 联系作者（问题反馈、账号申诉、内容投诉）的稳定邮箱 */
    const val AuthorEmail = "xlinandwch@outlook.com"

    const val FeedbackPlaceholder = "请描述您遇到的问题或建议，我们会在 15 个工作日内回复"

    val items: List<FaqItem> = listOf(
        FaqItem(
            icon = Res.drawable.user,
            question = "用户信息",
            answer = "我们只在您主动填写时收集账号信息（手机号、邮箱）与基本资料（年龄、性别），" +
                "用于注册登录、身份验证和改善回答相关性；AI 会话记录随会话保存在本机，删除会话即一并删除。" +
                "完整收集清单可在“隐私政策”第二节查看。",
        ),
        FaqItem(
            icon = Res.drawable.safe,
            question = "数据安全",
            answer = "AI 聊天记录默认存储在设备本地数据库，不上传云端；只有您主动使用跨设备迁移时，" +
                "数据才会在迁移必需的最短时间内临时上传，迁移完成后 7 个工作日内删除云端副本。" +
                "我们采取加密存储、权限最小化与去标识化措施，不会把您的个人信息用于个性化广告推送。",
        ),
        FaqItem(
            icon = Res.drawable.email,
            question = "联系作者",
            answer = "功能建议、账号问题、内容投诉都可以写信到作者邮箱，" +
                "我们会在 15 个工作日内响应；涉及删除个人信息的请求，请附上账号信息以便核实。",
            copyValue = AuthorEmail,
        ),
    )
}
