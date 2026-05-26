# ============================================================
# SignagePro -- 새 PC 서버 구축 스크립트
# USB의 signagepro_backup 폴더 안에서 실행:
#   PowerShell -ExecutionPolicy Bypass -File .\setup_from_usb.ps1
# ============================================================

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  SignagePro 새 PC 구축 스크립트" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ── 0. 경로 설정 ─────────────────────────────────────────────
$USB   = $PSScriptRoot                        # 이 스크립트가 있는 폴더 (USB)
$DEST  = "C:\WorkSpace\signagepro"            # 설치 대상 경로

# ── 1. Git 저장소 클론 확인 ──────────────────────────────────
Write-Host "[Step 1] 프로젝트 폴더 확인" -ForegroundColor Cyan

if (-not (Test-Path "$DEST\.git")) {
    Write-Host "  저장소가 없습니다. Git 클론을 먼저 하세요." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  git clone https://github.com/5sherlock/signagepro.git C:\WorkSpace\signagepro" -ForegroundColor White
    Write-Host ""
    $ans = Read-Host "  클론 완료 후 Enter, 또는 S 입력 시 건너뜀"
    if ($ans -ne "S" -and $ans -ne "s") {
        if (-not (Test-Path "$DEST\.git")) {
            Write-Host "  [ERROR] 폴더를 찾을 수 없습니다: $DEST" -ForegroundColor Red
            exit 1
        }
    }
} else {
    Write-Host "  [OK] 저장소 확인됨: $DEST" -ForegroundColor Green
}

New-Item -ItemType Directory -Path "$DEST\server\prisma"  -Force | Out-Null
New-Item -ItemType Directory -Path "$DEST\server\uploads" -Force | Out-Null
New-Item -ItemType Directory -Path "$DEST\server\update"  -Force | Out-Null
New-Item -ItemType Directory -Path "$DEST\android\app"    -Force | Out-Null

# ── 2. USB → 프로젝트 파일 복사 ─────────────────────────────
Write-Host ""
Write-Host "[Step 2] USB에서 파일 복사" -ForegroundColor Cyan

function Copy-Safe([string]$Src, [string]$Dst, [string]$Label = "") {
    $name = if ($Label) { $Label } else { Split-Path $Src -Leaf }
    if (Test-Path $Src) {
        $dir = Split-Path $Dst -Parent
        if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
        Copy-Item $Src $Dst -Force -Recurse
        Write-Host "  [OK] $name" -ForegroundColor Green
    } else {
        Write-Host "  [--] 없음 (건너뜀): $name" -ForegroundColor Yellow
    }
}

# DB
Copy-Safe "$USB\server\prisma\dev.db"   "$DEST\server\prisma\dev.db"   "dev.db (데이터베이스)"

# 환경변수
Copy-Safe "$USB\server\.env"            "$DEST\server\.env"             ".env (환경변수)"

# 미디어 파일
Write-Host "  uploads 복사 중 (시간이 걸릴 수 있습니다)..." -NoNewline
if (Test-Path "$USB\server\uploads") {
    $ud = "$DEST\server\uploads"
    Get-ChildItem "$USB\server\uploads" | ForEach-Object {
        Copy-Item $_.FullName $ud -Force
    }
    $cnt = (Get-ChildItem $ud).Count
    $mb  = [math]::Round((Get-ChildItem $ud | Measure-Object Length -Sum).Sum / 1MB, 1)
    Write-Host "`r  [OK] uploads ($cnt 개, $mb MB)          " -ForegroundColor Green
}

# OTA APK
Copy-Safe "$USB\server\update\app.apk"  "$DEST\server\update\app.apk"  "app.apk (OTA 배포용)"

# 서명 키
Copy-Safe "$USB\android\signagepro.keystore" "$DEST\android\app\signagepro.keystore" "signagepro.keystore (서명 키)"

# ── 3. Android 빌드 환경 설정 ────────────────────────────────
Write-Host ""
Write-Host "[Step 3] Android 빌드 환경 설정" -ForegroundColor Cyan

