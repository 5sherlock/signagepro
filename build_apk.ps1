# ─────────────────────────────────────────────────────────────────────────────
# build_apk.ps1  — SignagePro Android APK 빌드 + 서버 배포 폴더 자동 복사
#
# 사용법: .\build_apk.ps1
#   1. Release APK 빌드 (android/app/build/outputs/apk/release/app-release.apk)
#   2. server/update/app.apk 로 자동 복사 → 대시보드에서 바로 OTA 배포 가능
# ─────────────────────────────────────────────────────────────────────────────

$ROOT    = $PSScriptRoot
$ANDROID = "$ROOT\android"
$APK_SRC = "$ANDROID\app\build\outputs\apk\release\app-release.apk"
$APK_DST = "$ROOT\server\update\app.apk"

# build.gradle.kts에서 versionName 자동 추출
$gradleFile = "$ANDROID\app\build.gradle.kts"
$versionName = (Select-String -Path $gradleFile -Pattern 'versionName\s*=\s*"([^"]+)"').Matches[0].Groups[1].Value
$APK_NAMED  = "$ROOT\server\update\signagepro-$versionName.apk"

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
Copy-Item $APK_SRC $APK_NAMED -Force
Write-Host ""
Write-Host "📦 서버 배포 폴더 복사 완료" -ForegroundColor Green
Write-Host "   └ $APK_DST" -ForegroundColor DarkGray
Write-Host "   └ $APK_NAMED (버전명 포함)" -ForegroundColor DarkGray
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "  대시보드 → 환경설정 → OTA 배포에서 바로 배포하세요" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# 서버 업로드 함수
function Upload-Apk($label, $url) {
    Write-Host "🌐 $label 업로드 중..." -ForegroundColor Cyan
    $result = curl.exe -s -o - -w "`n%{http_code}" `
        -X POST "$url/api/update/apk" `
        -F "apk=@`"$APK_DST`";filename=signagepro-$versionName.apk" `
        --max-time 120
    $lines    = $result -split "`n"
    $httpCode = $lines[-1].Trim()
    $body     = ($lines[0..($lines.Count-2)] -join "`n").Trim()
    if ($httpCode -eq "200") {
        Write-Host "✅ $label 업로드 완료 (v$versionName)" -ForegroundColor Green
    } else {
        Write-Host "⚠️  $label 업로드 실패 (HTTP $httpCode): $body" -ForegroundColor Yellow
    }
}

Upload-Apk "개발 서버(3000)" "http://172.30.1.44:3000"
# 안정성 확인 후 수동으로 3300 배포
Write-Host ""
