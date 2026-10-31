package com.xlin.travel

/**
 * 顶层页面枚举与返回栈映射，从 [App] 拆出以便单文件不超过项目约定的 250 行。
 *
 * ThemeColor / ComingSoon / HelpCenter / Document / AccountControl 是设置的子页面，返回时先回设置列表；
 * FeedbackDetail / FeedbackCompose / BroadcastCompose / BroadcastDetail 是消息中心的子页面；
 * Guest 为未登入界面，Login 由未登入界面「登入」或退出登录进入；
 * MiniProgramDetail 为单个小程序的空壳页（只保留顶栏），从聊天页输入框上方的小程序快捷条进入；
 * A2ui 为 AI 编排的个性化界面（a2ui_render 的产物），从 AI 回答下方的「进入个性化界面」进入；
 * Surfaces 为个性化界面历史（顶栏 screen 图标），列出近 1~7 天生成过的所有界面；
 * VideoExplain 为视频讲解（输入框里的同名入口），模拟通话页，关闭后回聊天。
 */
internal enum class AppScreen {
    Chat,
    Guest,
    Login,
    Settings,
    Search,
    MiniProgramDetail,
    A2ui,
    Surfaces,
    VideoExplain,
    ThemeColor,
    History,
    MessageCenter,
    FeedbackDetail,
    FeedbackCompose,
    BroadcastCompose,
    BroadcastDetail,
    ComingSoon,
    HelpCenter,
    Document,
    AccountControl,
}

/**
 * 系统返回键的落点：设置子页回设置列表，消息中心子页回消息中心，
 * 其余子页（含小程序空壳页与个性化界面）回聊天页。[AppScreen.Chat] 与 [AppScreen.Guest] 是根页，
 * 由调用方决定是否交给系统退出应用；帮助中心内的面板为独立窗口，返回键由它自己消费，不走这里。
 * 个性化界面可能从历史页进入，它的返回目标不固定，由 [AppHost.surfaceBackTarget] 决定。
 */
internal fun AppScreen.systemBackTarget(): AppScreen = when (this) {
    AppScreen.ThemeColor, AppScreen.History, AppScreen.ComingSoon,
    AppScreen.HelpCenter, AppScreen.Document,
    AppScreen.AccountControl, AppScreen.MessageCenter -> AppScreen.Settings
    AppScreen.Surfaces -> AppScreen.Chat
    AppScreen.FeedbackDetail -> AppScreen.MessageCenter
    AppScreen.FeedbackCompose, AppScreen.BroadcastCompose,
    AppScreen.BroadcastDetail -> AppScreen.MessageCenter
    AppScreen.Login -> AppScreen.Guest
    else -> AppScreen.Chat
}
