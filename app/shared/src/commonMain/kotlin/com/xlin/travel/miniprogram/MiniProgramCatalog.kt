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
 * 小程序（景区助手）目录：以图标为主，每个小程序 = 一个 [MiniProgram]（图标 + 名称 + 对应工具）。
 *
 * [Quick] 为输入区上方横滑快捷条展示的常用小程序，
 * [All] 为点击右侧圆形菜单按钮后底部面板网格里展示的全部小程序（4 列排布）。
 *
 * 交互口径：点某个小程序不再进独立页面，而是在输入框插入「@名称」，
 * 发送时由 [MiniProgram.toolName] 指向的 Agent 工具取数作答（内容暂为写死的武汉·里份景区数据），
 * 用户在思考时间线里能看到「调用 XX 工具」的工具卡片。
 *
 * 图标资源位于 composeResources/drawable/，统一以 mini_ 前缀命名，便于一眼识别为小程序。
 * 注：天气预报（mini_weather.xml）当前是空文件、暂无图标，故未纳入目录，补上矢量图后再加回。
 */

/**
 * 一个小程序：展示名 [name]、图标 [icon] 与后端工具 [toolName] 绑定为一体。
 *
 * [toolHint] 是拼进模型提示的一句调用提示（带上用户常问的角度），让弱模型也知道该传什么参数。
 */
data class MiniProgram(
    val name: String,
    val icon: DrawableResource,
    val toolName: String,
    val toolHint: String,
) {
    /** 输入框里插入的提及文本，例如「@路线规划」 */
    val mention: String get() = "@$name"
}

object MiniProgramCatalog {

    private val RoutePlan = MiniProgram(
        name = "路线规划", icon = Res.drawable.mini_route,
        toolName = "route_plan", toolHint = "spotName 填武汉里份，travelMode 按用户说的步行/骑行/驾车",
    )
    private val SpotRecommend = MiniProgram(
        name = "景点推荐", icon = Res.drawable.mini_spot,
        toolName = "spot_recommend", toolHint = "preference 填用户的偏好：拍照/亲子/免费/夜景，没提就留空",
    )
    private val NearbySearch = MiniProgram(
        name = "周边查询", icon = Res.drawable.mini_nearby,
        toolName = "nearby_search", toolHint = "category 填地铁/停车/厕所/药店等类别，没提就留空",
    )
    private val AvoidPit = MiniProgram(
        name = "避坑指南", icon = Res.drawable.mini_avoid,
        toolName = "avoid_pit", toolHint = "scene 填门票/拍照/游船/吃饭等场景，没提就留空",
    )
    private val ShowTime = MiniProgram(
        name = "表演时间", icon = Res.drawable.mini_show,
        toolName = "show_time", toolHint = "date 填今晚/周末/具体时间，没提就留空",
    )
    private val HelpConsult = MiniProgram(
        name = "求助咨询", icon = Res.drawable.mini_help,
        toolName = "help_consult", toolHint = "issue 填走失/受伤/丢东西/投诉等，没提就留空",
    )
    private val TicketConsult = MiniProgram(
        name = "门票咨询", icon = Res.drawable.mini_ticket,
        toolName = "ticket_consult", toolHint = "spotName 填具体点位或项目，没提就留空",
    )
    private val FoodGuide = MiniProgram(
        name = "美食攻略", icon = Res.drawable.mini_food,
        toolName = "food_guide", toolHint = "preference 填早餐/夜宵/清淡等，没提就留空",
    )
    private val Traffic = MiniProgram(
        name = "交通出行", icon = Res.drawable.mini_traffic,
        toolName = "traffic_guide", toolHint = "from 填出发地或交通方式，没提就留空",
    )
    private val SpotGuide = MiniProgram(
        name = "景点讲解", icon = Res.drawable.mini_guide,
        toolName = "spot_guide", toolHint = "spotName 填要讲解的点位，没提就留空给全部讲稿",
    )
    private val FlowAnalysis = MiniProgram(
        name = "客流分析", icon = Res.drawable.mini_flow,
        toolName = "flow_analysis", toolHint = "time 填时段，没提就留空给全天曲线",
    )
    private val CheckIn = MiniProgram(
        name = "打卡分享", icon = Res.drawable.mini_checkin,
        toolName = "check_in", toolHint = "style 填建筑/人像/夜景等风格，没提就留空",
    )

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
