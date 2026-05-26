# ============================================================
# SignagePro -- Git commit + USB backup script
# Usage: .\export_usb.ps1
#        .\export_usb.ps1 -UsbPath "E:\" -Message "message"
# ============================================================
param(
    [string]$UsbPath = "",
    [string]$Message = ""
)

$ROOT = $PSScriptRoot
if (-not $ROOT) {
    $ROOT = "c:\Users\amore\WorkSpace\signagepro"
}
Set-Location $ROOT

# 1. Git commit + push
Write-Host ""
Write-Host "[1/3] Git commit & push" -ForegroundColor Cyan

# [NEW] 대상 피처 브랜치로 자동 순간이동 (생성 및 전환 예외 처리)
$targetBranch = "feature/device-delete-offline-ux"
$currentBranch = git rev-parse --abbrev-ref HEAD

if ($currentBranch -ne $targetBranch) {
    Write-Host "  현재 브랜치($currentBranch)에서 대상 피처 브랜치($targetBranch)로 스마트 이동 중..." -ForegroundColor Cyan
    $exists = git branch --list $targetBranch
    if ($exists) {
        git checkout $targetBranch
    } else {
        git checkout -b $targetBranch
    }
    Write-Host "  [OK] 브랜치 전환 완료: $targetBranch" -ForegroundColor Green
}

git add -A
$status = git status --porcelain
if ($status) {
    if (-not $Message) {
        # 변경 파일 기준으로 모듈 자동 감지
        $changedFiles = git diff --cached --name-only
        $mods = @()
        if ($changedFiles -match '^android/') { $mods += 'android' }
        if ($changedFiles -match '^server/')  { $mods += 'server' }
        if ($changedFiles -match '^dashboard/') { $mods += 'dashboard' }
        if ($changedFiles -match '^android_player/') { $mods += 'android_player' }
        if ($mods.Count -eq 0) { $mods += 'etc' }
        $modStr   = $mods -join ' · '
        $date     = Get-Date -Format 'yyyy-MM-dd HH:mm'
        $fileList = ($changedFiles | ForEach-Object { "- $_" }) -join "`n"
        $Message  = "update($modStr): $date`n`n$fileList"
        Write-Host "  자동 생성된 커밋 메시지:" -ForegroundColor DarkGray
        Write-Host $Message -ForegroundColor White
    }
    $branch = git rev-parse --abbrev-ref HEAD
    git commit -m $Message
    git push origin $branch
    Write-Host "  [OK] Git push 완료 (branch: $branch)" -ForegroundColor Green
} else {
    Write-Host "  [--] 변경사항 없음, push 건너뜀" -ForegroundColor Yellow
}

# 2. USB 경로 확인
Write-Host ""
Write-Host "[2/3] USB 드라이브 확인" -ForegroundColor Cyan
if (-not $UsbPath) {
    $removable = @(Get-WmiObject Win32_LogicalDisk | Where-Object { $_.DriveType -eq 2 })
    if ($removable.Count -eq 0) {
        Write-Host "  [ERROR] USB를 찾을 수 없습니다." -ForegroundColor Red
        exit 1
    } elseif ($removable.Count -eq 1) {
        $UsbPath = $removable[0].DeviceID + "\"
        Write-Host "  [OK] USB 감지: $UsbPath" -ForegroundColor Green
    } else {
        Write-Host "  여러 드라이브 감지됨:"
        $removable | ForEach-Object { Write-Host "    $($_.DeviceID) -- $($_.VolumeName)" }
        $UsbPath = Read-Host "  사용할 드라이브 (예: E:\)"
    }
}
if (-not (Test-Path $UsbPath)) {
    Write-Host "  [ERROR] 경로 없음: $UsbPath" -ForegroundColor Red; exit 1
}
$DEST = Join-Path $UsbPath "signagepro_backup"
New-Item -ItemType Directory -Path $DEST -Force | Out-Null
Write-Host "  백업 경로: $DEST"

# 3. 파일 복사
Write-Host ""
Write-Host "[3/3] 파일 복사 중..." -ForegroundColor Cyan

function Copy-Safe([string]$Src, [string]$Dst) {
    if (Test-Path $Src) {
        $dir = Split-Path $Dst -Parent
        if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
        Copy-Item $Src $Dst -Force
        $kb = [math]::Round((Get-Item $Dst).Length / 1KB, 1)
        Write-Host "  [OK] $([System.IO.Path]::GetFileName($Dst)) ($kb KB)"
    } else {
        Write-Host "  [--] 없음: $Src" -ForegroundColor Yellow
    }
}

Copy-Safe "$ROOT\server\prisma\dev.db"      "$DEST\server\prisma\dev.db"
Copy-Safe "$ROOT\server\.env"               "$DEST\server\.env"

Write-Host "  uploads 복사 중..."
if (Test-Path "$ROOT\server\uploads") {
    $ud = "$DEST\server\uploads"
    if (Test-Path $ud) { Remove-Item $ud -Recurse -Force }
    Copy-Item "$ROOT\server\uploads" $ud -Recurse
    $cnt = (Get-ChildItem $ud).Count
    $mb  = [math]::Round((Get-ChildItem $ud | Measure-Object Length -Sum).Sum / 1MB, 1)
    Write-Host "  [OK] uploads ($cnt 개, $mb MB)"
}

