package com.xlin.travel.a2ui

import org.jetbrains.compose.resources.DrawableResource
import travel.app.shared.generated.resources.Res
import travel.app.shared.generated.resources.lifen_map

/**
 * 语义图片名 → 本地图片。
 *
 * 目前只带了一张「武汉历史城区街巷与传统里份保护图」，因此地图类的语义名都落到它上面；
 * 其余名字返回 null，由渲染层退化为带图标的中性占位卡（不显示破图）。
 * 以后补了景点 / 美食实拍图，只改这张表，模型侧的契约不用动。
 */
object A2uiImages {

    fun painterFor(image: String): DrawableResource? = when (image.trim().lowercase()) {
        "", "map", "lifen", "district", "route", "nearby", "location" -> Res.drawable.lifen_map
        else -> null
    }
}
