@echo off
chcp 65001 >nul
title 影院管理系统 - E2E 测试
echo ==========================================
echo   影院管理系统 - E2E Playwright 测试
echo ==========================================
echo.

:: Step 1: 检查前端依赖
if not exist "xm_film\vue\node_modules" (
    echo [1/4] 安装前端依赖...
    cd xm_film\vue
    call npm install
    if %errorlevel% neq 0 (
        echo [错误] 依赖安装失败！
        pause
        exit /b 1
    )
    cd ..\..
) else (
    echo [1/4] 前端依赖已存在
)

:: Step 2: 检查 Playwright 浏览器
echo [2/4] 检查 Playwright 浏览器...
cd xm_film\vue
npx playwright install chromium
cd ..\..

:: Step 3: 检查后端是否运行
echo [3/4] 检查后端服务...
curl -s http://localhost:9090/api/v1/health >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 后端服务 (localhost:9090) 未运行
    echo 请先启动后端服务再进行测试
    echo.
    echo 启动方式: cd xm_film/springboot ^&^& java -jar target/springboot-0.0.1-SNAPSHOT.jar
    echo.
    choice /c YN /m "是否继续测试（仅测试前端渲染，跳过API检查）"
    if errorlevel 2 exit /b 1
)

:: Step 4: 运行测试
echo [4/4] 运行 E2E 测试...
echo.
cd xm_film\vue

:: 4a. 全量 E2E 扫描测试
echo --- E2E 扫描测试 ---
node e2e-tests\e2e-scan.spec.mjs
set SCAN_RESULT=%errorlevel%

:: 4b. API 契约测试（验证前后端接口一致性）
echo.
echo --- API 契约测试 ---
node e2e-tests\api-contract.spec.mjs
set CONTRACT_RESULT=%errorlevel%

cd ..\..

:: 合并结果
if %SCAN_RESULT% equ 0 if %CONTRACT_RESULT% equ 0 (set TEST_RESULT=0) else (set TEST_RESULT=1)

echo.
if %TEST_RESULT% equ 0 (
    echo [成功] 所有测试通过！
) else (
    echo [注意] 部分测试失败，查看报告获取详情
)

echo.
echo 查看 HTML 测试报告:
echo   xm_film\vue\e2e-tests\e2e-scan-report.html
echo.

pause
exit /b %TEST_RESULT%
