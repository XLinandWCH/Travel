package com.xlin.travel

import com.xlin.travel.agent.tool.kb.LifenKnowledge
import com.xlin.travel.agent.tool.kb.LifenKnowledgeCatalog
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/** 知识库检索冒烟：目录可用、章节能定位、关键词能命中段落 */
class KnowledgeSmokeTest {

    @Test
    fun catalogIsNotEmpty() {
        val toc = LifenKnowledgeCatalog.toc()
        assertTrue(toc.contains("04 咸安坊"), toc)
        assertTrue(toc.contains("12 美食推荐"), toc)
    }

    @Test
    fun emptyArgsReturnCatalog() = runBlocking {
        val text = LifenKnowledge.answer("", "")
        assertTrue(text.contains("目录"), text.take(80))
    }

    @Test
    fun chapterHintReadsSections() = runBlocking {
        val text = LifenKnowledge.answer("04", "门票 开放时间")
        println(text.take(300))
        assertTrue(text.contains("咸安坊"), text.take(120))
    }

    @Test
    fun queryRoutesToChapter() = runBlocking {
        val text = LifenKnowledge.answer("", "热干面 过早")
        println(text.take(200))
        assertTrue(text.contains("美食"), text.take(120))
    }
}
