package com.xlin.travel.color

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 全局颜色表（当前唯一主题为深色）。
 *
 * 按“用途角色”分类维护，替代原先按界面来源拆分的多个文件，避免重复与散乱。
 * 注释中的来源标签对应 UI设计/ 目录下的初稿：
 * - [抽屉] 带导航抽屉的主聊天界面.html
 * - [输入框] 输入框设计初稿.html
 * - [设置] 设置界面UI初稿.html
 * - [消息] 聊天消息既有配色
 * 注意：设置页的“主题颜色”选项目前仅记录偏好，浅色主题暂未全局实现。
 */

// ==================== 背景与容器 ====================
val ChatBackground = Color(0xFF1A1A1A)         // 全局页面背景（聊天 / 设置 / 搜索等子页面共用）
val SidebarBackground = Color(0xFF111111)      // 抽屉侧边栏背景 [抽屉]
val InputCardBackground = Color(0xFF2D2D2D)    // 输入框卡片主体 [输入框]
val UserBubbleBackground = Color(0xFF2D2D2D)   // 用户消息气泡（与输入框卡片同值）
val SettingCardBackground = Color(0xFF1E1E1E)  // 设置分组卡片 --card [设置]
val IconButtonBackground = Color(0xFF1E1E1E)   // 圆形 / 方形图标按钮背景 [抽屉]
val SearchModalBackground = Color(0xFF1E1E1E)  // 搜索弹窗面板 [抽屉]
val AvatarBackground = Color(0xFF333333)       // 用户头像圆底 [抽屉]
val WhiteBtnBackground = Color(0xFFF5F5F5)     // 白底动作按钮 [输入框]
val RemoveButtonBackground = Color(0xCC1E1E1E) // 图片预览删除按钮 rgba(30,30,30,0.8) [输入框]

// ==================== 交互状态 ====================
val NewChatButton = Color(0xFFFF4500)          // New chat 主按钮橙红 [抽屉]
val NewChatButtonPressed = Color(0xFFE03E00)   // 主按钮 hover / pressed [抽屉]
val SidebarItemActive = Color(0xFF2A2A2A)      // 历史项 hover / active 背景 [抽屉]
val IconButtonHover = Color(0xFF2A2A2A)        // 图标按钮 hover [抽屉]
val SettingItemPressed = Color(0x0AFFFFFF)     // 设置条目按下：白 4% [设置 .item:active]
val MenuButtonBackground = Color.Transparent   // 移动端菜单按钮底 [抽屉]

// ==================== 强调色 ====================
val AccentOrange = Color(0xFFFF5722)           // 主强调橙 --accent：光标条 / 设置图标 / 退出登录 [输入框][设置]

// ==================== 文本与图标 ====================
val TextWhite = Color(0xFFFFFFFF)              // 高亮文本 / 标题 [抽屉]
val TextGray300 = Color(0xFFD1D5DB)            // 历史项默认文本 [抽屉]
val TextGray400 = Color(0xFF9CA3AF)            // 图标 / 次要文本 [抽屉]
val TextGray500 = Color(0xFF6B7280)            // 分组标题 / 占位提示 [抽屉]
val SettingValueText = Color(0xFF9E9E9E)       // 设置条目右侧数值 --sub [设置]
val SettingChevron = Color(0xB39E9E9E)         // 右向箭头：--sub 叠加 70% 透明度 [设置 .arrow]
val InputTextColor = Color(0xFFE0E0E0)         // 输入文字 [输入框]
val UserMessageText = Color(0xFFE0E0E0)        // 用户消息文字（与输入文字同值）
val InputPlaceholderColor = Color(0xFF888888)  // 占位文字 [输入框]
val ModeTextColor = Color(0xFFBBBBBB)          // 模式选择文字 / 下三角 [输入框]
val AssistantTextColor = Color(0xFFFFFFFF)     // AI 回答正文 [消息]
val ReasoningTextColor = Color(0xFF9E9E9E)     // AI 思考内容灰色（与设置数值同值）[消息]
val BtnAddIcon = Color(0xFFCCCCCC)             // “+”按钮图标 [输入框]
val WhiteBtnIcon = Color(0xFF1A1A1A)           // 白底按钮内图标 [输入框]

// ==================== 分隔线与边框 ====================
val SidebarDivider = Color(0xFF252525)         // 侧边栏底部分隔线 [抽屉]
val SearchModalBorder = Color(0xFF333333)      // 搜索弹窗边框 [抽屉]
val BtnAddBorder = Color(0xFF555555)           // “+”按钮边框 [输入框]
val SettingDivider = Color(0x0DFFFFFF)         // 卡片内分隔线：白 5% [设置 --divider]

// ==================== 阴影与遮罩 ====================
val InputCardShadow = Color(0x4D000000)        // 卡片阴影 rgba(0,0,0,0.3) [输入框]
val SearchScrim = Color(0x99000000)            // 弹窗遮罩 black/60 [抽屉]

// ==================== 渐变 ====================
/** 聊天顶栏横向两端淡出衬底 [抽屉] */
val TopBarEdgeFadeBrush: Brush
    get() = Brush.horizontalGradient(
        colorStops = arrayOf(
            0.0f  to Color(0xFF1A1A1A).copy(alpha = 0.65f),
            0.25f to Color(0xFF1A1A1A).copy(alpha = 0.65f),
            0.35f to Color(0xFF1A1A1A).copy(alpha = 0.3f),
            0.5f  to Color.Transparent,
            0.65f to Color(0xFF1A1A1A).copy(alpha = 0.3f),
            0.75f to Color(0xFF1A1A1A).copy(alpha = 0.65f),
            1.0f  to Color(0xFF1A1A1A).copy(alpha = 0.65f),
        )
    )
