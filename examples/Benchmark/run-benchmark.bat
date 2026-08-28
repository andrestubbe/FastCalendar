@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo 🛠 Building Benchmark Uber-JAR...
call mvn package -DskipTests -q
if %ERRORLEVEL% NEQ 0 ( echo ❌ Benchmark build failed. & pause & exit /b %ERRORLEVEL% )

echo 🚀 Running Official JMH Benchmarks for FastCalendar...
java -jar target\benchmarks.jar -jvmArgs "-Xmx4g"
pause
