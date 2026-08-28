@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ⚡ Building Main Project (FastCalendar)...
call mvn install -DskipTests -q
if %ERRORLEVEL% NEQ 0 ( echo ❌ Main build failed. & pause & exit /b %ERRORLEVEL% )

echo 🛠 Building Benchmark Uber-JAR...
cd examples\Benchmark
call mvn package -DskipTests -q
if %ERRORLEVEL% NEQ 0 ( echo ❌ Benchmark build failed. & cd ..\.. & pause & exit /b %ERRORLEVEL% )

echo 🚀 Running Official JMH Benchmarks for FastCalendar...
java -jar target\benchmarks.jar -jvmArgs "-Xmx4g"

cd ..\..
pause
