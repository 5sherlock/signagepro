@echo off
chcp 65001 >nul
cd /d D:\WorkSpace\signagepro

echo.
echo [1/3] 蹂寃쎌궗???ㅽ뀒?댁쭠...
git add -A

:: 蹂寃쎌궗???놁쑝硫?醫낅즺
git diff --cached --quiet
if %errorlevel%==0 (
    echo 而ㅻ컠??蹂寃쎌궗??씠 ?놁뒿?덈떎.
    pause
    exit /b 0
)

echo [2/3] 而ㅻ컠 硫붿떆吏 ?먮룞 ?앹꽦...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$files = git diff --cached --name-only;" ^
    "$mods = @();" ^
    "if ($files -match '^android/') { $mods += 'android' };" ^
    "if ($files -match '^server/') { $mods += 'server' };" ^
    "if ($files -match '^dashboard/') { $mods += 'dashboard' };" ^
    "if ($files -match '^android_player/') { $mods += 'android_player' };" ^
    "if ($mods.Count -eq 0) { $mods += 'etc' };" ^
    "$modStr = $mods -join ' 쨌 ';" ^
    "$date = Get-Date -Format 'yyyy-MM-dd HH:mm';" ^
    "$fileLines = ($files | ForEach-Object { '- ' + $_ }) -join \"`n\";" ^
    "$msg = \"update($modStr): $date\`n\`n$fileLines\";" ^
    "$msg | Out-File -Encoding utf8 .git\COMMIT_MSG_TEMP;" ^
    "Write-Host '';" ^
    "Write-Host '?앹꽦??而ㅻ컠 硫붿떆吏:' -ForegroundColor Cyan;" ^
    "Write-Host '?????????????????????????????' -ForegroundColor DarkGray;" ^
    "Write-Host $msg -ForegroundColor White;" ^
    "Write-Host '?????????????????????????????' -ForegroundColor DarkGray;"

echo.
set /p CONFIRM="??硫붿떆吏濡?而ㅻ컠?섍쿋?듬땲源? [Y/n]: "
if /i "%CONFIRM%"=="n" (
    git reset HEAD >nul 2>&1
    echo 痍⑥냼?먯뒿?덈떎.
    pause
    exit /b 0
)

echo [3/3] 而ㅻ컠 & ?몄떆...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$msg = Get-Content .git\COMMIT_MSG_TEMP -Raw -Encoding utf8;" ^
    "git commit -m $msg;" ^
    "git push;" ^
    "Remove-Item .git\COMMIT_MSG_TEMP -ErrorAction SilentlyContinue;"

echo.
echo ?꾨즺!
timeout /t 2 /nobreak >nul

