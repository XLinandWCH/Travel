# -*- coding: utf-8 -*-
"""两份声明模板：合法合规及原创性声明、软件名称说明。

带生成式 AI 功能的软件，审核时可能要求补这两份。先做好放底稿里，
真要补正时打印、签字、扫描上传就行，不用再从头写。
"""

from pathlib import Path

import docxhelp


def _doc(cfg, title):
    doc = docxhelp.base_doc(body_size=12, line=1.5)
    section = doc.sections[0]
    docxhelp.header(section, f"{cfg['software_name']} {cfg['version']}")
    docxhelp.title(doc, title, size=18)
    return doc, section


def _sign(doc, cfg):
    docxhelp.blank(doc)
    for text in (f"软件全称：{cfg['software_name']}", f"版本号：{cfg['version']}",
                 f"著作权人（签字）：{cfg['owner']}", "日期：      年    月    日"):
        docxhelp.para(doc, text, size=12, indent=False, before=6, after=6)


def compliance(cfg, path):
    doc, _ = _doc(cfg, "软件合法合规及原创性声明")
    blocks = [
        f"本人为《{cfg['software_name']}{cfg['version']}》（以下简称“本软件”）的著作权人，"
        "现就本软件的开发、内容与数据处理情况作如下声明：",
        "一、原创性。本软件的源程序、界面设计、操作手册及说明文字均由本人独立完成，"
        "未抄袭他人软件，未使用他人享有著作权的代码、图片与文字；引用的公开资料已在知识库中注明来源并仅作事实性整理。",
        "二、生成式功能。本软件提供 AI 对话问答、语音识别与语音播报、视频讲解等功能，"
        "通过调用第三方已备案的大模型与语音服务接口实现，本软件不自行训练或发布生成式模型；"
        "对话与讲解内容均由模型实时生成，页面已标注“内容由 AI 生成”，并在常见问题中提示票价、"
        "开放时间等时效信息以现场公示为准。",
        "三、内容安全。本软件不提供面向公众的信息发布服务，生成内容仅返回给当前发起提问的用户；"
        "本软件设置了意见反馈与消息中心渠道，用户可对不当内容进行反馈，著作权人核实后处理。",
        "四、数据与隐私。本软件仅在实现功能所必需的范围内收集账号信息、会话记录与反馈内容，"
        "会话记录保存在用户设备本地，用户可在历史管理中自行清空或在账号控制中注销账号并删除数据；"
        "收集与使用规则已在软件内的隐私政策与服务条款中说明。",
        "五、责任承担。以上声明内容真实，如与事实不符，本人愿承担由此产生的相应责任。",
    ]
    for text in blocks:
        docxhelp.para(doc, text, size=12, before=3, after=3)
    _sign(doc, cfg)
    doc.save(str(path))
    return path


def naming(cfg, path):
    doc, _ = _doc(cfg, "软件名称说明")
    blocks = [
        f"本软件申请登记的名称为“{cfg['software_name']}”，简称“{cfg.get('short_name', '')}”，"
        "现就名称的含义与由来说明如下：",
        "一、名称构成。“武汉”表明软件服务的城市与内容取材范围；“里份”是武汉对近代成片联排住宅街坊的称呼，"
        "点明软件聚焦的对象是汉口老城区的里份建筑与街巷生活；“智慧旅行导览系统”说明软件的功能定位，"
        "即借助对话问答、路线规划、语音与视频讲解等方式为到访者提供导览服务。",
        "二、名称来源。名称由著作权人根据软件的内容范围与服务方式自行拟定，"
        "在开发过程中即以此名称指代本项目，未沿用任何现有软件、商标或企业名称。",
        "三、不侵权说明。该名称未使用他人注册的商标、商号、产品名，不含“中国”“国家”“国际”等字样，"
        "也未使用可能引人误解的表述；如后续出现名称冲突，著作权人将自行承担相应责任并配合变更。",
    ]
    for text in blocks:
        docxhelp.para(doc, text, size=12, before=3, after=3)
    _sign(doc, cfg)
    doc.save(str(path))
    return path


def write(cfg, work_dir):
    work_dir = Path(work_dir)
    name = f"{cfg['software_name']}{cfg['version']}_"
    files = [
        compliance(cfg, work_dir / f"{name}合法合规及原创性声明（备用）.docx"),
        naming(cfg, work_dir / f"{name}软件名称说明（备用）.docx"),
    ]
    return files
