@echo off
chcp 65001 >nul
cd /d "%~dp0\.."

echo ⚡ Initializing Git Repository...
if not exist .git (
    git init
    git branch -M main
)

git add .
git commit -m "feat: Initial release of FastCalendar 0.1.0"

echo 🌐 Creating GitHub Repository and Pushing...
gh repo create andrestubbe/FastCalendar --public --source=. --push
if %ERRORLEVEL% NEQ 0 (
    echo [Info] Repo might already exist, pushing directly...
    git push -u origin main
)

echo 🏷 Creating Tag 0.1.0...
git tag 0.1.0
git push origin 0.1.0

echo 🚀 Creating GitHub Release 0.1.0...
gh release create 0.1.0 --title "FastCalendar 0.1.0" --notes "Initial release of FastCalendar: Ultra-Fast, Zero-Allocation iCalendar (RFC 5545), CalDAV (RFC 4791), and RRULE Recurrence Engine for Java."

echo ✔ Release complete!
pause
