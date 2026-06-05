# SignagePro DB USB 諛깆뾽 ?ㅽ겕由쏀듃
$DB_PATH = "D:\WorkSpace\signagepro\server\prisma\dev.db"
$UPLOADS_PATH = "D:\WorkSpace\signagepro\server\uploads"
$BACKUP_FOLDER = "SignagePro_Backup"

# USB ?쒕씪?대툕 ?먮룞 ?먯깋 (DriveType=2: ?대룞???붿뒪??
$usb = Get-WmiObject Win32_LogicalDisk | Where-Object { $_.DriveType -eq 2 } | Select-Object -First 1

if (-not $usb) {
    Write-Host "[諛깆뾽 ?ㅽ뙣] USB ?쒕씪?대툕瑜?李얠쓣 ???놁뒿?덈떎." -ForegroundColor Red
    exit 1
}

$usbRoot = $usb.DeviceID + "\"
$backupDir = Join-Path $usbRoot $BACKUP_FOLDER
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# 諛깆뾽 ?대뜑 ?앹꽦
if (-not (Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir | Out-Null
}

# dev.db 諛깆뾽
if (Test-Path $DB_PATH) {
    $dbDest = Join-Path $backupDir "dev_$timestamp.db"
    Copy-Item -Path $DB_PATH -Destination $dbDest
    $sizeMB = [math]::Round((Get-Item $dbDest).Length / 1MB, 2)
    Write-Host "[諛깆뾽 ?꾨즺] DB ??$dbDest ($sizeMB MB)" -ForegroundColor Green
} else {
    Write-Host "[寃쎄퀬] dev.db ?뚯씪???놁뒿?덈떎. 諛깆뾽 嫄대꼫?." -ForegroundColor Yellow
}

# uploads ?대뜑 諛깆뾽
if (Test-Path $UPLOADS_PATH) {
    $uploadsDest = Join-Path $backupDir "uploads_$timestamp"
    Copy-Item -Path $UPLOADS_PATH -Destination $uploadsDest -Recurse
    $count = (Get-ChildItem $uploadsDest -File).Count
    Write-Host "[諛깆뾽 ?꾨즺] uploads ??$uploadsDest ($count 媛??뚯씪)" -ForegroundColor Green
} else {
    Write-Host "[寃쎄퀬] uploads ?대뜑媛 ?놁뒿?덈떎. 諛깆뾽 嫄대꼫?." -ForegroundColor Yellow
}

# ?ㅻ옒??諛깆뾽 ?뺣━ (理쒓렐 10媛쒕쭔 ?좎?)
$oldBackups = Get-ChildItem -Path $backupDir -Filter "dev_*.db" | Sort-Object Name -Descending | Select-Object -Skip 10
foreach ($old in $oldBackups) {
    Remove-Item $old.FullName -Force
    Write-Host "[?뺣━] ?ㅻ옒??諛깆뾽 ??젣: $($old.Name)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "USB ?쒕씪?대툕: $usbRoot" -ForegroundColor Cyan
Write-Host "諛깆뾽 ?꾩튂   : $backupDir" -ForegroundColor Cyan

