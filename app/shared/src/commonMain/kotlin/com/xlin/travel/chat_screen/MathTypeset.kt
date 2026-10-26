package com.xlin.travel.chat_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AssistantTextColor

/**
 * 独立公式（$$..$$ / \[..\]）的二维排版：
 * - 分数用上下叠放 + 真正的横线（`Column` + `IntrinsicSize.Max` 让横线贴合最宽的一分子/分母）；
 * - 根号用 √ 加一盖上划线；
 * - 上下标在同一个 `Text` 里用 `baselineShift` 缩小并位移，避免破坏行内基线。
 *
 * 行内公式（`$..$`）无法脱离段落做二维布局，仍走 [MathRender.format] 的 Unicode 近似。
 */
@Composable
internal fun MathBlockView(latex: String, modifier: Modifier = Modifier) {
    val nodes = remember(latex) { runCatching { MathParser.parse(latex) }.getOrNull() }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (nodes.isNullOrEmpty()) {
            // 解析异常：原样显示，绝不做二次解析
            Text(
                text = latex,
                color = AssistantTextColor,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = MathFontSize,
                lineHeight = (MathFontSize.value * 1.5f).sp,
                textAlign = TextAlign.Center,
            )
        } else {
            MathRow(nodes = nodes, fs = MathFontSize, color = AssistantTextColor)
        }
    }
}

@Composable
private fun MathRow(nodes: List<MathNode>, fs: TextUnit, color: Color, modifier: Modifier = Modifier) {
    val segs = remember(nodes, fs, color) { MathParser.segments(nodes) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        segs.forEach { seg ->
            when (seg) {
                is MathSeg.TextSeg -> MathTextSeg(seg.runs, fs, color)
                is MathSeg.FracSeg -> MathFrac(seg.num, seg.den, fs, color)
                is MathSeg.SqrtSeg -> MathSqrt(seg.rad, fs, color)
            }
        }
    }
}

/** 连续文本（含上下标）合成一个 Text，逐段套用斜体 / 缩放 / 基线位移 */
@Composable
private fun MathTextSeg(runs: List<MathRun>, fs: TextUnit, color: Color) {
    val text = remember(runs, fs) {
        buildAnnotatedString {
            runs.forEach { r ->
                pushStyle(
                    SpanStyle(
                        fontSize = (fs.value * r.scale).sp,
                        baselineShift = if (r.shift == 0f) null else BaselineShift(r.shift), // 正=上标上移，负=下标下移（比例为字号）
                        fontStyle = if (r.italic) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = FontFamily.Serif,
                    )
                )
                append(r.text)
                pop()
            }
        }
    }
    Text(
        text = text,
        color = color,
        fontSize = fs,
        fontFamily = FontFamily.Serif,
        lineHeight = (fs.value * 1.5f).sp,
    )
}

/** 分数：分子 / 横线 / 分母上下叠放，横线宽度 = 两者中较宽者 */
@Composable
private fun MathFrac(num: List<MathNode>, den: List<MathNode>, fs: TextUnit, color: Color) {
    val small = (fs.value * 0.86f).sp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(IntrinsicSize.Max).padding(horizontal = 3.dp, vertical = 1.dp),
    ) {
        MathRow(nodes = num, fs = small, color = color)
        Box(Modifier.fillMaxWidth().height(1.1.dp).background(color))
        MathRow(nodes = den, fs = small, color = color)
    }
}

/** 根号：√ 号 + 被开方数上盖一条横线 */
@Composable
private fun MathSqrt(rad: List<MathNode>, fs: TextUnit, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "√", color = color, fontSize = fs, fontFamily = FontFamily.Serif)
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.width(IntrinsicSize.Max),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(color))
            MathRow(nodes = rad, fs = fs, color = color)
        }
    }
}

private val MathFontSize = 18.sp
