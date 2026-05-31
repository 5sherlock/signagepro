# SignagePro - 원격 서버 안전 자동 백업 & 배포 통합 스크립트
# 사용법: PowerShell을 관리자 권한으로 실행한 뒤 .\deploy_server.ps1 실행

$ErrorActionPreference = "Stop"

# ── 1. 경로 및 설정 정의 ──────────────────────────────────────────
$ProjectRoot = "C:\signagepro"
$BackupDir = "C:\signagepro_backups"
$DateStr = Get-Date -Format "yyyyMMdd_HHmmss"

$DbSource = "$ProjectRoot\server\prisma\dev.db"
$DbBackupTarget = "$BackupDir\dev_$DateStr.db"

$UploadsSource = "$ProjectRoot\server\uploads"
$UploadsBackupTarget = "$BackupDir\uploads_$DateStr"

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "🚀 SignagePro 원격 서버 안전 업데이트 & 자동 백업 시작" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# ── 2. 백업 디렉토리 보장 및 백업 실행 ─────────────────────────────
if (!(Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
    Write-Host "📂 백업 폴더가 존재하지 않아 새로 생성했습니다: $BackupDir" -ForegroundColor Yellow
}

Write-Host "⏱️ 1. 현재 시각 데이터 백업 작업을 시작합니다..." -ForegroundColor Green

# SQLite DB 백업
if (Test-Path $DbSource) {
    Copy-Item $DbSource $DbBackupTarget -Force
    Write-Host "  ✅ DB 백업 완료: $DbBackupTarget" -ForegroundColor Green
} else {
    Write-Host "  ⚠️ 백업할 원본 DB 파일이 없습니다. (신규 설치 상태 혹은 무시)" -ForegroundColor Yellow
}

# Uploads 미디어 폴더 백업
if (Test-Path $UploadsSource) {
    # 폴더 통째로 복사
    Copy-Item $UploadsSource $UploadsBackupTarget -Recurse -Force
    Write-Host "  ✅ 미디어 업로드 폴더 백업 완료: $UploadsBackupTarget" -ForegroundColor Green
} else {
    Write-Host "  ⚠️ 백업할 미디어 폴더가 존재하지 않습니다." -ForegroundColor Yellow
}

# ── 3. 노드 프로세스 강제 종료 (DB 파일 잠금 해제) ─────────────────
Write-Host "`n⏱️ 2. 안전한 Git 동기화를 위해 노드 서버 및 PM2 데몬을 종료합니다..." -ForegroundColor Green

try {
    # PM2 서비스 일시 정지 및 데몬 완전 킬
    & pm2 stop signagepro-server 2>$null | Out-Null
    & pm2 kill 2>$null | Out-Null
    Write-Host "  ✅ PM2 프로세스 중지 성공" -ForegroundColor Green
} catch {}

# 실행 중인 모든 node 프로세스 강제 종료 (SQLite Lock 원천 예방)
$NodeProcesses = Get-Process -Name "node" -ErrorAction SilentlyContinue
if ($NodeProcesses) {
    $NodeProcesses | Stop-Process -Force
    Write-Host "  ✅ 실행 중이던 잔여 Node.js 프로세스 강제 종료 완료" -ForegroundColor Green
} else {
    Write-Host "  ✅ 가동 중인 잔여 Node 프로세스 없음 (안전)" -ForegroundColor Green
}

Start-Sleep -Seconds 2

# ── 4. Git 코드 동기화 ─────────────────────────────────────────────
Write-Host "`n⏱️ 3. 깃허브 원격 저장소에서 최신 코드를 안전하게 내려받습니다..." -ForegroundColor Green
Set-Location $ProjectRoot

# 로컬 수정 사항이 꼬여서 충돌 나는 것을 방지하기 위해 리셋 후 Pull
& git reset --hard
& git pull origin release/0.4.5
Write-Host "  ✅ 최신 소스 코드 동기화 완료" -ForegroundColor Green

# ── 5. 대시보드 빌드 검증 및 갱신 ──────────────────────────────────
Write-Host "`n⏱️ 4. 대시보드 프론트엔드 최신 리소스를 빌드합니다..." -ForegroundColor Green
Set-Location "$ProjectRoot\dashboard"

# 의존성 설치 및 프로덕션 빌드 실행
& npm install
& npm run build
Write-Host "  ✅ 대시보드 컴파일 빌드 완료" -ForegroundColor Green

# ── 6. 서버 재기동 (PM2) ───────────────────────────────────────────
Write-Host "`n⏱️ 5. 복구된 데이터와 소스로 서비스를 다시 구동합니다..." -ForegroundColor Green
Set-Location "$ProjectRoot\server"

# 단일 프로세스로 PM2 등록 및 시작
& pm2 start index.js --name "signagepro-server" --interpreter "C:\Users\Administrator\AppData\Local\nvm\v20.19.0\node.exe"
& pm2 save

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "🎉 모든 백업, 소스 갱신 및 서비스 기동 작업이 성공적으로 완수되었습니다!" -ForegroundColor Green
Write-Host "  - 백업 보존 위치: $BackupDir" -ForegroundColor Yellow
Write-Host "=============================================" -ForegroundColor Cyan