Copy-Safe "$ROOT\android\app\signagepro.keystore"                          "$DEST\android\signagepro.keystore"
Copy-Safe "$ROOT\android\app\build\outputs\apk\release\app-release.apk"   "$DEST\android\app-release.apk"
Copy-Safe "$ROOT\server\update\app.apk"                                    "$DEST\server\update\app.apk"

# 새 PC 구축 스크립트 (더블클릭용 .bat 포함)
Copy-Safe "$ROOT\setup_from_usb.ps1" "$DEST\setup_from_usb.ps1"
Copy-Safe "$ROOT\setup_from_usb.bat" "$DEST\setup_from_usb.bat"

# SETUP.md
$secret = ""
if (Test-Path "$ROOT\server\.env") {
    $line = (Get-Content "$ROOT\server\.env") -match "DEVICE_SECRET"
    if ($line) { $secret = ($line -replace 'DEVICE_SECRET\s*=\s*"?([^"]+)"?','$1').Trim() }
}

$sp = "$DEST\SETUP.md"
Set-Content -Path $sp -Value "# SignagePro 새 PC 구축 가이드" -Encoding utf8
Add-Content -Path $sp -Value ("생성: " + (Get-Date -Format 'yyyy-MM-dd HH:mm') + "`n")
Add-Content -Path $sp -Value "---`n"
Add-Content -Path $sp -Value "## 1. 사전 설치"
Add-Content -Path $sp -Value "- Node.js 20+   https://nodejs.org"
Add-Content -Path $sp -Value "- Java JDK 21   https://www.oracle.com/java"
Add-Content -Path $sp -Value "- Android Studio https://developer.android.com/studio"
Add-Content -Path $sp -Value "- Git            https://git-scm.com`n"
Add-Content -Path $sp -Value "## 2. 클론"
Add-Content -Path $sp -Value "    git clone -b dev https://github.com/5sherlock/signagepro.git C:\WorkSpace\signagepro"
Add-Content -Path $sp -Value "    cd C:\WorkSpace\signagepro`n"
Add-Content -Path $sp -Value "## 3. USB에서 파일 복사"
Add-Content -Path $sp -Value "    copy USB:\signagepro_backup\server\prisma\dev.db        server\prisma\dev.db"
Add-Content -Path $sp -Value "    copy USB:\signagepro_backup\server\.env                 server\.env"
Add-Content -Path $sp -Value "    xcopy USB:\signagepro_backup\server\uploads\*           server\uploads\ /E /I /Y"
Add-Content -Path $sp -Value "    xcopy USB:\signagepro_backup\server\update\*            server\update\ /E /I /Y"
Add-Content -Path $sp -Value "    copy USB:\signagepro_backup\android\signagepro.keystore android\app\signagepro.keystore`n"
Add-Content -Path $sp -Value "## 4. 서버 실행"
Add-Content -Path $sp -Value "    cd server"
Add-Content -Path $sp -Value "    npm install"
Add-Content -Path $sp -Value "    npx prisma generate"
Add-Content -Path $sp -Value "    npx prisma db push"
Add-Content -Path $sp -Value "    node index.js`n"
Add-Content -Path $sp -Value "## 5. 대시보드 실행"
Add-Content -Path $sp -Value "    cd dashboard"
Add-Content -Path $sp -Value "    npm install"
Add-Content -Path $sp -Value "    npm run dev`n"
Add-Content -Path $sp -Value "## 6. Android 빌드"
Add-Content -Path $sp -Value "    android\local.properties 생성:"
Add-Content -Path $sp -Value "        sdk.dir=C:\\Users\\<사용자명>\\AppData\\Local\\Android\\Sdk`n"
Add-Content -Path $sp -Value "    android\gradle.properties 에 추가:"
Add-Content -Path $sp -Value "        org.gradle.java.home=C:\\Program Files\\Java\\jdk-21.0.11`n"
Add-Content -Path $sp -Value "    빌드:"
Add-Content -Path $sp -Value "        cd android"
Add-Content -Path $sp -Value "        .\gradlew.bat assembleDebug`n"
Add-Content -Path $sp -Value "## 7. 포트"
Add-Content -Path $sp -Value "    3000  API 서버 + Socket.io"
Add-Content -Path $sp -Value "    5173  대시보드"
Add-Content -Path $sp -Value "    10080 Android TCP 하트비트"
Add-Content -Path $sp -Value "    5555  ADB over TCP`n"
Add-Content -Path $sp -Value "## 8. 기기 설정 정보"
Add-Content -Path $sp -Value ("    Device Secret : " + $secret)
Add-Content -Path $sp -Value "    서버 주소     : http://<이 PC IP>:3000"
Add-Content -Path $sp -Value "    기기 진입     : 우상단 더블탭 -> 관리 메뉴 -> 설정 변경"
Write-Host "  [OK] SETUP.md 생성됨"

# 완료
Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host " 백업 완료: $DEST" -ForegroundColor Green
$total = [math]::Round((Get-ChildItem $DEST -Recurse -File | Measure-Object Length -Sum).Sum / 1MB, 1)
Write-Host " 총 크기  : $total MB" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "백업 파일 목록:" -ForegroundColor Cyan
Get-ChildItem $DEST -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Replace($DEST,"").TrimStart("\")
    $kb  = [math]::Round($_.Length/1KB,1)
    Write-Host ("  " + $rel + " (" + $kb + " KB)")
}
