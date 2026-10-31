@echo off
chcp 65001 >nul
cd /d "%~dp0"

rem 用项目里的虚拟环境跑，没装 python-docx 的机器上会提示
set PY=..\python\.venv\Scripts\python.exe
if not exist "%PY%" (
    echo 没有找到虚拟环境：%PY%
    echo 先在 python 目录下执行  python -m venv .venv  再装依赖：
    echo     .venv\Scripts\python.exe -m pip install python-docx -i https://pypi.tuna.tsinghua.edu.cn/simple
    pause
    exit /b 1
)

"%PY%" main.py
if errorlevel 1 (
    echo.
    echo 生成过程出错，上面的信息可以发给我看。
) else (
    echo.
    echo 材料已经放到 软件著作权申请资料\正式资料\
)
pause
