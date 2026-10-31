package com.xlin.travel.agent.tool.lifen

/**
 * 景点讲解写死数据：服务「@景点讲解」。
 *
 * 每个点位给一段可直接念给用户听的讲稿 + 一条冷知识，
 * 模型只做串联与语气调整，不自行编造年代与人物。
 */
data class LifenScript(
    val spot: String,
    val story: String,
    val funFact: String,
) {
    fun matches(keyword: String): Boolean {
        val key = keyword.replace(" ", "")
        if (key.isEmpty()) return true
        return listOf(spot, story, funFact).any { it.replace(" ", "").contains(key) }
    }
}

object LifenNarration {

    private val all = listOf(
        LifenScript(
            spot = "唐华里·里份博物馆",
            story = "你现在站的这片巷子，就是武汉人说的「里份」。北方叫胡同，南方叫弄堂，" +
                "武汉把天井、过街楼和石库门都揉在一起，做成了一排排住人的院落。" +
                "馆里讲的是百年汉口住区怎么长出来的：先有码头，再有商行，最后才是这些住人的巷子。",
            funFact = "「里份」这个词来自旧时房地契上的“里分”，后来念着念着就成了里份。",
        ),
        LifenScript(
            spot = "咸安坊",
            story = "咸安坊是汉口保存得比较完整的一片石库门建筑群。红砖清水墙、拱形门洞、过街楼，" +
                "当年住的多是银行与洋行的职员。现在改成街区，但骨架还是原来的骨架，" +
                "抬头看山墙和门楣，能看出上世纪二三十年代的手艺。",
            funFact = "石库门并不是上海独有，汉口开埠后沿着租界一带也成片建了起来。",
        ),
        LifenScript(
            spot = "江汉关大楼",
            story = "江汉关是 1924 年落成的海关大楼，钟楼是汉口的报时中心。那时候江面上船来船往，" +
                "靠的就是这口钟。楼下是海关，楼上是办公，江边一步就是码头，" +
                "整座城市的时间与货物都从这里进出。",
            funFact = "钟声一响，老武汉人过去是对表用的：听到钟声就知道该收摊还是该上班。",
        ),
        LifenScript(
            spot = "沿江大道江滩",
            story = "这里是汉口江滩，长江在左手边，租界老楼在右手边。江滩以前是防洪堤，" +
                "后来一点点变成市民的客厅：早上有人打拳，傍晚有人放风筝，" +
                "秋天芦苇一开，整段堤岸都是白的。",
            funFact = "江滩的标高是按防洪要求做的，台阶的高低不只是好看，也记着历次水位的教训。",
        ),
        LifenScript(
            spot = "中华路码头 · 夜游长江",
            story = "从武汉关到中华路，轮渡几分钟就能过江。过去这是两岸上班族的通勤线，" +
                "现在成了最便宜的江景位。夜里上船，左边是灯光秀，右边是长江大桥，" +
                "一座城的轮廓就在水面上铺开。",
            funFact = "武汉长江大桥是 1957 年通车的公铁两用桥，被称作“万里长江第一桥”。",
        ),
        LifenScript(
            spot = "黎黄陂路街头博物馆",
            story = "黎黄陂路的名字来自民国大总统黎元洪，他是黄陂人。这条街两侧多是当年的公寓与洋行，" +
                "现在把整条街当作露天展馆，叫“街头博物馆”。梧桐树、老墙、咖啡馆，" +
                "是既旧又新的汉口日常。",
            funFact = "街名里的“黄陂”是武汉的一个区，用籍贯命名街道是民国时期的习惯。",
        ),
        LifenScript(
            spot = "巴公房子",
            story = "巴公房子是 1910 年代的俄式公寓，砖红色的三角形平面像一艘停在街口的船。" +
                "当年是俄国茶商的产业，楼下商铺楼上住人，汉口茶叶贸易的繁华都在这栋楼里留下过影子。",
            funFact = "因为地形是锐角三角，两个立面的窗户数量并不对称，拍照时可以找找这个差别。",
        ),
    )

    /** 景点讲解正文：按点位筛选，命不中退回全部讲稿 */
    fun answer(spotName: String): String {
        val key = spotName.trim()
        val matched = all.filter { it.matches(key) }
        val picked = matched.ifEmpty { all }
        return buildString {
            append("【").append(LifenScene.NAME).append("】景点讲解")
            when {
                key.isEmpty() -> append("（按游览顺序，可逐段念给用户）")
                matched.isEmpty() -> append("（没有匹配「").append(key).append("」的讲稿，先给全部）")
                else -> append("（讲解：").append(key).append("）")
            }
            append('\n')
            picked.forEachIndexed { index, script ->
                append(index + 1).append(". ").append(script.spot).append('\n')
                append("   讲解：").append(script.story).append('\n')
                append("   冷知识：").append(script.funFact).append('\n')
            }
            append("讲解节奏：每站点 1–2 分钟，先讲“这是什么”，再讲“为什么在这里”，最后留一句冷知识收尾。")
            append('\n').append(LifenScene.DATA_NOTE)
        }
    }
}
