@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ⚡ Compiling FastCalendar & Running Tests...
call mvn clean test
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Test suite failed!
    pause
    exit /b %ERRORLEVEL%
)

echo 📦 Packaging FastCalendar JAR...
call mvn package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Package failed!
    pause
    exit /b %ERRORLEVEL%
)

echo ✔ FastCalendar compiled and packaged successfully!
pause
