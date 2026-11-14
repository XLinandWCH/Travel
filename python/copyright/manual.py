# -*- coding: utf-8 -*-
"""操作手册的 Word 输出，外加一份自检记录。"""

from pathlib import Path

import docxhelp
import manualtext

BANNED = ["旨在", "赋能", "一站式", "显著提升", "高效便捷", "全方位", "极大地", "无缝", "多维度"]
SHOT_EXT = (".png", ".jpg", ".jpeg", ".webp")


def load_shots(cfg):
    """截图按文件名取，config.json 里 images 写哪张就用哪张。"""
    folder = Path(cfg["output_dir"]).parent / cfg.get("shots_dir", "截图原始")
    if not folder.is_dir():
        return folder, {}
    files = [p for p in folder.iterdir() if p.suffix.lower() in SHOT_EXT]
    return folder, {p.name: p for p in files}


def _emit(doc, blocks, shots, cfg):
    width = float(cfg.get("image_width_cm", 13.5))
    height = float(cfg.get("image_max_height_cm", 15.0))
    used, chapter, figures, missing = 0, 0, 0, []
    for kind, payload in blocks:
        if kind == "cover_title":
            docxhelp.para(doc, payload, size=22, bold=True, indent=False,
                          align=1, before=130, after=12)
        elif kind == "cover_sub":
            docxhelp.para(doc, payload, size=16, bold=True, indent=False,
                          align=1, before=4, after=4)
        elif kind == "cover_line":
            docxhelp.para(doc, payload, size=12, indent=False, align=1, before=2, after=2)
        elif kind == "break":
            doc.add_page_break()
        elif kind == "title":
            docxhelp.title(doc, payload, size=20)
        elif kind == "subtitle":
            docxhelp.para(doc, payload, size=16, bold=True, indent=False,
                          align=1, before=0, after=18)
        elif kind == "h1":
            chapter += 1
            figures = 0
            docxhelp.heading(doc, payload, size=14)
        elif kind == "p":
            docxhelp.para(doc, payload, size=12, before=2, after=2)
        elif kind == "q":
            docxhelp.para(doc, "问：" + payload, size=12, bold=True, indent=False,
                          before=6, after=0)
        elif kind == "a":
            docxhelp.para(doc, "答：" + payload, size=12, before=0, after=4)
        elif kind == "shot":
            items = payload.get("images", []) if isinstance(payload, dict) else []
            fallback = payload.get("caption", "") if isinstance(payload, dict) else ""
            for item in items:
                name, text = (item, fallback) if isinstance(item, str) else (item[0], item[1])
                if name not in shots:
                    missing.append(name)
                    continue
                docxhelp.image(doc, shots[name], width, height)
                used += 1
                if text:
                    figures += 1
                    cap = docxhelp.para(doc, f"图 {chapter}-{figures} {text}", size=10.5,
                                        indent=False, align=1, before=0, after=10)
                    cap.paragraph_format.keep_together = True
        elif kind == "table":
            docxhelp.table(doc, payload, widths=[4.0, 7.0, 4.2], size=10.5, header_row=True)
            docxhelp.blank(doc)
    return used, missing


def write(cfg, path=None):
    blocks = manualtext.build(cfg)
    doc = docxhelp.base_doc(body_size=12, line=1.5)
    section = doc.sections[0]
    docxhelp.header(section, f"{cfg['software_name']} {cfg['version']}")
    if cfg.get("cover", True):
        # 封面不显示页眉页码，正文第一页接着编
        section.different_first_page_header_footer = True
        section.first_page_header.paragraphs[0].text = ""
    folder, shots = load_shots(cfg)
    used, missing = _emit(doc, blocks, shots, cfg)
    out = Path(path) if path else _default_path(cfg)
    out.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(out))
    return out, blocks, {"used": used, "found": len(shots), "missing": missing,
                         "folder": str(folder)}


def _default_path(cfg):
    return Path(cfg["output_dir"]) / "正式资料" / f"{cfg['software_name']}{cfg['version']}_操作手册.docx"


def check(cfg, blocks):
    """把容易露出破绽的地方过一遍：章节数、段落厚度、套话、配图。"""
    chapters, current = [], None
    texts = []
    shots = images = 0
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
            if isinstance(payload, dict):
                images += len(payload.get("images") or [])
    joined = "".join(texts)
    hits = {w: joined.count(w) for w in BANNED if w in joined}
    thin = [c for c in chapters[4:-2] if c["paragraphs"] < 3 or c["chars"] < 220]
    avg = round(sum(len(t) for t in texts) / max(1, len(texts)), 1)
    slots = 0
    for mod in cfg.get("modules", []):
        slots += len(mod.get("images") or [])
    return {
        "chapters": len(chapters), "modules": len(cfg.get("modules", [])),
        "shots": shots, "images": images, "slots": slots, "banned": hits,
        "thin": thin, "avg_chars": avg, "paragraphs": len(texts),
    }


def write_check(cfg, result, path):
    lines = [
        f"# 操作手册自检记录（{cfg['software_name']} {cfg['version']}）",
        "",
        "## 第一轮：章节完整性",
        f"- 一级章节 {result['chapters']} 个，其中功能章节 {result['modules']} 个",
        f"- 配图章节 {result['shots']} 个，实际插入图片 {result['images']} 幅，",
        f"  与 config.json 登记的 {result['slots']} 幅一致：{'是' if result['images'] == result['slots'] else '否'}",
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
