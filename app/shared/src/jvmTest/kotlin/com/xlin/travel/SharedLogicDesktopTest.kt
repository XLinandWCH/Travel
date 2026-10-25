package com.xlin.travel

import ai.koog.agents.core.agent.asMermaidDiagram
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicDesktopTest {

    @Test
    fun example() {
        val mermaidDiagram: String = strategy("策略图初步测试").asMermaidDiagram()

        println(mermaidDiagram)
    }
}