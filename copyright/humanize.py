# -*- coding: utf-8 -*-
"""压一压文字里的套话。词表按审核场景里常见的那几句攒的，还有一批连接词用来换口气。"""

import hashlib

SWAPS = [
    ("旨在", "主要用于"),
    ("赋能", "帮助"),
    ("一站式", "集中"),
    ("显著提升", "明显提高"),
    ("大幅提升", "明显提高"),
    ("高效便捷", "省事"),
    ("极大地", "很大程度上"),
    ("全方位", "各方面"),
    ("多维度", "多个角度"),
    ("致力于", "着力于"),
    ("强大能力", "较完整的功能"),
    ("丰富功能", "多种功能"),
    ("极致", "尽可能好"),
    ("无缝", "顺畅"),
    ("深度赋能", "深入支持"),
    ("智慧化", "具备一定判断能力"),
    ("线上线下", "线上与现场"),
]

# 连接词按种子挑，同一个模块每次生成的结果不变，不同模块之间不会整齐划一
STEP_LEADS = ["具体操作时，", "使用时，", "实际用起来，", "操作顺序并不复杂，"]
# 按位置取词，保证先后关系读得顺；每组三个备选，不同章节不会重复同一个
STEP_ORDER = [
    ["然后", "接着", "之后"],
    ["接着", "随后", "最后"],
    ["随后", "最后", "接着"],
    ["最后", "随后", "接着"],
]
RULE_LEADS = ["有几点要注意，", "使用时还有几处限制，", "另外要留意这几点，"]
RULE_ORDER = ["另外", "还有", "同时", "此外", "再者"]


def seed_of(text):
    return int(hashlib.md5(text.encode("utf-8")).hexdigest()[:8], 16)


def choose(seed, options, offset=0):
    return options[(seed + offset) % len(options)]


def clean(text):
    for old, new in SWAPS:
        text = text.replace(old, new)
    return text


def join_steps(steps, seed):
    if not steps:
        return ""
    body = steps[0]
    for i, step in enumerate(steps[1:]):
        pool = STEP_ORDER[min(i, len(STEP_ORDER) - 1)]
        body += "；" + pool[(seed + i) % len(pool)] + "，" + step
    return body.rstrip("；") + "。"


def join_rules(rules, seed):
    if not rules:
        return ""
    body = rules[0]
    for i, rule in enumerate(rules[1:]):
        body += "；" + RULE_ORDER[min(i, len(RULE_ORDER) - 1)] + "，" + rule
    return body.rstrip("；") + "。"


def sentence_span(text):
    """粗略估计句子长短，自检时用来看段落是不是太碎或者太糊。"""
    marks = [c for c in text if c in "。！？；"]
    return len(text) / max(1, len(marks))
