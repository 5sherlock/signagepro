Set-Location D:\WorkSpace\signagepro

# 1. check if dashboard/src changed -> needs build
$changed    = git diff --name-only HEAD
$staged     = git diff --cached --name-only
$allChanged = (@($changed) + @($staged)) | Select-Object -Unique
$needsBuild = $allChanged | Where-Object { $_ -like "dashboard/src/*" }

if ($needsBuild) {
    Write-Host "[build] dashboard/src changed -> building..." -ForegroundColor Yellow
    Set-Location D:\WorkSpace\signagepro\dashboard
    npm run build
    if ($LASTEXITCODE -ne 0) { Write-Host "[error] build failed" -ForegroundColor Red; exit 1 }
    Write-Host "[build] done" -ForegroundColor Green
    Set-Location D:\WorkSpace\signagepro
}

# 2. stage all
git add -A

# 3. nothing to commit?
git diff --cached --quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "nothing to commit" -ForegroundColor DarkGray
    exit 0
}

# 4. auto-generate commit message
$files = git diff --cached --name-only
$mods  = @()
if ($files -match "^android/")        { $mods += "android" }
if ($files -match "^server/")         { $mods += "server" }
if ($files -match "^dashboard/")      { $mods += "dashboard" }
if ($files -match "^android_player/") { $mods += "android_player" }
if ($mods.Count -eq 0)                { $mods += "etc" }

$modStr    = $mods -join " + "
$date      = Get-Date -Format "yyyy-MM-dd HH:mm"
$fileLines = ($files | ForEach-Object { "  - $_" }) -join "`n"
$msg       = "update($modStr): $date`n`n$fileLines"

Write-Host ""
Write-Host "-----------------------------" -ForegroundColor DarkGray
Write-Host $msg -ForegroundColor White
Write-Host "-----------------------------" -ForegroundColor DarkGray
Write-Host ""

# 5. confirm
$confirm = Read-Host "commit with this message? [Y/n]"
if ($confirm -ieq "n") {
    git reset HEAD | Out-Null
    Write-Host "cancelled" -ForegroundColor Yellow
    exit 0
}

# 6. commit & push
git commit -m $msg
git push origin dev

Write-Host ""
Write-Host "[done] pushed to dev" -ForegroundColor Green
