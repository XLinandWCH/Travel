# -*- coding: utf-8 -*-
"""扫描项目：统计源码文件、行数，顺带把说明文档路径记下来。"""

import os
from pathlib import Path

LANG = {
    ".kt": "Kotlin", ".kts": "Kotlin", ".swift": "Swift", ".java": "Java",
    ".py": "Python", ".ts": "TypeScript", ".tsx": "TypeScript",
    ".js": "JavaScript", ".jsx": "JavaScript", ".sql": "SQL",
    ".c": "C", ".h": "C", ".cpp": "C++", ".cs": "C#", ".go": "Go", ".rs": "Rust",
}
DOC = {".md", ".txt"}
MAX_BYTES = 2 * 1024 * 1024


def _blocked(rel_parts, excludes):
    for part in rel_parts:
        if part in excludes:
            return True
        if part.startswith(".venv") or part.endswith(".egg-info"):
            return True
    return False


def count_lines(path):
    try:
        raw = path.read_bytes()
    except OSError:
        return 0
    if b"\x00" in raw[:4096]:
        return 0
    return raw.count(b"\n") + (0 if raw.endswith(b"\n") else 1)


def walk(cfg, want_code=True, want_doc=False):
    root = Path(cfg["project_root"])
    excludes = set(cfg.get("exclude_paths", []))
    code, docs = [], []
    for cur, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if not _blocked([d], excludes)]
        cur_path = Path(cur)
        rel_dir = cur_path.relative_to(root).parts
        if _blocked(rel_dir, excludes):
            continue
        for name in files:
            path = cur_path / name
            try:
                size = path.stat().st_size
            except OSError:
                continue
            if size == 0 or size > MAX_BYTES:
                continue
            ext = path.suffix.lower()
            rel = str(path.relative_to(root)).replace("\\", "/")
            if want_code and ext in LANG:
                lines = count_lines(path)
                if lines:
                    code.append({
                        "path": str(path), "rel": rel, "ext": ext,
                        "lang": LANG[ext], "lines": lines, "size": size,
                    })
            elif want_doc and ext in DOC:
                docs.append({"path": str(path), "rel": rel, "size": size,
                             "lines": count_lines(path)})
    return code, docs


def summarize(cfg):
    code, docs = walk(cfg, True, True)
    by_lang = {}
    for item in code:
        by_lang[item["lang"]] = by_lang.get(item["lang"], 0) + item["lines"]
    return {
        "project_root": cfg["project_root"],
        "code_files": len(code),
        "code_lines": sum(i["lines"] for i in code),
        "by_lang": dict(sorted(by_lang.items(), key=lambda kv: -kv[1])),
        "docs": sorted(docs, key=lambda d: -d["lines"])[:20],
        "files": code,
    }
