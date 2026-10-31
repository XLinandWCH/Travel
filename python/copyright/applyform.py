# -*- coding: utf-8 -*-
"""申请表信息：一份 Word 方便看，一份 txt 方便复制到登记系统。"""

from pathlib import Path

import docxhelp


def rows_of(cfg, source_lines):
    program_pages = int(cfg["pages_per_doc"]) * 2
    return [
        ("软件全称", cfg["software_name"]),
        ("软件简称", cfg.get("short_name", "")),
        ("版本号", cfg["version"]),
        ("著作权人", cfg["owner"]),
        ("开发完成日期", cfg["complete_date"]),
        ("首次发表日期", cfg.get("publish_state", "未发表")),
        ("开发方式", cfg.get("develop_mode", "独立开发")),
        ("权利取得方式", cfg.get("rights_source", "原始取得")),
        ("权利范围", cfg.get("rights_scope", "全部权利")),
        ("软件分类", cfg.get("software_category", "应用软件")),
        ("软件说明", "原创软件"),
        ("开发的硬件环境", "处理器四核 2.4GHz 及以上、内存 16GB 及以上的计算机一台"),
        ("运行的硬件环境", cfg["run_hardware"]),
        ("开发该软件的操作系统", cfg["dev_os"]),
        ("软件开发环境/开发工具", cfg["dev_tools"]),
        ("该软件的运行平台/操作系统", cfg["run_software"]),
        ("软件运行支撑环境/支持软件", "客户端为手机操作系统自带的运行环境；服务端需要 Java 运行环境与数据库服务"),
        ("编程语言", cfg["language"]),
        ("源程序量", f"{source_lines} 行"),
        ("程序鉴别材料", f"源程序前 30 页、后 30 页，共 {program_pages} 页"),
        ("开发的起始日期", cfg.get("start_date", "2025年11月1日")),
        ("开发目的", cfg["purpose"]),
        ("面向行业", cfg["industry"]),
        ("主要功能", cfg["main_functions"]),
        ("技术特点", cfg["tech_features"]),
        ("文档鉴别材料", "软件操作手册，共 1 册"),
    ]


def write_docx(cfg, rows, path):
    doc = docxhelp.base_doc(body_size=11, line=1.5)
    docxhelp.header(doc.sections[0], f"{cfg['software_name']} {cfg['version']}")
    docxhelp.title(doc, "计算机软件著作权登记申请表信息", size=16)
    docxhelp.para(doc, "以下内容按登记系统的填写顺序整理，可逐项复制到申请表对应栏目。",
                  size=11, indent=True, after=8)
    table_rows = [["栏目", "填写内容"]] + [[k, v] for k, v in rows]
    docxhelp.table(doc, table_rows, widths=[4.4, 10.8], size=10.5, header_row=True)
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(path))
    return path


def write_txt(cfg, rows, path):
    lines = [f"{'=' * 8} 计算机软件著作权登记申请表信息 {'=' * 8}", ""]
    for key, value in rows:
        lines.append(f"【{key}】")
        lines.append(str(value))
        lines.append("")
    Path(path).write_text("\n".join(lines), encoding="utf-8")
    return path


def build(cfg, source_lines):
    rows = rows_of(cfg, source_lines)
    out_dir = Path(cfg["output_dir"]) / "正式资料"
    out_dir.mkdir(parents=True, exist_ok=True)
    stem = f"{cfg['software_name']}{cfg['version']}_申请表信息"
    return rows, write_docx(cfg, rows, out_dir / f"{stem}.docx"), write_txt(cfg, rows, out_dir / f"{stem}.txt")
