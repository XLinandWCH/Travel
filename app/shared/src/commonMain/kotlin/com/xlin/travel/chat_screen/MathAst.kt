package com.xlin.travel.chat_screen

/**
 * LaTeX 数学公式的结构化解析：把裸公式（去掉 $ 定界符后）解析成节点树，
 * 供「行内 Unicode 近似」与「独立公式二维排版」两条路径共用。
 *
 * 关键约定：数学模式下空格被忽略（修复 `\Delta t` 渲染成 “Δ t” 的问题），
 * 关系 / 二元运算符（= + × · ≤ …）在原子文本两侧补一个空格以保持可读间距。
 */
internal sealed class MathNode {
    data class Atom(val text: String, val italic: Boolean) : MathNode()
    data class Group(val nodes: List<MathNode>) : MathNode()
    data class Frac(val num: List<MathNode>, val den: List<MathNode>) : MathNode()
    data class Sqrt(val rad: List<MathNode>) : MathNode()
    data class Script(val base: MathNode, val sup: List<MathNode>?, val sub: List<MathNode>?) : MathNode()
}

/** 二维排版的一段：连续文本（含上下标）合成一个 Text，分数 / 根号各自独立成段 */
internal sealed class MathSeg {
    data class TextSeg(val runs: List<MathRun>) : MathSeg()
    data class FracSeg(val num: List<MathNode>, val den: List<MathNode>) : MathSeg()
    data class SqrtSeg(val rad: List<MathNode>) : MathSeg()
}

/**
 * 一个文本片段：[scale] 为相对基准字号的比例（上下标用 ~0.72），
 * [shift] 为相对基准字号的基线位移（正=上标上移，负=下标下移）。
 */
internal data class MathRun(val text: String, val italic: Boolean, val scale: Float, val shift: Float)

internal object MathParser {

    fun parse(src: String): List<MathNode> = Cursor(src).parseSeq(stopOnBrace = false)

    /** 把节点序列切成「文本段 / 分数 / 根号」混排，供渲染层逐段处理 */
    fun segments(nodes: List<MathNode>): List<MathSeg> {
        val segs = ArrayList<MathSeg>()
        var runs = ArrayList<MathRun>()
        fun flush() {
            if (runs.isNotEmpty()) { segs.add(MathSeg.TextSeg(runs.toList())); runs = ArrayList() }
        }
        for (n in nodes) {
            when (n) {
                is MathNode.Atom -> runs.add(MathRun(n.text, n.italic, 1f, 0f))
                is MathNode.Group -> runs.addAll(runsOf(n.nodes, 1f, 0f))
                is MathNode.Script -> runs.addAll(runsOfScript(n))
                is MathNode.Frac -> { flush(); segs.add(MathSeg.FracSeg(n.num, n.den)) }
                is MathNode.Sqrt -> { flush(); segs.add(MathSeg.SqrtSeg(n.rad)) }
            }
        }
        flush()
        return segs
    }

    /** 子树压平为文本片段：内部分数用 a/b、根号用 √(...) 近似（上下标里的分根本就罕见） */
    private fun runsOf(nodes: List<MathNode>, scale: Float, shift: Float): List<MathRun> {
        val out = ArrayList<MathRun>()
        for (n in nodes) when (n) {
            is MathNode.Atom -> out.add(MathRun(n.text, n.italic, scale, shift))
            is MathNode.Group -> out.addAll(runsOf(n.nodes, scale, shift))
            is MathNode.Script -> out.addAll(runsOfScript(n, scale, shift))
            is MathNode.Frac -> {
                out.add(MathRun("(", false, scale, shift))
                out.addAll(runsOf(n.num, scale, shift))
                out.add(MathRun(")/(", false, scale, shift))
                out.addAll(runsOf(n.den, scale, shift))
                out.add(MathRun(")", false, scale, shift))
            }
            is MathNode.Sqrt -> {
                out.add(MathRun("√(", false, scale, shift))
                out.addAll(runsOf(n.rad, scale, shift))
                out.add(MathRun(")", false, scale, shift))
            }
        }
        return out
    }

