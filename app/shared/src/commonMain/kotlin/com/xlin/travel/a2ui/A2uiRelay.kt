package com.xlin.travel.a2ui

/**
 * 工具 → 界面的单程票据。
 *
 * 为什么需要它：[com.xlin.travel.agent.tool.A2uiRenderTool] 的返回值是要进模型上下文的文本，
 * 把整份 JSON 塞回去既浪费 token，又容易让模型把 JSON 抄进正文。所以工具执行完只把
 * 解析好的 [A2uiSurface] 存在这里，由聊天层在「本轮工具调用结束」时取走并挂到消息上。
 *
 * 线程安全：一次对话里工具执行与事件回调在同一个协程内串行发生，且每轮开始时会 [clear]，
 * 因此单槽位足够；不做并发保护是为了不引入平台相关的锁实现。
 */
object A2uiRelay {

    private var staged: A2uiSurface? = null

    /** 工具执行成功时投递，后到的覆盖先到的（同一轮多次调用以最后一次为准） */
    fun publish(surface: A2uiSurface) {
        staged = surface
    }

    /** 取走并清空；没有可用的界面时返回 null */
    fun take(): A2uiSurface? {
        val current = staged
        staged = null
        return current
    }

    /** 每轮开始前清空，避免上一轮（或上一条会话）的界面串到这一条消息上 */
    fun clear() {
        staged = null
    }
}
