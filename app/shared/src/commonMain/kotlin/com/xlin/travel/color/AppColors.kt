package com.xlin.travel.color

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 全局颜色表（深色 / 浅色双主题）。
 *
 * 每个颜色都是 getter：组合函数中读取时会根据 [AppTheme.isDark] 返回对应主题的值，
 * 主题切换后所有用到的地方自动重组。名称沿用深色初稿的语义（如 TextWhite 在浅色
 * 主题下返回黑色），常量名保持不变，引用处无需改动。
 *
 * 注释中的来源标签对应 UI设计/ 目录下的初稿：
 * - [抽屉] 带导航抽屉的主聊天界面.html
 * - [输入框] 输入框设计初稿.html
 * - [设置] 设置界面UI初稿.html
 * - [消息] 聊天消息既有配色
 * 浅色主题没有独立初稿，按“白字变黑字、深底变浅底、白按钮变黑灰按钮”的规则取反。
 */

// ==================== 背景与容器 ====================
val ChatBackground: Color
    get() = themeColor(Color(0xFF1A1A1A), Color(0xFFF5F5F5))    // 全局页面背景（聊天 / 设置 / 搜索等子页面共用）
val SidebarBackground: Color
    get() = themeColor(Color(0xFF111111), Color(0xFFEAEAEA))    // 抽屉侧边栏背景 [抽屉]
val InputCardBackground: Color
    get() = themeColor(Color(0xFF2D2D2D), Color(0xFFFFFFFF))    // 输入框卡片主体 [输入框]
val UserBubbleBackground: Color
    get() = themeColor(Color(0xFF2D2D2D), Color(0xFFE4E4E4))    // 用户消息气泡
val SettingCardBackground: Color
    get() = themeColor(Color(0xFF1E1E1E), Color(0xFFFFFFFF))    // 设置分组卡片 --card [设置]
val IconButtonBackground: Color
    get() = themeColor(Color(0xFF1E1E1E), Color(0xFFE4E4E4))    // 圆形 / 方形图标按钮背景 [抽屉]
val SearchModalBackground: Color
    get() = themeColor(Color(0xFF1E1E1E), Color(0xFFFFFFFF))    // 搜索弹窗面板 [抽屉]
val AvatarBackground: Color
    get() = themeColor(Color(0xFF333333), Color(0xFFD9D9D9))    // 用户头像圆底 [抽屉]
val WhiteBtnBackground: Color
    get() = themeColor(Color(0xFFF5F5F5), Color(0xFF2D2D2D))    // 动作按钮：深主题白底 / 浅主题黑灰底 [输入框]
val RemoveButtonBackground: Color
    get() = themeColor(Color(0xCC1E1E1E), Color(0xCCFFFFFF))    // 图片预览删除按钮（80% 卡片色）[输入框]

// ==================== 交互状态 ====================
val NewChatButton = Color(0xFFFF4500)          // New chat 主按钮橙红，两套主题一致 [抽屉]
val NewChatButtonPressed = Color(0xFFE03E00)   // 主按钮 hover / pressed [抽屉]
val SidebarItemActive: Color
    get() = themeColor(Color(0xFF2A2A2A), Color(0xFFDCDCDC))    // 历史项 hover / active 背景 [抽屉]
val IconButtonHover: Color
    get() = themeColor(Color(0xFF2A2A2A), Color(0xFFD6D6D6))    // 图标按钮 hover [抽屉]
val SettingItemPressed: Color
    get() = themeColor(Color(0x0AFFFFFF), Color(0x0F000000))    // 设置条目按下：深色底叠白 4% / 浅色底叠黑 6% [设置]
val MenuButtonBackground = Color.Transparent   // 移动端菜单按钮底 [抽屉]

// ==================== 强调色 ====================
val AccentOrange = Color(0xFFFF5722)           // 主强调橙 --accent：光标条 / 设置图标 / 退出登录，两套主题一致

// ==================== 文本与图标 ====================
val TextWhite: Color
    get() = themeColor(Color(0xFFFFFFFF), Color(0xFF1A1A1A))    // 高亮文本 / 标题（浅色主题下为黑）[抽屉]
val TextGray300: Color
    get() = themeColor(Color(0xFFD1D5DB), Color(0xFF374151))    // 历史项默认文本 [抽屉]
val TextGray400: Color
    get() = themeColor(Color(0xFF9CA3AF), Color(0xFF6B7280))    // 图标 / 次要文本 [抽屉]
val TextGray500: Color
    get() = themeColor(Color(0xFF6B7280), Color(0xFF9CA3AF))    // 分组标题 / 占位提示 [抽屉]
val SettingValueText: Color
    get() = themeColor(Color(0xFF9E9E9E), Color(0xFF616161))    // 设置条目右侧数值 --sub [设置]
val SettingChevron: Color
    get() = themeColor(Color(0xB39E9E9E), Color(0xB3616161))    // 右向箭头：次要文本色叠加 70% 透明度 [设置]
val InputTextColor: Color
    get() = themeColor(Color(0xFFE0E0E0), Color(0xFF1A1A1A))    // 输入文字 [输入框]
val UserMessageText: Color
    get() = themeColor(Color(0xFFE0E0E0), Color(0xFF1A1A1A))    // 用户消息文字（与输入文字同值）
val InputPlaceholderColor: Color
    get() = themeColor(Color(0xFF888888), Color(0xFF9E9E9E))    // 占位文字 [输入框]
val ModeTextColor: Color
    get() = themeColor(Color(0xFFBBBBBB), Color(0xFF555555))    // 模式选择文字 / 下三角 [输入框]
