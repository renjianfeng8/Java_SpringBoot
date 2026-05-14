@echo off
chcp 65001 >nul
title 影院购票管理系统 - 一键启动
echo ==========================================
echo   影院购票管理系统 - 一键启动
echo ==========================================
echo.

:: 检查端口占用
netstat -ano | findstr ":9090 " >nul 2>&1
if %errorlevel% equ 0 (
    echo [警告] 端口 9090 已被占用，请先关闭占用程序
    echo.
)

netstat -ano | findstr ":5173 " >nul 2>&1
if %errorlevel% equ 0 (
    echo [警告] 端口 5173 已被占用，请先关闭占用程序
    echo.
)

:: Step 1: 检查后端是否已构建
if not exist "xm_film\springboot\target\springboot-0.0.1-SNAPSHOT.jar" (
    echo [1/3] 构建后端...
    cd xm_film\springboot
    call mvn clean package -DskipTests
    if %errorlevel% neq 0 (
        echo [错误] 后端构建失败！
        pause
        exit /b 1
    )
    cd ..\..
) else (
    echo [1/3] 后端已构建，跳过
)

:: Step 2: 检查前端依赖
if not exist "xm_film\vue\node_modules" (
    echo [2/3] 安装前端依赖...
    cd xm_film\vue
    call npm install
    cd ..\..
) else (
    echo [2/3] 前端依赖已安装，跳过
)

:: Step 3: 启动服务
echo.
echo [3/3] 启动服务...
echo.

:: 启动后端（新窗口）
start "后端服务" cmd /c "cd /d %cd%\xm_film\springboot && java -jar target\springboot-0.0.1-SNAPSHOT.jar"

:: 等待后端启动
echo 等待后端启动 (5秒)...
ping -n 6 127.0.0.1 >nul

:: 启动前端（新窗口）
start "前端服务" cmd /c "cd /d %cd%\xm_film\vue && npm run dev"

echo.
echo ==========================================
echo   服务启动中...
echo   后端地址: http://localhost:9090
echo   前端地址: http://localhost:5173
echo.
echo   默认管理员账号: 999 / 999
echo.
echo   按任意键退出...
echo ==========================================
pause

:: 关闭后台进程
taskkill /fi "WINDOWTITLE eq 后端服务" /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq 前端服务" /f >nul 2>&1
