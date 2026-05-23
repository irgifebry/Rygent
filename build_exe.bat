@echo off
setlocal
echo ==============================================
echo Rygent Agent Windows Builder (.exe)
echo ==============================================
echo [1/3] Checking dependencies...

REM Check if Python is installed
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python not found. Please install Python 3.10+ first.
    pause
    exit /b
)

echo [2/3] Installing/Updating required Python packages...
pip install -r requirements.txt
pip install pyinstaller

echo [3/3] Building single .exe file...
echo This may take a minute or two...
pyinstaller --onefile --windowed --icon=icon.png --name=RygentAgent --add-data "static;static" --add-data "templates;templates" --add-data "icon.png;." agent_gui.py

if %errorlevel% equ 0 (
    echo ==============================================
    echo [SUCCESS] RygentAgent.exe created in the 'dist' folder!
    echo ==============================================
) else (
    echo ==============================================
    echo [ERROR] Build failed. Check the logs above.
    echo ==============================================
)

pause