# local.properties — Android SDK 경로 (PC마다 다를 수 있음)
$sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
if (Test-Path $sdkPath) {
    $escaped = $sdkPath.Replace("\", "\\")
    "sdk.dir=$escaped" | Out-File "$DEST\android\local.properties" -Encoding ascii
    Write-Host "  [OK] local.properties (sdk.dir=$sdkPath)" -ForegroundColor Green
} else {
    Write-Host "  [!!] Android SDK를 찾을 수 없습니다." -ForegroundColor Yellow
    Write-Host "       Android Studio 설치 후 아래 파일을 직접 작성하세요:" -ForegroundColor Yellow
    Write-Host "       $DEST\android\local.properties" -ForegroundColor White
    Write-Host "       내용: sdk.dir=C:\\Users\\<사용자명>\\AppData\\Local\\Android\\Sdk" -ForegroundColor White
}

# gradle.properties — Java 경로
$javaPath = ""
@(
    "C:\Program Files\Java\jdk-21.0.11",
    "C:\Program Files\Eclipse Adoptium\jdk-21*",
    "C:\Program Files\Microsoft\jdk-21*"
) | ForEach-Object {
    if (-not $javaPath) {
        $found = Get-Item $_ -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { $javaPath = $found.FullName }
    }
}

$gradleProps = "$DEST\android\gradle.properties"
if ($javaPath) {
    $content = Get-Content $gradleProps -Raw -ErrorAction SilentlyContinue
    $escaped  = $javaPath.Replace("\", "\\")
    if ($content -notmatch "org.gradle.java.home") {
        "`norg.gradle.java.home=$escaped" | Add-Content $gradleProps -Encoding utf8
    }
    Write-Host "  [OK] gradle.properties (java.home=$javaPath)" -ForegroundColor Green
} else {
    Write-Host "  [!!] Java 21을 찾을 수 없습니다." -ForegroundColor Yellow
    Write-Host "       https://www.oracle.com/java 에서 JDK 21 설치 후" -ForegroundColor Yellow
    Write-Host "       android\gradle.properties 에 추가:" -ForegroundColor Yellow
    Write-Host "       org.gradle.java.home=C:\\Program Files\\Java\\jdk-21.0.11" -ForegroundColor White
}

# ── 4. 서버 npm install + DB 초기화 ─────────────────────────
Write-Host ""
Write-Host "[Step 4] 서버 패키지 설치 및 DB 초기화" -ForegroundColor Cyan

Set-Location "$DEST\server"

Write-Host "  npm install 중..."
npm install --silent
Write-Host "  [OK] npm install 완료" -ForegroundColor Green

Write-Host "  prisma generate 중..."
npx prisma generate --silent 2>$null
Write-Host "  [OK] prisma generate 완료" -ForegroundColor Green

Write-Host "  prisma db push 중..."
npx prisma db push --skip-generate 2>&1 | Out-Null
Write-Host "  [OK] DB 초기화 완료" -ForegroundColor Green

# ── 5. 대시보드 npm install ──────────────────────────────────
Write-Host ""
Write-Host "[Step 5] 대시보드 패키지 설치" -ForegroundColor Cyan

Set-Location "$DEST\dashboard"
Write-Host "  npm install 중..."
npm install --silent
Write-Host "  [OK] npm install 완료" -ForegroundColor Green

# ── 6. PM2 설치 + 서버 시작 ─────────────────────────────────
Write-Host ""
Write-Host "[Step 6] PM2 설치 및 서버 시작" -ForegroundColor Cyan

Set-Location "$DEST"

$pm2 = Get-Command pm2 -ErrorAction SilentlyContinue
if (-not $pm2) {
    Write-Host "  PM2 설치 중..."
    npm install -g pm2 --silent
    Write-Host "  [OK] PM2 설치 완료" -ForegroundColor Green
} else {
    Write-Host "  [OK] PM2 이미 설치됨" -ForegroundColor Green
}

# 기존 프로세스 정리 후 재시작
pm2 delete signagepro 2>$null
pm2 delete dashboard  2>$null

pm2 start "$DEST\server\index.js" --name signagepro --cwd "$DEST\server"
pm2 start node --name dashboard -- "$DEST\dashboard\node_modules\vite\bin\vite.js" --cwd "$DEST\dashboard"
pm2 save

Write-Host "  [OK] 서버 시작됨" -ForegroundColor Green

# ── 완료 ─────────────────────────────────────────────────────
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  구축 완료!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

# 이 PC의 IP 주소 출력
$ip = (Get-NetIPAddress -AddressFamily IPv4 |
       Where-Object { $_.IPAddress -notmatch "^127\." -and $_.PrefixOrigin -eq "Dhcp" } |
       Select-Object -First 1).IPAddress

Write-Host "  대시보드  : http://localhost:5173" -ForegroundColor White
Write-Host "  API 서버  : http://localhost:3000" -ForegroundColor White
if ($ip) {
    Write-Host "  이 PC IP  : $ip" -ForegroundColor White
    Write-Host "  기기 서버 : http://${ip}:3000" -ForegroundColor Yellow
}
Write-Host ""
Write-Host "  Android 빌드가 필요하면:" -ForegroundColor Cyan
Write-Host "    cd $DEST\android" -ForegroundColor White
Write-Host "    .\gradlew.bat assembleDebug" -ForegroundColor White
Write-Host ""
