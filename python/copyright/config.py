# -*- coding: utf-8 -*-
"""读取 config.json。路径都按配置文件所在目录解析，整包挪走也不用改。"""

import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
CONFIG_PATH = HERE / "config.json"


def load(path=None):
    p = Path(path) if path else CONFIG_PATH
    if not p.is_file():
        raise SystemExit(f"没找到配置文件：{p}")
    cfg = json.loads(p.read_text(encoding="utf-8"))
    base = p.parent
    for key, default in (("project_root", ".."), ("output_dir", "软件著作权申请资料")):
        value = Path(cfg.get(key, default))
        cfg[key] = str(value if value.is_absolute() else (base / value).resolve())
    cfg.setdefault("pages_per_doc", 30)
    cfg.setdefault("lines_per_page", 54)
    cfg.setdefault("code_wrap_col", 90)
    cfg.setdefault("exclude_paths", ["build", ".git"])
    return cfg


def doc_path(cfg, suffix, ext="docx"):
    out = Path(cfg["output_dir"]) / "正式资料"
    out.mkdir(parents=True, exist_ok=True)
    return out / f"{cfg['software_name']}{cfg['version']}_{suffix}.{ext}"


def header_text(cfg):
    return f"{cfg['software_name']} {cfg['version']}"