val AssistantTextColor: Color
    get() = themeColor(Color(0xFFFFFFFF), Color(0xFF1A1A1A))    // AI 回答正文 [消息]
val ReasoningTextColor: Color
    get() = themeColor(Color(0xFF9E9E9E), Color(0xFF616161))    // AI 思考内容灰色（与设置数值同值）[消息]
val BtnAddIcon: Color
    get() = themeColor(Color(0xFFCCCCCC), Color(0xFF444444))    // “+”按钮图标 [输入框]
val WhiteBtnIcon: Color
    get() = themeColor(Color(0xFF1A1A1A), Color(0xFFF5F5F5))    // 动作按钮图标：与 WhiteBtnBackground 反转配对 [输入框]

// ==================== 分隔线与边框 ====================
val SidebarDivider: Color
    get() = themeColor(Color(0xFF252525), Color(0xFFD9D9D9))    // 侧边栏底部分隔线 [抽屉]
val SearchModalBorder: Color
    get() = themeColor(Color(0xFF333333), Color(0xFFC7C7C7))    // 搜索弹窗边框 [抽屉]
val BtnAddBorder: Color
    get() = themeColor(Color(0xFF555555), Color(0xFFA3A3A3))    // “+”按钮边框 [输入框]
val SettingDivider: Color
    get() = themeColor(Color(0x0DFFFFFF), Color(0x14000000))    // 卡片内分隔线：深叠白 5% / 浅叠黑 8% [设置]

// ==================== 消息动作按钮 / 提示 / 反馈面板 ====================
val ActionIconColor: Color
    get() = themeColor(Color(0xFFFFFFFF), Color(0xFF1A1A1A))    // 正文下方动作图标：深色白 / 浅色黑 [消息]
val FeedbackAccent: Color = Color(0xFF2F6BFF)                   // 反馈面板选中态蓝 [反馈]
val ToastBackground: Color
    get() = themeColor(Color(0xE62A2B32), Color(0xE6333333))    // 顶部提示胶囊底 [反馈]
val ToastText: Color = Color(0xFFFFFFFF)                        // 顶部提示文字，两套主题都是白 [反馈]
val SheetBackground: Color
    get() = themeColor(Color(0xFF141418), Color(0xFFFFFFFF))    // 底部动作面板底 [反馈]
val SheetHandle: Color
    get() = themeColor(Color(0xFF3A3A42), Color(0xFFC7C7C7))    // 面板顶部拖拽条 [反馈]
val SheetDivider: Color
    get() = themeColor(Color(0xFF202025), Color(0xFFE6E6E6))    // 面板标题栏分隔线 [反馈]
val ChipBorder: Color
    get() = themeColor(Color(0xFF2C2D34), Color(0xFFD5D5D5))    // 反馈标签默认边框 [反馈]
val ChipSelectedBackground: Color
    get() = themeColor(Color(0x1F2F6BFF), Color(0x142F6BFF))    // 反馈标签选中底：蓝色 12% / 8% [反馈]
val ChipSelectedText: Color
    get() = themeColor(Color(0xFFC9DBFF), Color(0xFF2F6BFF))    // 反馈标签选中文字 [反馈]
val SubmitDisabledBackground: Color
    get() = themeColor(Color(0xFF25262C), Color(0xFFE0E0E0))    // 提交按钮不可用底 [反馈]
val SubmitDisabledText: Color
    get() = themeColor(Color(0xFF9A9AA0), Color(0xFF8A8A90))    // 提交按钮不可用文字 [反馈]
val SubmitReadyBackground: Color
    get() = themeColor(Color(0xFFECECF0), Color(0xFF1A1A1A))    // 提交按钮可用底：与主题反转 [反馈]
val SubmitReadyText: Color
    get() = themeColor(Color(0xFF141418), Color(0xFFF5F5F5))    // 提交按钮可用文字 [反馈]

// ==================== Markdown 渲染 ====================
val CodeBlockBackground: Color
    get() = themeColor(Color(0xFF232327), Color(0xFFEDEDED))    // 代码块容器底 [markdown]
val CodeBlockText: Color
    get() = themeColor(Color(0xFFD4D4D8), Color(0xFF27272A))    // 代码块文字 [markdown]
val InlineCodeBackground: Color
    get() = themeColor(Color(0xFF2A2A2E), Color(0xFFE4E4E4))    // 行内代码底 [markdown]
val MarkdownLinkColor: Color
    get() = themeColor(Color(0xFF7BA4FF), Color(0xFF2F6BFF))    // 链接文字 [markdown]
val QuoteBarColor: Color
    get() = themeColor(Color(0xFF3A3B43), Color(0xFFC9C9CF))    // 引用左侧竖条 [markdown]

// ==================== 阴影与遮罩 ====================
val InputCardShadow: Color
    get() = themeColor(Color(0x4D000000), Color(0x26000000))    // 卡片阴影（浅色主题下减淡）[输入框]
val SearchScrim: Color
    get() = themeColor(Color(0x99000000), Color(0x66000000))    // 弹窗遮罩（浅色主题下减淡）[抽屉]

// ==================== 渐变 ====================
/** 聊天顶栏横向两端淡出衬底，跟随页面背景色 [抽屉] */
val TopBarEdgeFadeBrush: Brush
    get() {
        val base = ChatBackground
        return Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f  to base.copy(alpha = 0.65f),
                0.25f to base.copy(alpha = 0.6f),
                0.35f to base.copy(alpha = 0.2f),
                0.5f  to Color.Transparent,
                0.65f to base.copy(alpha = 0.2f),
                0.75f to base.copy(alpha = 0.6f),
                1.0f  to base.copy(alpha = 0.65f),
            )
        )
    }
