package com.xlin.travel.a2ui

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
 * 语义图标名 → 项目内矢量图。
 *
 * AI 只写 `icon: "food"` 这种语义名，不去猜资源路径；映射表复用 12 个小程序的图标，
 * 让「AI 生成的界面」和「小程序入口」看起来是同一套视觉语言。
 * 未识别的名字返回 null，渲染层退化为圆点，不显示破图。
 */
object A2uiIcons {

    fun painterFor(icon: String): DrawableResource? = when (icon.trim().lowercase()) {
        "route", "route_plan", "plan", "walk" -> Res.drawable.mini_route
        "spot", "sight", "scenic", "star" -> Res.drawable.mini_spot
        "nearby", "around", "location", "map" -> Res.drawable.mini_nearby
        "avoid", "pit", "warn", "warning" -> Res.drawable.mini_avoid
        "show", "time", "schedule", "night" -> Res.drawable.mini_show
        "help", "support", "service" -> Res.drawable.mini_help
        "ticket", "money", "price" -> Res.drawable.mini_ticket
        "food", "eat", "restaurant" -> Res.drawable.mini_food
        "traffic", "bus", "metro", "car" -> Res.drawable.mini_traffic
        "guide", "story", "history" -> Res.drawable.mini_guide
        "flow", "crowd", "people" -> Res.drawable.mini_flow
        "checkin", "check_in", "photo", "camera" -> Res.drawable.mini_checkin
        else -> null
    }
}
