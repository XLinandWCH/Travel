package com.xlin.travel.qrcode

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.xlin.travel.color.LifenPaper as AppLifenPaper
import com.xlin.travel.color.LifenPaperDeep as AppLifenPaperDeep

/**
 * 里份旅游线专属暖色表（扫码 → 里份路线图 → 线路页 → 详情页 / 地图查看）。
 *
 * 有意不走 AppColors 的深浅主题 getter：这一片界面固定用一套暖色，
 * 取意汉口里份的红砖墙、老门牌与泛黄纸页，黑白主题切换不影响它。
 * 全表不用纯白 / 纯黑：纸底带米黄，墨字带棕味。
 */

/** 页面底：宣纸米黄（与 AppColors 共享） */
val LifenPaper: Color get() = AppLifenPaper

/** 页面渐变下端：陈纸色（与 AppColors 共享） */
val LifenPaperDeep: Color get() = AppLifenPaperDeep

/** 卡片 / 按钮表面：暖白（非纯白） */
val LifenCard = Color(0xFFFAF5E9)

/** 标题 / 主文字：深棕墨（非纯黑） */
val LifenInk = Color(0xFF4A3325)

/** 次要文字 / 提示：灰棕 */
val LifenInkSoft = Color(0xFF8A6E55)

/** 强调色：红砖 / 印章红，用于门牌竖条、按钮、扫描框 */
val LifenBrick = Color(0xFFA8402F)

/** 强调浅底：红砖 8%，用于标签 / 选中底 */
val LifenBrickSoft = Color(0x14A8402F)

/** 分隔线 / 描边：纸纹棕 */
val LifenLine = Color(0xFFDFD1B6)

/** 图标按钮底：略深的纸色 */
val LifenChip = Color(0xFFE9DCC3)

/** 页面背景渐变：从上到下微微变深，避免大面积单色的单调感 */
val LifenPageBrush: Brush
    get() = Brush.verticalGradient(listOf(LifenPaper, LifenPaperDeep))