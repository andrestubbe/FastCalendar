@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ⚡ Building Main FastCalendar Project...
call mvn clean install -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo 🚀 Running FastCalendar Hero Demo...
call mvn -f examples/Demo/pom.xml compile -q
call java -cp "target\FastCalendar-0.1.0.jar;examples\Demo\target\classes" fastcalendar.demo.Demo
pause
