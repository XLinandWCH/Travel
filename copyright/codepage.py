# -*- coding: utf-8 -*-
"""把选中的源码排成代码材料：去空行、按显示宽度折行、切前 30 页和后 30 页。"""

import json
import unicodedata
from pathlib import Path

import docxhelp
import pick


def width_of(ch):
    return 2 if unicodedata.east_asian_width(ch) in ("W", "F") else 1


def wrap_line(text, limit):
    """按显示宽度折行。快超宽时优先在空格处断开，免得把英文标识符切成两半。"""
    out, buf, used = [], [], 0
    for ch in text:
        w = width_of(ch)
        if used + w > limit and buf:
            cut = -1
            if ch not in " \t":
                for i in range(len(buf) - 1, max(-1, len(buf) - 20), -1):
                    if buf[i] == " ":
                        cut = i
                        break
            if cut > 0:
                out.append("".join(buf[:cut]).rstrip())
                buf = buf[cut + 1:]
                used = sum(width_of(c) for c in buf)
            else:
                out.append("".join(buf))
                buf, used = [], 0
        buf.append(ch)
        used += w
    if buf:
        out.append("".join(buf))
    return out


def load_lines(files, limit):
    """源码整文件读进来，空行去掉，长行按 90 列折好，Word 里就不会再自动换行。"""
    out = []
    for item in pick.listing_order(files):
        try:
            text = Path(item["path"]).read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        for raw in text.splitlines():
            line = raw.expandtabs(4).rstrip()
            if not line.strip():
                continue
            out.extend(wrap_line(line, limit))
    return out


def split(lines, pages_per_doc, lines_per_page):
    per_doc = pages_per_doc * lines_per_page
    if len(lines) >= per_doc * 2:
        return {"front": lines[:per_doc], "back": lines[-per_doc:]}
    return {"all": lines}


def _estimate_pages(lines, lines_per_page):
    return max(1, round(len(lines) / lines_per_page + 0.4999))


def prepare(cfg, files):
    limit = int(cfg.get("code_wrap_col", 90))
    lines = load_lines(files, limit)
    buckets = split(lines, int(cfg["pages_per_doc"]), int(cfg["lines_per_page"]))
    return lines, buckets, limit


def build(cfg, files, manifest_dir):
    lines, buckets, limit = prepare(cfg, files)
    outputs = []
    for key, chunk in buckets.items():
        suffix = {"front": "源程序（前30页）", "back": "源程序（后30页）", "all": "源程序（全部）"}[key]
        path = doc_path_for(cfg, suffix)
        docxhelp.code_doc(cfg, chunk, path)
        outputs.append({
            "key": key, "path": str(path), "lines": len(chunk),
            "pages": _estimate_pages(chunk, int(cfg["lines_per_page"])),
        })
    write_manifest(cfg, files, outputs, manifest_dir, len(lines), limit)
    return {"total_lines": len(lines), "outputs": outputs,
            "pages": _estimate_pages(lines, int(cfg["lines_per_page"]))}


def doc_path_for(cfg, suffix):
    out = Path(cfg["output_dir"]) / "正式资料"
    out.mkdir(parents=True, exist_ok=True)
    return out / f"{cfg['software_name']}{cfg['version']}_{suffix}.docx"


def write_manifest(cfg, files, outputs, manifest_dir, total_lines, limit):
    manifest_dir = Path(manifest_dir)
    manifest_dir.mkdir(parents=True, exist_ok=True)
    rows = [
        "| 序号 | 源文件 | 行数 | 选取理由 |",
        "| --- | --- | --- | --- |",
    ]
    for i, item in enumerate(pick.listing_order(files), 1):
        rows.append(f"| {i} | {item['rel']} | {item['lines']} | {item['reason']} |")
    head = [
        f"# 代码提取清单（{cfg['software_name']} {cfg['version']}）",
        "",
        f"- 抽取文件数：{len(files)}",
        f"- 有效代码行（去空行后含折行）：{total_lines}",
        f"- 单行折行宽度：{limit} 显示列",
        f"- 输出：{'、'.join(Path(o['path']).name for o in outputs)}",
        "",
    ]
    (manifest_dir / "代码提取清单.md").write_text("\n".join(head + rows) + "\n", encoding="utf-8")
    payload = {
        "software": cfg["software_name"], "version": cfg["version"],
        "total_lines": total_lines, "wrap_col": limit,
        "files": [{"rel": i["rel"], "lines": i["lines"], "reason": i["reason"]}
                  for i in pick.listing_order(files)],
        "outputs": outputs,
    }
    (manifest_dir / "代码提取清单.json").write_text(
        json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
