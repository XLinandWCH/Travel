package com.xlin.travel.miniprogram

/**
 * 小程序（景区助手）目录，对应 UI设计/小程序选项UI参考.html：
 * [QuickNames] 为输入区上方横滑快捷条展示的小程序，
 * [AllNames] 为点击右侧圆形菜单按钮后底部面板网格里的全部小程序（4 列排布）。
 * 目前所有小程序均为占位「开发中」界面，点哪个都进入 [MiniProgramDetailScreen]。
 */
object MiniProgramCatalog {

    /** 快捷条子集（顺序与 HTML .quick-bar 一致） */
    val QuickNames = listOf(
        "路线规划",
        "景点推荐",
        "周边查询",
        "现场避坑",
        "表演时间",
    )

    /** 面板全量列表（顺序与 HTML .panel 逐行一致） */
    val AllNames = listOf(
        "景点推荐", "周边查询", "现场避坑", "表演时间",
        "求助咨询", "门票预订", "深度游玩", "美食攻略",
        "交通导航", "景点讲解", "外卖助手", "AI精讲",
        "行程规划", "个人计划", "消费记录", "客流分析",
        "答题闯关", "旅游计划", "饮食助手",
    )
}
