@echo off
setlocal

REM Kiem tra neu mvn co san trong PATH
where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    mvn %*
    exit /b %ERRORLEVEL%
)

REM Fallback den Maven tich hop san cua IntelliJ IDEA neu co
set "IDEA_MAVEN=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
if exist "%IDEA_MAVEN%" (
    call "%IDEA_MAVEN%" %*
    exit /b %ERRORLEVEL%
)

echo [ERROR] Khong tim thay Maven tren may cua ban!
echo Vui long cai dat Maven hoac them thu muc bin cua Maven vao bien moi truong PATH.
exit /b 1
