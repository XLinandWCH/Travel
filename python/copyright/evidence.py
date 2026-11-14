# -*- coding: utf-8 -*-
"""补三份底稿：材料自查清单、开发留痕（git 提交记录）、文档创作说明。

自查清单是提交前逐条打勾用的；留痕和创作说明是万一被问“文档是不是 AI 写的”
时能拿出来的东西，平时不用交。
"""

import subprocess
from pathlib import Path

CHECKS = [
    ("软件名称一致", "申请表、两份源程序页眉、手册封面、文件名全部为同一名称与版本号"),
    ("源程序页数", "前 30 页、后 30 页，合计 60 页；不足 60 页时全部提交"),
    ("源程序页码", "前 30 页编号 1-30，后 30 页接着编 31-60"),
    ("源程序行数", "每页不少于 50 行，页眉标注软件全称与版本号"),
    ("文档页数", "操作手册不足 60 页，全部提交；每页不少于 30 行（有图除外）"),
    ("文档内容", "章节与软件实际功能一一对应，配图为软件真实运行界面"),
    ("功能表述", "手册功能章节与申请表“主要功能”保持一致，不出现软件没有的模块"),
    ("申请表字段", "开发完成日期、起始日期、发表状态、源程序量按实际情况填写"),
    ("个人材料", "身份证正反面扫描件、申请表签字、签章页承诺逐字抄写"),
    ("AI 使用", "代码与文档由本人完成并逐段核对；如被问及，能说明 AI 工具用在哪些环节"),
]


def git_log(cfg, limit=120):
    root = Path(cfg["project_root"]).resolve()
    cmd = ["git", "-C", str(root), "log", f"-{limit}", "--date=short",
           "--pretty=format:%h\t%ad\t%an\t%s"]
    try:
        out = subprocess.run(cmd, capture_output=True, text=True,
                             encoding="utf-8", errors="replace", timeout=60)
    except (OSError, subprocess.SubprocessError):
        return []
    if out.returncode != 0:
        return []
    rows = []
    for line in out.stdout.splitlines():
        parts = line.split("\t")
        if len(parts) == 4 and parts[3]:
            rows.append(parts)
    return rows


def checklist(cfg, stats, code_result, manual_result, rows, path):
    pages = "、".join(f"{Path(o['path']).stem} {o['pages']} 页（页码 {o.get('start_page', 1)} 起）"
                     for o in code_result["outputs"])
    lines = [
        f"# 材料自查清单（{cfg['software_name']}{cfg['version']}）",
        "",
        "逐条核对，确认后在方框里打勾再提交。",
        "",
        "| 项目 | 要求 | 结论 |",
        "| --- | --- | --- |",
    ]
    for name, desc in CHECKS:
        lines.append(f"| {name} | {desc} | 待核对 |")
    lines += [
        "",
        "## 本机生成时要留意的数字",
        f"- 源程序量（申请表填写）：{stats['code_lines']} 行，源码文件 {stats['code_files']} 个",
        f"- 代码材料：{pages}",
        f"- 操作手册：一级章节 {manual_result['chapters']} 个，功能章节 {manual_result['modules']} 个，"
        f"配图 {manual_result['images']} 幅",
        f"- 开发留痕：抓到 {len(rows)} 条提交记录，见同目录《开发留痕（git提交记录）.md》",
        "",
        "## 常见打回原因对照",
        "1. 源程序页数不足或页码从 1 编两次——两份材料都从 1 开始编就错了，后 30 页要接 31。",
        "2. 页眉没写软件全称和版本，或与申请表写法不一致（多一个空格、版本写成 V1）。",
        "3. 文档里出现了软件没有的页面，或截图是别处的界面，一问就对不上。",
        "4. 申请表里的源程序量随手写，与提交的 60 页代码量明显不符。",
        "5. 文档通篇套话、没有任何真实界面与操作细节，被判定为模板化材料。",
    ]
    Path(path).write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def trace(cfg, rows, path):
    lines = [
        f"# 开发留痕（git 提交记录）",
        "",
        f"仓库：{cfg['project_root']}",
        f"软件：{cfg['software_name']}{cfg['version']}",
        f"开发起始日期：{cfg['start_date']}　开发完成日期：{cfg['complete_date']}",
        "",
        "以下记录取自项目仓库本身的提交历史，用于说明代码与功能是逐步迭代出来的，"
        "可以作为“文档描述与代码实现一致”的佐证材料，平时不需要提交。",
        "",
        "| 提交 | 日期 | 提交人 | 说明 |",
        "| --- | --- | --- | --- |",
    ]
    for sha, day, who, subject in rows:
        lines.append(f"| {sha} | {day} | {who} | {subject} |")
    if not rows:
        lines.append("| — | — | — | 未取到提交记录，请确认本机 git 可用且项目在 git 仓库中 |")
    Path(path).write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def ai_note(cfg, path):
    """一页纸说明，只在收到“文档 AIGC 检出率高”这类补正时才用。"""
    lines = [
        f"# 文档创作说明（备用）",
        "",
        f"软件全称：{cfg['software_name']}　版本号：{cfg['version']}",
        f"著作权人：{cfg['owner']}",
        "",
        "> 这份说明不随申请材料提交。若收到“文档鉴别材料 AIGC 检出率高”一类的补正通知，"
        "按下面的格式如实填写、签字后作为补正材料上传。内容必须是真的，写多少就做多少。",
        "",
        "## 一、文档撰写情况",
        "- 撰写人：________　撰写时间段：____年__月 至 ____年__月",
        "- 撰写过程：______（例如：先按软件实际页面列章节，再逐章边操作边记录，最后统一文字）",
        "- 是否使用 AI 工具：____（用到就写，例如“用于调整语句通顺度”，不必写成 0%）",
        "- AI 参与环节与占比：______（自行估算，如“约两成段落的措辞，功能与操作内容均为自己撰写”）",
        "",
        "## 二、人工完成的部分",
        "1. 章节划分按软件实际页面来定，未收录软件里不存在的功能。",
        "2. 每章的入口、操作步骤、异常提示均对照实际运行界面逐条抄录，例如登录页的“暂不登录”、"
        "语音输入的“正在聆听，请说话”、消息中心的空状态“暂无公告”。",
        "3. 手册中的 __ 幅配图为软件运行时的真实截图，与章节一一对应。",
        "4. 术语表与常见问题取自开发过程中实际遇到的情况（权限、地图密钥、离线可查历史等）。",
        "",
        "## 三、可提供的证明材料",
        "- 开发留痕：项目仓库提交记录（见《开发留痕（git提交记录）.md》）。",
        "- 界面截图原始文件与拍摄时间。",
        "- 手册中出现的控件名称与提示语，可在工程源码中检索到对应字符串。",
        "- 申请表、源代码与文档三者之间的功能对应关系说明（即本目录的自查清单）。",
        "",
        "## 四、结论",
        "文档内容经本人逐段核对，描述的是本软件的实际功能与操作方法，"
        "体现本人的设计与实现思路；对不符合实际的表述已逐处改正。",
        "",
        "著作权人签字：____________　　日期：____年__月__日",
    ]
    Path(path).write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def write(cfg, stats, code_result, manual_result, work_dir):
    work_dir = Path(work_dir)
    rows = git_log(cfg)
    files = [
        checklist(cfg, stats, code_result, manual_result, rows, work_dir / "材料自查清单.md"),
        trace(cfg, rows, work_dir / "开发留痕（git提交记录）.md"),
        ai_note(cfg, work_dir / "文档创作说明（如遇补正再用）.md"),
    ]
    return files, len(rows)
