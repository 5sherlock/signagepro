# -----------------------------------------------------------------------------
# build_apk.ps1  - SignagePro Android APK Build and Deploy
# -----------------------------------------------------------------------------

$ROOT    = $PSScriptRoot
$ANDROID = "$ROOT\android"
$gradleFile = "$ANDROID\app\build.gradle.kts"
$versionName = (Select-String -Path $gradleFile -Pattern 'versionName\s*=\s*"([^"]+)"').Matches[0].Groups[1].Value
$APK_SRC = "$ANDROID\app\build\outputs\apk\release\signagepro-$versionName.apk"
$APK_DST = "$ROOT\server\update\app.apk"
$APK_NAMED  = "$ROOT\server\update\signagepro-$versionName.apk"

Set-Location $ANDROID

Write-Host "========================================"
Write-Host "  SignagePro APK Build Start"
Write-Host "========================================"

# Build
.\gradlew assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build Failed (exit code $LASTEXITCODE)" -ForegroundColor Red
    exit 1
}

# Check output
if (-not (Test-Path $APK_SRC)) {
    Write-Host "APK not found: $APK_SRC" -ForegroundColor Red
    exit 1
}

$sizeMB = [math]::Round((Get-Item $APK_SRC).Length / 1MB, 1)
Write-Host "Build Success: $sizeMB MB"
Write-Host "   Path: $APK_SRC"

# Copy to update folder
$updateDir = Split-Path $APK_DST
if (-not (Test-Path $updateDir)) { New-Item -ItemType Directory -Force $updateDir | Out-Null }

Copy-Item $APK_SRC $APK_DST -Force
Copy-Item $APK_SRC $APK_NAMED -Force
Write-Host "Copy complete"
Write-Host "   Target: $APK_DST"
Write-Host "   Target: $APK_NAMED"

# Upload to server function
function Upload-Apk($label, $url) {
    Write-Host "Uploading to $label..."
    $result = curl.exe -s -o - -w "`n%{http_code}" `
        -X POST "$url/api/update/apk" `
        -H "Authorization: Bearer dev-mode" `
        -F "apk=@`"$APK_DST`";filename=signagepro-$versionName.apk" `
        --max-time 120
    $lines    = $result -split "`n"
    $httpCode = $lines[-1].Trim()
    $body     = ($lines[0..($lines.Count-2)] -join "`n").Trim()
    if ($httpCode -eq "200") {
        Write-Host "Upload Success: $label (v$versionName)"
    } else {
        Write-Host "Upload Failed: $label (HTTP $httpCode): $body" -ForegroundColor Yellow
    }
}

Upload-Apk "Dev Server" "http://172.30.1.44:3000"
Write-Host ""
