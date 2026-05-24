# SignagePro DB USB 백업 스크립트
$DB_PATH = "C:\WorkSpace\signagepro\server\prisma\dev.db"
$UPLOADS_PATH = "C:\WorkSpace\signagepro\server\uploads"
$BACKUP_FOLDER = "SignagePro_Backup"

# USB 드라이브 자동 탐색 (DriveType=2: 이동식 디스크)
$usb = Get-WmiObject Win32_LogicalDisk | Where-Object { $_.DriveType -eq 2 } | Select-Object -First 1

if (-not $usb) {
    Write-Host "[백업 실패] USB 드라이브를 찾을 수 없습니다." -ForegroundColor Red
    exit 1
}

$usbRoot = $usb.DeviceID + "\"
$backupDir = Join-Path $usbRoot $BACKUP_FOLDER
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# 백업 폴더 생성
if (-not (Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir | Out-Null
}

# dev.db 백업
if (Test-Path $DB_PATH) {
    $dbDest = Join-Path $backupDir "dev_$timestamp.db"
    Copy-Item -Path $DB_PATH -Destination $dbDest
    $sizeMB = [math]::Round((Get-Item $dbDest).Length / 1MB, 2)
    Write-Host "[백업 완료] DB → $dbDest ($sizeMB MB)" -ForegroundColor Green
} else {
    Write-Host "[경고] dev.db 파일이 없습니다. 백업 건너뜀." -ForegroundColor Yellow
}

# uploads 폴더 백업
if (Test-Path $UPLOADS_PATH) {
    $uploadsDest = Join-Path $backupDir "uploads_$timestamp"
    Copy-Item -Path $UPLOADS_PATH -Destination $uploadsDest -Recurse
    $count = (Get-ChildItem $uploadsDest -File).Count
    Write-Host "[백업 완료] uploads → $uploadsDest ($count 개 파일)" -ForegroundColor Green
} else {
    Write-Host "[경고] uploads 폴더가 없습니다. 백업 건너뜀." -ForegroundColor Yellow
}

# 오래된 백업 정리 (최근 10개만 유지)
$oldBackups = Get-ChildItem -Path $backupDir -Filter "dev_*.db" | Sort-Object Name -Descending | Select-Object -Skip 10
foreach ($old in $oldBackups) {
    Remove-Item $old.FullName -Force
    Write-Host "[정리] 오래된 백업 삭제: $($old.Name)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "USB 드라이브: $usbRoot" -ForegroundColor Cyan
Write-Host "백업 위치   : $backupDir" -ForegroundColor Cyan
