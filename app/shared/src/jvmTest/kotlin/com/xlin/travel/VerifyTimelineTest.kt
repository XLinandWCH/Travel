package com.xlin.travel

import com.xlin.travel.chat_screen.ThinkingItem
import com.xlin.travel.chat_screen.decodeThinkingTimeline
import com.xlin.travel.chat_screen.decodeVerifySteps
import com.xlin.travel.chat_screen.encodeThinkingTimeline
import com.xlin.travel.chat_screen.encodeVerifySteps
import com.xlin.travel.chat_screen.stripThinkingMarkers
import com.xlin.travel.chat_screen.stripVerifySteps
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 校验步骤与思考时间线共用 reasoning 一列：验证两类标记互不串味，
 * 思考区拿到的仍是纯思考文本 + 工具卡片，校验区拿到的是自己的进度行。
 */
class VerifyTimelineTest {

    private val toolMark = "\u0001TOOL\u0001调用识图工具\u00010\u0002"

    @Test
    fun thinkingAndVerifyMarksCoexist() {
        val stored = "先看图片" + toolMark + "再看文字" + encodeVerifySteps(listOf("搜索网页A", "比对事实B"))

        val timeline = decodeThinkingTimeline(stored)
        assertEquals(3, timeline.size)
        assertEquals("先看图片", (timeline[0] as ThinkingItem.Text).value)
        assertEquals("调用识图工具", (timeline[1] as ThinkingItem.ToolCall).label)
        assertEquals("再看文字", (timeline[2] as ThinkingItem.Text).value)

        assertEquals(listOf("搜索网页A", "比对事实B"), decodeVerifySteps(stored))
        assertEquals("先看图片再看文字", stripVerifySteps(stripThinkingMarkers(stored)))
    }

    @Test
    fun plainReasoningStaysUnchanged() {
        // 没有校验时 reasoning 不应多出任何字符，老数据回读保持原样
        val plain = "只有思考，没有工具也没有校验"
        assertEquals(plain, plain + encodeVerifySteps(emptyList()))
        assertEquals(listOf(ThinkingItem.Text(plain)), decodeThinkingTimeline(plain))
        assertEquals(emptyList(), decodeVerifySteps(plain))
    }
}
