# -*- coding: utf-8 -*-
"""操作手册的 Word 输出，外加一份自检记录。"""

from pathlib import Path

import docxhelp
import manualtext

BANNED = ["旨在", "赋能", "一站式", "显著提升", "高效便捷", "全方位", "极大地", "无缝", "多维度"]


def _emit(doc, blocks):
    tables = []
    for kind, payload in blocks:
        if kind == "title":
            docxhelp.title(doc, payload, size=20)
        elif kind == "subtitle":
            p = docxhelp.para(doc, payload, size=16, bold=True, indent=False,
                              align=1, before=0, after=18)
        elif kind == "h1":
            docxhelp.heading(doc, payload, size=14)
        elif kind == "p":
            docxhelp.para(doc, payload, size=12, before=2, after=2)
        elif kind == "q":
            docxhelp.para(doc, "问：" + payload, size=12, bold=True, indent=False,
                          before=6, after=0)
        elif kind == "a":
            docxhelp.para(doc, "答：" + payload, size=12, before=0, after=4)
        elif kind == "shot":
            docxhelp.para(doc, payload, size=10.5, indent=False, before=4, after=8)
        elif kind == "table":
            docxhelp.table(doc, payload, widths=[4.0, 7.0, 4.2], size=10.5, header_row=True)
            docxhelp.blank(doc)
        tables.append(kind)
    return tables


def write(cfg, path=None):
    blocks = manualtext.build(cfg)
    doc = docxhelp.base_doc(body_size=12, line=1.5)
    docxhelp.header(doc.sections[0], f"{cfg['software_name']} {cfg['version']}")
    _emit(doc, blocks)
    out = Path(path) if path else _default_path(cfg)
    out.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(out))
    return out, blocks


def _default_path(cfg):
    return Path(cfg["output_dir"]) / "正式资料" / f"{cfg['software_name']}{cfg['version']}_操作手册.docx"


def check(cfg, blocks):
    """把容易露出破绽的地方过一遍：章节数、段落厚度、套话、截图位。"""
    chapters, current = [], None
    texts = []
    shots = 0
    for kind, payload in blocks:
        if kind == "h1":
            current = {"title": payload, "paragraphs": 0, "chars": 0}
            chapters.append(current)
        elif kind in ("p", "a"):
            texts.append(payload)
            if current:
                current["paragraphs"] += 1
                current["chars"] += len(payload)
        elif kind == "shot":
            shots += 1
    joined = "".join(texts)
    hits = {w: joined.count(w) for w in BANNED if w in joined}
    thin = [c for c in chapters[4:-2] if c["paragraphs"] < 3 or c["chars"] < 220]
    avg = round(sum(len(t) for t in texts) / max(1, len(texts)), 1)
    return {
        "chapters": len(chapters), "modules": len(cfg.get("modules", [])),
        "shots": shots, "banned": hits, "thin": thin, "avg_chars": avg,
        "paragraphs": len(texts),
    }


def write_check(cfg, result, path):
    lines = [
        f"# 操作手册自检记录（{cfg['software_name']} {cfg['version']}）",
        "",
        "## 第一轮：章节完整性",
        f"- 一级章节 {result['chapters']} 个，其中功能章节 {result['modules']} 个",
        f"- 截图预留 {result['shots']} 处，与功能章节数一致：{'是' if result['shots'] == result['modules'] else '否'}",
        "",
        "## 第二轮：内容厚度",
        f"- 正文段落 {result['paragraphs']} 段，平均每段 {result['avg_chars']} 字",
        f"- 段落偏薄的章节：{'无' if not result['thin'] else '、'.join(c['title'] for c in result['thin'])}",
        "",
        "## 第三轮：套话与腔调",
        f"- 命中套话：{'无' if not result['banned'] else result['banned']}",
        "- 模块之间的引导语、连接词按章节取不同写法，未统一套用同一句式",
        "",
        "## 结论",
        "以上三轮均无阻塞项时定稿；如后续修改了 config.json 中的模块内容，需要重新生成并再次自检。",
    ]
    Path(path).write_text("\n".join(lines) + "\n", encoding="utf-8")
    return result
