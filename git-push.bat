@echo off
chcp 65001 >nul
cd /d C:\WorkSpace\signagepro

echo.
echo [1/3] 변경사항 스테이징...
git add -A

:: 변경사항 없으면 종료
git diff --cached --quiet
if %errorlevel%==0 (
    echo 커밋할 변경사항이 없습니다.
    pause
    exit /b 0
)

echo [2/3] 커밋 메시지 자동 생성...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$files = git diff --cached --name-only;" ^
    "$mods = @();" ^
    "if ($files -match '^android/') { $mods += 'android' };" ^
    "if ($files -match '^server/') { $mods += 'server' };" ^
    "if ($files -match '^dashboard/') { $mods += 'dashboard' };" ^
    "if ($files -match '^android_player/') { $mods += 'android_player' };" ^
    "if ($mods.Count -eq 0) { $mods += 'etc' };" ^
    "$modStr = $mods -join ' · ';" ^
    "$date = Get-Date -Format 'yyyy-MM-dd HH:mm';" ^
    "$fileLines = ($files | ForEach-Object { '- ' + $_ }) -join \"`n\";" ^
    "$msg = \"update($modStr): $date\`n\`n$fileLines\";" ^
    "$msg | Out-File -Encoding utf8 .git\COMMIT_MSG_TEMP;" ^
    "Write-Host '';" ^
    "Write-Host '생성된 커밋 메시지:' -ForegroundColor Cyan;" ^
    "Write-Host '─────────────────────────────' -ForegroundColor DarkGray;" ^
    "Write-Host $msg -ForegroundColor White;" ^
    "Write-Host '─────────────────────────────' -ForegroundColor DarkGray;"

echo.
set /p CONFIRM="이 메시지로 커밋하겠습니까? [Y/n]: "
if /i "%CONFIRM%"=="n" (
    git reset HEAD >nul 2>&1
    echo 취소됐습니다.
    pause
    exit /b 0
)

echo [3/3] 커밋 & 푸시...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$msg = Get-Content .git\COMMIT_MSG_TEMP -Raw -Encoding utf8;" ^
    "git commit -m $msg;" ^
    "git push;" ^
    "Remove-Item .git\COMMIT_MSG_TEMP -ErrorAction SilentlyContinue;"

echo.
echo 완료!
timeout /t 2 /nobreak >nul
