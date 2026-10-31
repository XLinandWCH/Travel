# -*- coding: utf-8 -*-
"""挑代码材料用的文件。顺序按功能模块排，审核的人顺着读能看懂软件在做什么。"""

import hashlib

# 靠前的关键字排在前面，决定代码材料里的出现顺序
ORDER = [
    "App.kt", "AppHost.kt", "AppNavigation.kt", "AppChatRoutes.kt",
    "chat_screen", "inputbox", "agent", "routeplan", "map", "miniprogram",
    "videoexplain", "setting", "search", "drawer", "login", "guest",
    "db", "tts", "image_picker", "image_processor", "markdown", "a2ui",
    "color", "server", "core",
]

REASON = {
    "chat_screen": "对话主界面与消息渲染逻辑",
    "inputbox": "输入框、附件与语音输入交互",
    "agent": "AI 问答链路与工具调用实现",
    "routeplan": "路线规划与地图展示",
    "map": "位置服务与地图参数管理",
    "miniprogram": "小程序快捷面板",
    "videoexplain": "摄像头视频讲解流程",
    "setting": "设置、消息中心与反馈管理",
    "search": "历史消息检索",
    "drawer": "历史会话抽屉",
    "login": "账号注册登录与会话保持",
    "guest": "游客访问入口",
    "db": "本地会话记录持久化",
    "tts": "语音播报",
    "image_picker": "图片与文档选取",
    "image_processor": "图片压缩与格式转换",
    "markdown": "回答内容排版与公式渲染",
    "a2ui": "个性化界面组件渲染",
    "color": "主题配色与界面风格",
    "server": "后台服务与接口实现",
    "core": "公共基础代码",
}

SKIP_DIRS = {"commonTest", "androidHostTest", "jvmTest", "iosTest", "test", "build"}
SKIP_FILES = {"Greeting.kt", "Platform.kt", "ToolDisplayNames.kt"}


def _rank(rel):
    name = rel.rsplit("/", 1)[-1]
    if name in ("App.kt", "AppHost.kt", "AppNavigation.kt", "AppChatRoutes.kt"):
        return -1
    parts = rel.split("/")
    best = 99
    for idx, key in enumerate(ORDER):
        if key in parts:
            best = min(best, idx)
    return best


def _reason(rel):
    parts = rel.split("/")
    hits = [k for k in ORDER if k in parts]
    if hits:
        return REASON.get(hits[0], "核心业务代码")
    name = rel.rsplit("/", 1)[-1]
    if name in ("App.kt", "AppHost.kt", "AppNavigation.kt", "AppChatRoutes.kt"):
        return "应用入口与页面路由"
    return "支撑性业务代码"


def _keep(item):
    rel = item["rel"]
    parts = rel.split("/")[:-1]
    if any(p in SKIP_DIRS for p in parts):
        return False
    name = rel.rsplit("/", 1)[-1]
    if name in SKIP_FILES:
        return False
    if item["lines"] < 15:
        return False
    if item["ext"] not in (".kt", ".kts", ".swift", ".java", ".sql", ".js", ".ts"):
        return False
    return True


def choose(cfg, files, need_lines):
    """先按模块顺序排好，凑够材料所需行数就停，避免把整个工程塞进去。"""
    picked, total = [], 0
    for item in sorted([f for f in files if _keep(f)], key=lambda f: (_rank(f["rel"]), f["rel"])):
        if total >= need_lines:
            break
        picked.append({
            "rel": item["rel"], "path": item["path"], "lang": item["lang"],
            "lines": item["lines"], "reason": _reason(item["rel"]),
        })
        total += item["lines"]
    return picked


def listing_order(picked):
    """代码材料里的文件顺序尽量固定，重跑两次结果一致。"""
    return sorted(picked, key=lambda f: (_rank(f["rel"]), hashlib.md5(f["rel"].encode()).hexdigest()))