    private fun runsOfScript(node: MathNode.Script, scale: Float = 1f, shift: Float = 0f): List<MathRun> {
        val out = ArrayList<MathRun>()
        out.addAll(runsOf(listOf(node.base), scale, shift))
        node.sup?.let { out.addAll(runsOf(it, scale * 0.72f, shift + 0.42f * scale)) }
        node.sub?.let { out.addAll(runsOf(it, scale * 0.72f, shift - 0.22f * scale)) }
        return out
    }

    // ---- 命令 / 符号表 ----

    /** 需要左右留空的运算符（渲染时补空格，替代被忽略的 LaTeX 源空格） */
    private val SpacedOperators = setOf(
        "=", "<", ">", "+", "−", "±", "∓", "×", "÷", "·", "→", "←", "⇒", "⇐", "↔",
        "≈", "≤", "≥", "≠", "≡", "∝", "∈", "∉", "⊂", "⊃", "⊆", "⊇", "∪", "∩",
    )

    /** LaTeX 命令 → Unicode 字形（希腊字母、运算符、关系符等） */
    val Symbols: Map<String, String> = linkedMapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\epsilon" to "ε", "\\varepsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η",
        "\\theta" to "θ", "\\iota" to "ι", "\\kappa" to "κ", "\\lambda" to "λ",
        "\\mu" to "μ", "\\nu" to "ν", "\\xi" to "ξ", "\\pi" to "π",
        "\\rho" to "ρ", "\\sigma" to "σ", "\\tau" to "τ", "\\upsilon" to "υ",
        "\\phi" to "φ", "\\varphi" to "φ", "\\chi" to "χ", "\\psi" to "ψ", "\\omega" to "ω",
        "\\Gamma" to "Γ", "\\Delta" to "Δ", "\\Theta" to "Θ", "\\Lambda" to "Λ",
        "\\Xi" to "Ξ", "\\Pi" to "Π", "\\Sigma" to "Σ", "\\Phi" to "Φ",
        "\\Psi" to "Ψ", "\\Omega" to "Ω",
        "\\times" to "×", "\\div" to "÷", "\\cdot" to "·", "\\pm" to "±", "\\mp" to "∓",
        "\\leq" to "≤", "\\geq" to "≥", "\\neq" to "≠", "\\approx" to "≈",
        "\\equiv" to "≡", "\\sim" to "∼", "\\propto" to "∝", "\\ll" to "≪", "\\gg" to "≫",
        "\\infty" to "∞", "\\sum" to "∑", "\\prod" to "∏", "\\int" to "∫",
        "\\partial" to "∂", "\\nabla" to "∇", "\\degree" to "°",
        "\\in" to "∈", "\\notin" to "∉", "\\subseteq" to "⊆", "\\supseteq" to "⊇",
        "\\subset" to "⊂", "\\supset" to "⊃", "\\cup" to "∪", "\\cap" to "∩", "\\emptyset" to "∅",
        "\\forall" to "∀", "\\exists" to "∃", "\\neg" to "¬", "\\land" to "∧", "\\lor" to "∨",
        "\\rightarrow" to "→", "\\to" to "→", "\\Rightarrow" to "⇒", "\\Leftarrow" to "⇐",
        "\\leftarrow" to "←", "\\leftrightarrow" to "↔",
        "\\log" to "l o g", "\\ln" to "l n", "\\sin" to "s i n", "\\cos" to "c o s", "\\tan" to "t a n",
        "\\min" to "m i n", "\\max" to "m a x", "\\lim" to "l i m",
    )

    // ---- 递归下降解析 ----

    private class Cursor(val s: String) {
        var i = 0

        fun parseSeq(stopOnBrace: Boolean): List<MathNode> {
            val out = ArrayList<MathNode>()
            while (i < s.length) {
                val c = s[i]
                if (c == '}') { if (stopOnBrace) return out; i++; continue }
                when {
                    c == ' ' -> i++
                    c == '{' -> out.add(readGroup())
                    c == '^' || c == '_' -> { i++; attachScript(out, c, readArgNodes()) }
                    c == '\\' -> readCommand(out)
                    else -> { out.add(charAtom(c)); i++ }
                }
            }
            return out
        }

        private fun readGroup(): MathNode {
            i++ // consume '{'
            val inner = parseSeq(stopOnBrace = true)
            if (i < s.length && s[i] == '}') i++
            return if (inner.size == 1) inner[0] else MathNode.Group(inner)
        }

        /** 读取一个参数：{...} 组、单个命令、或单个字符 */
        private fun readArgNodes(): List<MathNode> {
            while (i < s.length && s[i] == ' ') i++
            if (i >= s.length) return emptyList()
            return when (s[i]) {
                '{' -> { val g = readGroup(); if (g is MathNode.Group) g.nodes else listOf(g) }
                '\\' -> { val tmp = ArrayList<MathNode>(); readCommand(tmp); tmp }
                else -> { val a = charAtom(s[i]); i++; listOf(a) }
            }
        }

        private fun readCommand(out: MutableList<MathNode>) {
            i++ // consume '\'
            if (i >= s.length) return
            val c = s[i]
            if (c.isLetter()) {
                val sb = StringBuilder()
                while (i < s.length && s[i].isLetter()) { sb.append(s[i]); i++ }
                handleCommand(sb.toString(), out)
            } else {
                i++ // 非字母命令（\{ \} \% 等）
                when (c) {
                    ',', ';', ':', '!' -> {} // 间距命令：忽略
                    else -> out.add(atomLiteral(c.toString()))
                }
            }
        }

        private fun handleCommand(name: String, out: MutableList<MathNode>) {
            when (name) {
                "frac", "dfrac", "tfrac" -> {
                    val n = readArgNodes(); val d = readArgNodes()
                    out.add(MathNode.Frac(n, d))
                }
                "sqrt" -> out.add(MathNode.Sqrt(readArgNodes()))
                "text", "mathrm", "operatorname", "mathbf", "bm", "boldsymbol", "mathit" -> {
                    val raw = readRawGroup()
                    out.add(MathNode.Atom(raw, name == "mathit"))
                }
                "left", "right", "big", "Big", "bigg", "Bigg", "displaystyle", "limits", "nolimits" -> {
                    // 定界符 / 样式命令：本身不产出，后续符号按普通字符解析
                }
                else -> {
                    val glyph = Symbols["\\" + name]
                    if (glyph != null) out.add(atomLiteral(glyph)) else out.add(MathNode.Atom(name, false))
                }
            }
        }

        /** \text{...} 等：原样保留内部文字（含空格），不做数学模式处理 */
        private fun readRawGroup(): String {
            while (i < s.length && s[i] == ' ') i++
            if (i >= s.length) return ""
            if (s[i] != '{') { val ch = s[i]; i++; return ch.toString() }
            i++; var depth = 1
            val sb = StringBuilder()
            while (i < s.length) {
                val ch = s[i]
                when {
                    ch == '{' -> { depth++; sb.append(ch) }
                    ch == '}' -> { depth--; if (depth == 0) { i++; return sb.toString() }; sb.append(ch) }
                    else -> sb.append(ch)
                }
                i++
            }
            return sb.toString()
        }

        private fun attachScript(out: MutableList<MathNode>, kind: Char, arg: List<MathNode>) {
            val base = if (out.isNotEmpty()) out.removeAt(out.size - 1) else MathNode.Atom("", false)
            if (base is MathNode.Script) {
                out.add(if (kind == '^') base.copy(sup = arg) else base.copy(sub = arg))
            } else {
                out.add(
                    if (kind == '^') MathNode.Script(base, sup = arg, sub = null)
                    else MathNode.Script(base, sup = null, sub = arg)
                )
            }
        }

        private fun charAtom(c: Char): MathNode {
            val italic = c in 'a'..'z' || c in 'A'..'Z'
            val str = c.toString()
            return if (str in SpacedOperators) MathNode.Atom(" $str ", false) else MathNode.Atom(str, italic)
        }

        private fun atomLiteral(glyph: String): MathNode =
            if (glyph in SpacedOperators) MathNode.Atom(" $glyph ", false) else MathNode.Atom(glyph, false)
    }
}
