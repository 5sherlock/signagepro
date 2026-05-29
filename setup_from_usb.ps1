# ============================================================
# SignagePro -- 새 PC 서버 구축 스크립트 v2
# USB에서 setup_from_usb.bat 더블클릭으로 실행 (관리자 권한)
# ============================================================

$ErrorActionPreference = "Continue"

$USB  = $PSScriptRoot
$DEST = "C:\signagepro"

function Write-Step { param($msg) Write-Host "`n[$msg]" -ForegroundColor Cyan }
function Write-Ok   { param($msg) Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn { param($msg) Write-Host "  [!!] $msg" -ForegroundColor Yellow }
function Write-Fail { param($msg) Write-Host "  [XX] $msg" -ForegroundColor Red }

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  SignagePro 서버 구축 스크립트 v2" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ── 0. 관리자 권한 확인 ──────────────────────────────────────
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Fail "관리자 권한으로 실행하세요. (우클릭 → 관리자 권한으로 실행)"
    Read-Host "엔터를 누르면 종료합니다"
    exit 1
}

# ── 1. Node.js 확인 ──────────────────────────────────────────
Write-Step "Node.js 확인"
$nodeVer = node --version 2>$null
if ($nodeVer) {
    Write-Ok "Node.js $nodeVer 설치됨"
} else {
    Write-Warn "Node.js 없음. 다운로드 및 설치 중..."
    $nodeInstaller = "$env:TEMP\node-installer.msi"
    Invoke-WebRequest -Uri "https://nodejs.org/dist/v20.19.0/node-v20.19.0-x64.msi" -OutFile $nodeInstaller -UseBasicParsing
    Start-Process msiexec -ArgumentList "/i `"$nodeInstaller`" /quiet /norestart" -Wait
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    Write-Ok "Node.js 설치 완료"
}

# ── 2. 폴더 생성 ─────────────────────────────────────────────
Write-Step "폴더 구조 생성"
New-Item -ItemType Directory -Path "$DEST\server\prisma"       -Force | Out-Null
New-Item -ItemType Directory -Path "$DEST\server\uploads"      -Force | Out-Null
New-Item -ItemType Directory -Path "$DEST\server\update"       -Force | Out-Null
New-Item -ItemType Directory -Path "$DEST\dashboard\dist"      -Force | Out-Null
Write-Ok "폴더 생성 완료 ($DEST)"

# ── 3. 파일 복사 ─────────────────────────────────────────────
Write-Step "USB → 서버 파일 복사"

# 서버 소스 (node_modules, uploads, update, prisma, .env 제외)
$serverExcludes = @("node_modules","uploads","update","prisma",".env")
Get-ChildItem "$USB\server" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notin $serverExcludes } |
    ForEach-Object { Copy-Item $_.FullName "$DEST\server\" -Recurse -Force }
Write-Ok "서버 소스코드"

# Prisma 스키마 (dev.db 제외)
Get-ChildItem "$USB\server\prisma" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -ne "dev.db" } |
    ForEach-Object { Copy-Item $_.FullName "$DEST\server\prisma\" -Recurse -Force }
Write-Ok "Prisma 스키마"

# 대시보드 빌드 결과물 (dist/)
if (Test-Path "$USB\dashboard\dist") {
    Copy-Item "$USB\dashboard\dist\*" "$DEST\dashboard\dist\" -Recurse -Force
    Write-Ok "대시보드 빌드 파일 (dist/)"
} else {
    Write-Warn "dashboard/dist 없음 — USB에 빌드 파일을 포함시켜야 합니다"
}

# DB 복사 (기존 DB 있으면 덮지 않음)
if (-not (Test-Path "$DEST\server\prisma\dev.db")) {
    if (Test-Path "$USB\server\prisma\dev.db") {
        Copy-Item "$USB\server\prisma\dev.db" "$DEST\server\prisma\dev.db" -Force
        Write-Ok "데이터베이스 (dev.db)"
    } else {
        Write-Warn "dev.db 없음 — 새 DB로 초기화됩니다"
    }
} else {
    Write-Ok "데이터베이스 — 기존 DB 유지 (덮어쓰기 안 함)"
}

# 미디어 파일 (uploads/)
if (Test-Path "$USB\server\uploads") {
    Get-ChildItem "$USB\server\uploads" | ForEach-Object {
        Copy-Item $_.FullName "$DEST\server\uploads\" -Force
    }
    $cnt = (Get-ChildItem "$DEST\server\uploads" -ErrorAction SilentlyContinue).Count
    Write-Ok "미디어 파일 ($cnt 개)"
}

# OTA APK
if (Test-Path "$USB\server\update\app.apk") {
    Copy-Item "$USB\server\update\app.apk" "$DEST\server\update\app.apk" -Force
    Write-Ok "OTA APK"
}

# ── 4. .env 생성 ─────────────────────────────────────────────
Write-Step ".env 환경변수 설정"

# USB의 .env를 참조해 DEVICE_SECRET, ADMIN_PASSWORD 읽기
$usbEnv = @{}
if (Test-Path "$USB\server\.env") {
    Get-Content "$USB\server\.env" | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.*)$') {
            $usbEnv[$Matches[1].Trim()] = $Matches[2].Trim().Trim('"')
        }
    }
}

$deviceSecret = if ($usbEnv["DEVICE_SECRET"]) { $usbEnv["DEVICE_SECRET"] } else { "signagepro-secret-2026" }
$adminPw      = if ($usbEnv["ADMIN_PASSWORD"] -or $usbEnv["adminPassword"]) {
                    $usbEnv["ADMIN_PASSWORD"] ?? $usbEnv["adminPassword"]
                } else { "" }

# 절대경로 DATABASE_URL (따옴표 없이) — dotenvx 호환
$envContent = @"
DATABASE_URL=file:C:/signagepro/server/prisma/dev.db
DEVICE_SECRET="$deviceSecret"
ADMIN_PASSWORD="$adminPw"
"@

Set-Content -Path "$DEST\server\.env" -Value $envContent -Encoding UTF8
Write-Ok ".env 생성 (DATABASE_URL 절대경로)"

# ── 5. npm install (서버만) ───────────────────────────────────
Write-Step "서버 패키지 설치 (npm install)"
Set-Location "$DEST\server"
npm install --silent 2>&1 | Out-Null
Write-Ok "npm install 완료"

# ── 6. Prisma 초기화 ─────────────────────────────────────────
Write-Step "Prisma 초기화"
$env:DATABASE_URL = "file:C:/signagepro/server/prisma/dev.db"
npx prisma generate 2>&1 | Out-Null
npx prisma db push --skip-generate --accept-data-loss 2>&1 | Out-Null
Write-Ok "Prisma DB 초기화 완료"

# ── 7. PM2 설치 및 서버 시작 ─────────────────────────────────
Write-Step "PM2 설치 및 서버 등록"

$pm2Ver = pm2 --version 2>$null | Select-Object -Last 1
if ($pm2Ver) {
    Write-Ok "PM2 $pm2Ver 이미 설치됨"
} else {
    npm install -g pm2 --silent 2>&1 | Out-Null
    Write-Ok "PM2 설치 완료"
}

# 포트 충돌 방지
foreach ($port in @(3300, 10080)) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $conn | Select-Object -ExpandProperty OwningProcess | ForEach-Object {
            Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
        }
    }
}
Start-Sleep -Seconds 1

# 기존 pm2 프로세스 정리 후 재시작
pm2 delete signagepro-server 2>$null | Out-Null
pm2 start "$DEST\server\index.js" --name signagepro-server --cwd "$DEST\server" --restart-delay=3000 2>&1 | Out-Null
pm2 save 2>&1 | Out-Null
Write-Ok "PM2 signagepro-server 등록 완료"

# ── 8. 부팅 자동 시작 (작업 스케줄러) ───────────────────────
Write-Step "부팅 시 자동 시작 등록"

# 배치 파일 생성
$batContent = "@echo off`r`ncd /d C:\signagepro\server`r`npm2 resurrect`r`n"
Set-Content -Path "C:\signagepro\start-server.bat" -Value $batContent -Encoding ASCII

schtasks /delete /tn "SignagePro Server" /f 2>$null | Out-Null
schtasks /create /tn "SignagePro Server" /tr "C:\signagepro\start-server.bat" /sc onstart /ru SYSTEM /f 2>&1 | Out-Null
Write-Ok "작업 스케줄러 등록 완료 (부팅 시 자동 시작)"

# ── 9. Windows 방화벽 규칙 ───────────────────────────────────
Write-Step "Windows 방화벽 포트 개방"

$rules = @(
    @{ Name="SignagePro API";      Port=3300  },
    @{ Name="SignagePro TCP";      Port=10080 }
)
foreach ($r in $rules) {
    netsh advfirewall firewall delete rule name=$r.Name 2>$null | Out-Null
    netsh advfirewall firewall add rule name=$r.Name dir=in action=allow protocol=TCP localport=$r.Port | Out-Null
    Write-Ok "$($r.Name) (TCP $($r.Port)) 개방"
}

# ── 완료 ─────────────────────────────────────────────────────
$localIp = (Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object { $_.IPAddress -notmatch "^127\." -and $_.PrefixOrigin -eq "Dhcp" } |
    Select-Object -First 1).IPAddress

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  구축 완료!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "  대시보드  : http://localhost:3300" -ForegroundColor White
if ($localIp) {
    Write-Host "  이 PC IP  : $localIp" -ForegroundColor White
    Write-Host "  기기 서버 : http://${localIp}:3300" -ForegroundColor Yellow
}
Write-Host ""
Write-Host "  PM2 명령어:" -ForegroundColor Cyan
Write-Host "    pm2 status                    # 상태 확인" -ForegroundColor Gray
Write-Host "    pm2 logs signagepro-server    # 서버 로그" -ForegroundColor Gray
Write-Host "    pm2 restart signagepro-server # 서버 재시작" -ForegroundColor Gray
Write-Host ""
Write-Host "  ※ 공인 IP로 외부 접속 시 공유기 포트포워딩 필요:" -ForegroundColor Yellow
Write-Host "    3300 (HTTP/대시보드), 10080 (기기 TCP) → $localIp" -ForegroundColor Yellow
Write-Host ""
Read-Host "엔터를 누르면 종료합니다"
