# ─────────────────────────────────────────────────────────────────────────────
# build_apk.ps1  — SignagePro Android APK 빌드 + 서버 배포 폴더 자동 복사
#
# 사용법: .\build_apk.ps1
#   1. Release APK 빌드 (android/app/build/outputs/apk/release/app-release.apk)
#   2. server/update/app.apk 로 자동 복사 → 대시보드에서 바로 OTA 배포 가능
# ─────────────────────────────────────────────────────────────────────────────

$ROOT    = "C:\Users\amore\WorkSpace\signagepro"
$ANDROID = "$ROOT\android"
$APK_SRC = "$ANDROID\app\build\outputs\apk\release\app-release.apk"
$APK_DST = "$ROOT\server\update\app.apk"

Set-Location $ANDROID

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "  SignagePro APK 빌드 시작" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

# 빌드
.\gradlew assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "❌ 빌드 실패 (exit code $LASTEXITCODE)" -ForegroundColor Red
    exit 1
}

# 빌드 출력 확인
if (-not (Test-Path $APK_SRC)) {
    Write-Host "❌ APK 파일이 없습니다: $APK_SRC" -ForegroundColor Red
    exit 1
}

$sizeMB = [math]::Round((Get-Item $APK_SRC).Length / 1MB, 1)
Write-Host ""
Write-Host "✅ 빌드 성공: $sizeMB MB" -ForegroundColor Green
Write-Host "   └ $APK_SRC" -ForegroundColor DarkGray

# 서버 배포 폴더로 복사
$updateDir = Split-Path $APK_DST
if (-not (Test-Path $updateDir)) { New-Item -ItemType Directory -Force $updateDir | Out-Null }

Copy-Item $APK_SRC $APK_DST -Force
Write-Host ""
Write-Host "📦 서버 배포 폴더 복사 완료" -ForegroundColor Green
Write-Host "   └ $APK_DST" -ForegroundColor DarkGray
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "  대시보드 → 환경설정 → OTA 배포에서 바로 배포하세요" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
