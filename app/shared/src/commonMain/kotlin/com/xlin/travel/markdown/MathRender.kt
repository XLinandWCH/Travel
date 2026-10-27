package com.xlin.travel.markdown

/**
 * 数学公式的两种渲染入口共用一层薄壳：
 * - [stripDelimiters]：识别 `$..$` / `$$..$$` / `\(..\)` / `\[..\]`，返回去掉定界符后的裸 LaTeX；
 * - [format]：行内公式无法做二维排版，解析成 AST 后压平为可读的 Unicode 近似文本。
 *
 * 二维排版（分数上下叠放、真正分数线）由 [MathTypeset] 负责，只用于独立公式块。
 */
internal object MathRender {

    /** 行内 / 独立公式定界符：命中则返回裸 LaTeX，否则 null（单独的 $$ 视为开括，交多行分支） */
    fun stripDelimiters(raw: String): String? {
        val s = raw.trim()
        return when {
            s.startsWith("$$") && s.endsWith("$$") && s.length >= 5 -> s.substring(2, s.length - 2)
            s.length >= 3 && !s.startsWith("$$") && s.startsWith("$") && s.endsWith("$") -> s.substring(1, s.length - 1)
            s.startsWith("\\(") && s.endsWith("\\)") && s.length >= 5 -> s.substring(2, s.length - 2)
            s.startsWith("\\[") && s.endsWith("\\]") && s.length >= 5 -> s.substring(2, s.length - 2)
            else -> null
        }
    }

    /** 行内公式：解析为 AST 后压平成 Unicode 近似串；解析异常时退回原样文本 */
    fun format(latex: String): String =
        runCatching { flatten(MathParser.parse(latex)) }.getOrElse { latex.trim() }

    /** 把节点压平成单行 Unicode 文本：分数用 a/b、上下标用 Unicode、根号用 √() */
    private fun flatten(nodes: List<MathNode>): String {
        val sb = StringBuilder()
        for (n in nodes) when (n) {
            is MathNode.Atom -> sb.append(n.text)
            is MathNode.Group -> sb.append(flatten(n.nodes))
            is MathNode.Sqrt -> sb.append("√(").append(flatten(n.rad)).append(")")
            is MathNode.Frac -> sb.append("(").append(flatten(n.num)).append(")/(").append(flatten(n.den)).append(")")
            is MathNode.Script -> {
                sb.append(flatten(listOf(n.base)))
                n.sup?.let { sb.append(script(flatten(it), SuperscriptMap)) }
                n.sub?.let { sb.append(script(flatten(it), SubscriptMap)) }
            }
        }
        return sb.toString().replace(Regex(" {2,}"), " ").trim()
    }

    private fun script(chunk: String, map: Map<Char, Char>): String {
        // 去掉压平产生的空格，再逐字映射为上 / 下标；无对应字符保留原样并前置空格以免粘连
        val compact = chunk.replace(" ", "")
        return compact.map { map[it] ?: it }.joinToString("").ifEmpty { chunk }
    }

    private val SuperscriptMap = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ', 'a' to 'ᵃ', 'e' to 'ᵉ',
        'k' to 'ᵏ', 'm' to 'ᵐ', 'o' to 'ᵒ', 'p' to 'ᵖ', 't' to 'ᵗ',
    )

    private val SubscriptMap = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'i' to 'ᵢ', 'n' to 'ₙ', 'x' to 'ₓ',
        'o' to 'ₒ', 'r' to 'ᵣ', 'u' to 'ᵤ', 'v' to 'ᵥ', 'k' to 'ₖ',
    )
}
