# SignagePro Server Setup Script
# 관리자 권한으로 실행하세요: 우클릭 → PowerShell로 실행

$ErrorActionPreference = 'Stop'

function Write-Step { param($msg) Write-Host "`n[$msg]" -ForegroundColor Cyan }
function Write-Ok   { param($msg) Write-Host "  ✓ $msg" -ForegroundColor Green }
function Write-Warn { param($msg) Write-Host "  ! $msg" -ForegroundColor Yellow }
function Write-Fail { param($msg) Write-Host "  ✗ $msg" -ForegroundColor Red }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host ""
Write-Host "==========================================" -ForegroundColor Blue
Write-Host "   SignagePro 서버 자동 설치 스크립트" -ForegroundColor Blue
Write-Host "==========================================" -ForegroundColor Blue

# ── 1. Node.js 확인 ──────────────────────────────────────────────────
Write-Step "Node.js 확인"
try {
    $nodeVer = node --version 2>$null
    Write-Ok "Node.js $nodeVer 설치됨"
} catch {
    Write-Warn "Node.js가 없습니다. 설치를 시작합니다..."
    $nodeInstaller = "$env:TEMP\node-installer.msi"
    Write-Host "  다운로드 중..." -NoNewline
    Invoke-WebRequest -Uri "https://nodejs.org/dist/v20.19.0/node-v20.19.0-x64.msi" -OutFile $nodeInstaller -UseBasicParsing
    Write-Host " 완료"
    Start-Process msiexec -ArgumentList "/i `"$nodeInstaller`" /quiet /norestart" -Wait
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    Write-Ok "Node.js 설치 완료"
}

# ── 2. ADB 확인 ──────────────────────────────────────────────────────
Write-Step "ADB (Android Debug Bridge) 확인"
$adbFound = $false
$adbLocations = @(
    "adb",
    "C:\adb\adb.exe",
    "C:\platform-tools\adb.exe",
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    "$env:ProgramFiles\Android\android-sdk\platform-tools\adb.exe"
)
foreach ($loc in $adbLocations) {
    try {
        $ver = & $loc version 2>$null | Select-Object -First 1
        if ($ver -match "Android Debug Bridge") {
            Write-Ok "ADB 발견: $loc"
            $adbFound = $true
            break
        }
    } catch {}
}

if (-not $adbFound) {
    Write-Warn "ADB를 찾을 수 없습니다."
    Write-Host "  다운로드 중..." -NoNewline
    $ptZip = "$env:TEMP\platform-tools.zip"
    Invoke-WebRequest -Uri "https://dl.google.com/android/repository/platform-tools-latest-windows.zip" -OutFile $ptZip -UseBasicParsing
    Write-Host " 완료"
    Expand-Archive -Path $ptZip -DestinationPath "C:\" -Force
    $adbDir = "C:\platform-tools"
    $currentPath = [System.Environment]::GetEnvironmentVariable("Path","User")
    if ($currentPath -notlike "*platform-tools*") {
        [System.Environment]::SetEnvironmentVariable("Path", "$currentPath;$adbDir", "User")
        $env:Path += ";$adbDir"
    }
    Write-Ok "ADB 설치 완료 (C:\platform-tools)"
}

# ── 3. .env 파일 설정 ────────────────────────────────────────────────
Write-Step ".env 설정"
$envFile = Join-Path $ScriptDir ".env"
if (Test-Path $envFile) {
    Write-Ok ".env 파일 이미 존재 — 기존 설정 유지"
} else {
    Write-Host ""
    Write-Host "  서버 설정을 입력하세요:" -ForegroundColor White
    $dbPath = Read-Host "  DB 경로 (엔터=기본값: ./prisma/signage.db)"
    if ([string]::IsNullOrWhiteSpace($dbPath)) { $dbPath = "./prisma/signage.db" }
    $envContent = "DATABASE_URL=`"file:$dbPath`"`n"
    Set-Content -Path $envFile -Value $envContent -Encoding UTF8
    Write-Ok ".env 파일 생성 완료"
}

# ── 4. npm 의존성 설치 ───────────────────────────────────────────────
Write-Step "npm 패키지 설치"
Set-Location $ScriptDir
npm install --silent
Write-Ok "패키지 설치 완료"

# ── 5. Prisma DB 초기화 ──────────────────────────────────────────────
Write-Step "데이터베이스 초기화"
npx prisma db push --accept-data-loss 2>&1 | Out-Null
Write-Ok "DB 스키마 적용 완료"

# ── 6. PM2 설치 및 서버 등록 ─────────────────────────────────────────
Write-Step "PM2 프로세스 매니저 설치"
try {
    $pm2Ver = pm2 --version 2>$null | Select-Object -Last 1
    Write-Ok "PM2 $pm2Ver 이미 설치됨"
} catch {
    npm install -g pm2 --silent
    Write-Ok "PM2 설치 완료"
}

Write-Step "서버 PM2 등록"
# 기존 프로세스 중지
pm2 delete signagepro 2>$null | Out-Null

# 포트 3000 충돌 방지
$portInUse = Get-NetTCPConnection -LocalPort 3000 -State Listen -ErrorAction SilentlyContinue
if ($portInUse) {
    $portInUse | Select-Object -ExpandProperty OwningProcess | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 1
}

pm2 start "$ScriptDir\index.js" --name signagepro --restart-delay=3000 2>&1 | Out-Null
pm2 save 2>&1 | Out-Null
Write-Ok "PM2에 signagepro 등록 완료"

# ── 7. Windows 시작 프로그램 등록 ────────────────────────────────────
Write-Step "Windows 시작 시 자동 실행 등록"
$startupFolder = [System.Environment]::GetFolderPath('Startup')
$batPath = "$startupFolder\signagepro-pm2.bat"
$batContent = "@echo off`r`npm2 resurrect`r`n"
Set-Content -Path $batPath -Value $batContent -Encoding ASCII
Write-Ok "시작 프로그램 등록 완료: $batPath"

# ── 8. ADB 기기 연결 ─────────────────────────────────────────────────
Write-Step "현장 기기 ADB 연결"
$deviceIPs = @("192.168.0.76", "192.168.0.73", "192.168.0.75")
foreach ($ip in $deviceIPs) {
    $result = adb connect "${ip}:5555" 2>&1
    if ($result -match "connected") {
        Write-Ok "$ip 연결됨"
    } else {
        Write-Warn "$ip 연결 실패 (ADB TCP 미활성 또는 네트워크 문제)"
    }
}

# ── 완료 ─────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "   설치 완료!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  서버 주소 : http://localhost:3000" -ForegroundColor White
Write-Host "  대시보드  : http://localhost:3000  (브라우저에서 열기)" -ForegroundColor White
Write-Host ""
Write-Host "  PM2 명령어:" -ForegroundColor White
Write-Host "    pm2 status              # 상태 확인" -ForegroundColor Gray
Write-Host "    pm2 logs signagepro     # 로그 보기" -ForegroundColor Gray
Write-Host "    pm2 restart signagepro  # 재시작" -ForegroundColor Gray
Write-Host ""
Write-Host "  ※ 서버 IP가 바뀐 경우 현장 기기에서" -ForegroundColor Yellow
Write-Host "    우상단 2번 클릭 → 설정 변경 → 서버 URL 수정 필요" -ForegroundColor Yellow
Write-Host ""
Read-Host "엔터를 누르면 종료합니다"
