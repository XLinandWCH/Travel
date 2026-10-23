package com.xlin.travel.color

import androidx.compose.ui.graphics.Color

// 对应 UI设计/带导航抽屉的主聊天界面.html 的配色，统一维护方便复用

// 侧边栏容器
val SidebarBackground = Color(0xFF111111)     // aside 背景
val SidebarDivider = Color(0xFF252525)        // 底部分隔线 border-t
val SidebarItemActive = Color(0xFF2A2A2A)     // 历史项 hover / active 背景

// New chat 主按钮
val NewChatButton = Color(0xFFFF4500)         // 橙红主按钮背景
val NewChatButtonPressed = Color(0xFFE03E00)  // hover / pressed

// 圆形图标按钮（搜索）
val IconButtonBackground = Color(0xFF1E1E1E)  // 搜索按钮背景
val IconButtonHover = Color(0xFF2A2A2A)       // hover

// 文本
val TextWhite = Color(0xFFFFFFFF)             // 高亮文本
val TextGray300 = Color(0xFFD1D5DB)           // 历史项默认文本
val TextGray400 = Color(0xFF9CA3AF)           // 图标 / 次要文本
val TextGray500 = Color(0xFF6B7280)           // 分组标题 / 占位

// 头像
val AvatarBackground = Color(0xFF333333)      // 用户头像圆底

// 搜索弹窗
val SearchModalBackground = Color(0xFF1E1E1E) // 弹窗面板
val SearchModalBorder = Color(0xFF333333)     // 弹窗边框
val SearchScrim = Color(0x99000000)           // 遮罩 black/60

// 移动端菜单按钮
val MenuButtonBackground = Color(0xFF252525)  // 菜单按钮底
