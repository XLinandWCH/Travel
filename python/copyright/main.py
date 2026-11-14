# -*- coding: utf-8 -*-
"""一条命令出齐软著材料。脚本放在 python/copyright 下，配置和输出也在这里。

用法：
    python main.py                    # 按 config.json 生成全部材料
    python main.py --owner 张三        # 临时换著作权人，不改配置文件
    python main.py --project ../../app # 只扫描某个子目录
"""

import argparse
import json
from pathlib import Path

import applyform
import codepage
import config
import evidence
import manual
import pick
import scan
import statements


def parse_args():
    p = argparse.ArgumentParser(description="软件著作权申请材料生成")
    p.add_argument("--config", help="配置文件路径，默认用同目录的 config.json")
    p.add_argument("--project", help="覆盖要扫描的项目目录")
    p.add_argument("--name", help="覆盖软件全称")
    p.add_argument("--version", help="覆盖版本号")
    p.add_argument("--owner", help="覆盖著作权人")
    p.add_argument("--out", help="覆盖输出目录")
    return p.parse_args()


def override(cfg, args):
    for key, value in (("software_name", args.name), ("version", args.version),
                       ("owner", args.owner), ("output_dir", args.out),
                       ("project_root", args.project)):
        if value:
            cfg[key] = str(Path(value).resolve()) if key in ("output_dir", "project_root") else value
    return cfg


def clear_old(cfg):
    """同一个软件名下重新生成时，先清掉上一次的源程序 Word，免得新旧混在一起。"""
    folder = Path(cfg["output_dir"]) / "正式资料"
    if not folder.is_dir():
        return
    prefix = f"{cfg['software_name']}{cfg['version']}_源程序"
    for path in folder.glob(prefix + "*.docx"):
        path.unlink()


def report(cfg, stats, code_result, manual_check, form_files, work_dir, extra):
    out = Path(cfg["output_dir"]) / "生成报告.md"
    pages = [f"{Path(o['path']).name}：约 {o['pages']} 页（{o['lines']} 行，页码 {o.get('start_page', 1)} 起）"
             for o in code_result["outputs"]]
    lines = [
        f"# 生成报告（{cfg['software_name']} {cfg['version']}）",
        "",
        "## 基本信息",
        f"- 软件全称：{cfg['software_name']}",
        f"- 版本号：{cfg['version']}",
        f"- 著作权人：{cfg['owner']}",
        f"- 扫描目录：{cfg['project_root']}",
        "",
        "## 项目统计",
        f"- 源码文件：{stats['code_files']} 个",
        f"- 源码总行数：{stats['code_lines']} 行（申请表源程序量按此填写）",
        f"- 语言分布：{json.dumps(stats['by_lang'], ensure_ascii=False)}",
        "",
        "## 代码材料",
        f"- 抽取文件：{len(code_result.get('files', []))} 个，整理后有效行 {code_result['total_lines']} 行",
        *[f"- {line}" for line in pages],
        "",
        "## 操作手册",
        f"- 一级章节 {manual_check['chapters']} 个，功能章节 {manual_check['modules']} 个",
        f"- 配图：已插入 {manual_check['images']} 幅（登记 {manual_check['slots']} 幅），图注按章节编号，如“图 5-1”",
        f"- 套话检查：{'无命中' if not manual_check['banned'] else manual_check['banned']}",
        "",
        "## 输出文件",
        *[f"- {Path(o['path']).name}" for o in code_result["outputs"]],
        f"- {Path(form_files[1]).name}",
        f"- {Path(form_files[2]).name}",
        f"- {cfg['software_name']}{cfg['version']}_操作手册.docx",
        f"- 工作底稿目录：{work_dir}",
        *[f"- 工作底稿：{Path(p).name}" for p in extra],
        "",
        "## 交付前还要做的三件事",
        "1. 用 Word 或 PDF 再翻一遍两份源程序：页码应为 1-30 与 31-60，每页不少于 50 行代码。",
        "2. 手册里的图片取自 应用图片 目录，已按章节对应好；若页面改过版，把新图换进 截图原始 并改 config.json 的 images 后重跑。",
        "3. 个人申请需要身份证正反面扫描件，申请表要本人签字；签章页的承诺事项要看清楚再签。",
    ]
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return out


def main():
    args = parse_args()
    cfg = override(config.load(args.config), args)
    out_dir = Path(cfg["output_dir"])
    work_dir = out_dir / "工作底稿"
    work_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "正式资料").mkdir(parents=True, exist_ok=True)

    print(f"[1/5] 扫描 {cfg['project_root']}")
    stats = scan.summarize(cfg)
    print(f"      源码 {stats['code_files']} 个文件，共 {stats['code_lines']} 行")

    need = int(cfg["pages_per_doc"]) * int(cfg["lines_per_page"]) * 2
    factor = 1.2
    while True:
        # 源码里的空行会被去掉，按行数选出的文件可能不够 60 页，不够就再放一点进来
        files = pick.choose(cfg, stats["files"], int(need * factor))
        _, buckets, _ = codepage.prepare(cfg, files)
        if "front" in buckets or factor >= 5:
            break
        factor *= 1.4
    print(f"[2/5] 选中 {len(files)} 个文件，生成源程序 Word")
    clear_old(cfg)
    code_result = codepage.build(cfg, files, work_dir)
    code_result["files"] = files
    for item in code_result["outputs"]:
        print(f"      {Path(item['path']).name}  约 {item['pages']} 页")

    print("[3/5] 生成操作手册")
    manual_path, blocks, shot_info = manual.write(cfg)
    result = manual.write_check(cfg, manual.check(cfg, blocks), work_dir / "操作手册自检记录.md")
    print(f"      {manual_path.name}  章节 {result['chapters']} 个，配图 {result['images']} 幅")

    print("[4/5] 生成申请表信息")
    rows, form_docx, form_txt = applyform.build(cfg, stats["code_lines"])
    (work_dir / "project.json").write_text(json.dumps(
        {k: v for k, v in stats.items() if k != "files"}, ensure_ascii=False, indent=2), encoding="utf-8")

    print("[5/5] 生成自查清单与开发留痕")
    extra, commits = evidence.write(cfg, stats, code_result, result, work_dir)
    extra += statements.write(cfg, work_dir)
    print(f"      自查清单、留痕（{commits} 条提交）、两份声明模板")

    report_path = report(cfg, stats, code_result, result, (rows, form_docx, form_txt),
                        work_dir, extra)
    print(f"\n完成，材料在：{out_dir}")
    print(f"报告：{report_path.name}")


if __name__ == "__main__":
    main()
