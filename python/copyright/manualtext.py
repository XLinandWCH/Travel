# -*- coding: utf-8 -*-
"""拼操作手册的正文。段落怎么排、用哪个连接词都由模块名做种子，各章不会一个腔调。"""

import humanize

CN = ["一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
      "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十"]


def _h1(index, text):
    return ("h1", f"{CN[index]}、{text}")


def _cat(*parts):
    return "".join(p.strip() for p in parts if p and p.strip())


def module_blocks(index, mod):
    seed = humanize.seed_of(mod["title"])
    blocks = [_h1(index, mod["title"])]

    entry = humanize.clean(mod.get("entry", ""))
    use = humanize.clean(mod.get("use", ""))
    elements = humanize.clean(mod.get("elements", ""))
    steps = [humanize.clean(s) for s in mod.get("steps", [])]
    rules = [humanize.clean(r) for r in mod.get("rules", [])]
    feedback = humanize.clean(mod.get("feedback", ""))
    shot = mod.get("shot", "")
    images = mod.get("images") or ([mod["image"]] if mod.get("image") else [])

    # 有的章节把入口和页面内容并成一段，读起来更连贯，也免得每章都一个模子
    if seed % 3 == 0:
        blocks.append(("p", _cat(entry, use, elements)))
    else:
        blocks.append(("p", _cat(entry, use)))
        blocks.append(("p", elements))

    step_text = humanize.choose(seed, humanize.STEP_LEADS) + humanize.join_steps(steps, seed)
    rule_text = ""
    if rules:
        rule_text = humanize.choose(seed, humanize.RULE_LEADS, 2) + humanize.join_rules(rules, seed)

    if seed % 4 == 2 and rule_text:
        blocks.append(("p", rule_text))
        blocks.append(("p", step_text))
    else:
        blocks.append(("p", step_text))
        if rule_text:
            blocks.append(("p", rule_text))

    blocks.append(("p", feedback))
    if shot or images:
        # 图注跟图走，哪一章用哪几张图写在 config.json 的 images 里
        blocks.append(("shot", {"caption": shot, "images": images}))
    return blocks


def cover_blocks(cfg):
    """单独一页封面，后面再进正文。"""
    return [
        ("cover_title", cfg["software_name"]),
        ("cover_sub", cfg["version"]),
        ("cover_sub", "操 作 手 册"),
        ("cover_line", ""),
        ("cover_line", f"著作权人：{cfg['owner']}"),
        ("cover_line", f"开发完成日期：{cfg['complete_date']}"),
        ("break", ""),
    ]


def front_blocks(cfg):
    name = cfg["software_name"]
    version = cfg["version"]
    blocks = [("title", f"{name}{version}"), ("subtitle", "操作手册")]
    blocks.append(_h1(0, "相关文档"))
    blocks.append(("p", "与本次申请配套的文档列在下表，使用过程中如需了解页面之外的设计考虑，可以对照查阅。"))
    blocks.append(("table", [
        ["文档名称", "主要内容", "适用场景"],
        [f"《{name}总体设计》", "模块划分、页面结构与主要数据表", "需要了解整体结构时查阅"],
        [f"《{name}详细设计》", "对话、路线、语音等关键功能的处理过程", "需要了解功能细节时查阅"],
        [f"《{name}测试案例》", "主要功能的测试用例与测试记录", "核对功能完成情况时查阅"],
        [f"《{name}操作手册》", "页面用途、操作方法与常见问题", "交付用户与培训时使用"],
    ]))
    blocks.append(_h1(1, "软件简介"))
    for text in cfg.get("manual_intro", []):
        blocks.append(("p", humanize.clean(text)))
    blocks.append(_h1(2, "功能特点"))
    for text in cfg.get("manual_features", []):
        blocks.append(("p", humanize.clean(text)))
    blocks.append(_h1(3, "运行环境"))
    blocks.append(("p", "客户端安装在手机上使用。硬件方面，" + humanize.clean(cfg["run_hardware"]) + "。"))
    blocks.append(("p", "软件方面，" + humanize.clean(cfg["run_software"]) + "。首次使用需要联网完成登录，"
                   "之后查看本地聊天记录不需要联网；语音输入、AI 问答、路线生成与视频讲解需要保持网络连接。"))
    return blocks


def back_blocks(cfg, index):
    blocks = [_h1(index, "常见问题解答")]
    for question, answer in cfg.get("faq", []):
        blocks.append(("q", question if question.endswith("？") else question + "？"))
        blocks.append(("a", humanize.clean(answer)))
    blocks.append(_h1(index + 1, "术语表"))
    rows = [["术语", "说明"]]
    rows.extend([[term, humanize.clean(desc)] for term, desc in cfg.get("glossary", [])])
    blocks.append(("table", rows))
    return blocks


def build(cfg):
    blocks = cover_blocks(cfg) if cfg.get("cover", True) else []
    blocks.extend(front_blocks(cfg))
    index = 4
    for mod in cfg.get("modules", []):
        blocks.extend(module_blocks(index, mod))
        index += 1
    blocks.extend(back_blocks(cfg, index))
    return blocks
