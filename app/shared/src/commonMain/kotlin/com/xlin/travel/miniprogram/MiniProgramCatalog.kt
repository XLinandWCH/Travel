package com.xlin.travel.miniprogram

import org.jetbrains.compose.resources.DrawableResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.mini_avoid
import travel.app.shared.generated.resources.mini_checkin
import travel.app.shared.generated.resources.mini_flow
import travel.app.shared.generated.resources.mini_food
import travel.app.shared.generated.resources.mini_guide
import travel.app.shared.generated.resources.mini_help
import travel.app.shared.generated.resources.mini_nearby
import travel.app.shared.generated.resources.mini_route
import travel.app.shared.generated.resources.mini_show
import travel.app.shared.generated.resources.mini_spot
import travel.app.shared.generated.resources.mini_ticket
import travel.app.shared.generated.resources.mini_traffic

/**
 * 小程序（景区助手）目录：以图标为主，每个小程序 = 一个 [MiniProgram]（图标 + 名称，二者一体）。
 *
 * [Quick] 为输入区上方横滑快捷条展示的常用小程序，
 * [All] 为点击右侧圆形菜单按钮后底部面板网格里展示的全部小程序（4 列排布）。
 * 目前所有小程序均为占位「开发中」界面，点哪个都进入 [MiniProgramDetailScreen]。
 *
 * 图标资源位于 composeResources/drawable/，统一以 mini_ 前缀命名，便于一眼识别为小程序。
 * 注：天气预报（mini_weather.xml）当前是空文件、暂无图标，故未纳入目录，补上矢量图后再加回。
 */

/** 一个小程序：展示名 [name] 与其图标 [icon] 绑定为一体 */
data class MiniProgram(
    val name: String,
    val icon: DrawableResource,
)

object MiniProgramCatalog {

    private val RoutePlan = MiniProgram("路线规划", Res.drawable.mini_route)
    private val SpotRecommend = MiniProgram("景点推荐", Res.drawable.mini_spot)
    private val NearbySearch = MiniProgram("周边查询", Res.drawable.mini_nearby)
    private val AvoidPit = MiniProgram("避坑指南", Res.drawable.mini_avoid)
    private val ShowTime = MiniProgram("表演时间", Res.drawable.mini_show)
    private val HelpConsult = MiniProgram("求助咨询", Res.drawable.mini_help)
    private val TicketConsult = MiniProgram("门票咨询", Res.drawable.mini_ticket)
    private val FoodGuide = MiniProgram("美食攻略", Res.drawable.mini_food)
    private val Traffic = MiniProgram("交通出行", Res.drawable.mini_traffic)
    private val SpotGuide = MiniProgram("景点讲解", Res.drawable.mini_guide)
    private val FlowAnalysis = MiniProgram("客流分析", Res.drawable.mini_flow)
    private val CheckIn = MiniProgram("打卡分享", Res.drawable.mini_checkin)

    /** 快捷条子集（顺序与 HTML .quick-bar 一致） */
    val Quick = listOf(
        RoutePlan, SpotRecommend, NearbySearch, AvoidPit, ShowTime,
    )

    /** 面板全量列表（每行 4 个，与 HTML .panel 排布一致） */
    val All = listOf(
        SpotRecommend, NearbySearch, AvoidPit, ShowTime,
        HelpConsult, TicketConsult, SpotGuide, FoodGuide,
        Traffic, RoutePlan, FlowAnalysis, CheckIn,
    )
}
