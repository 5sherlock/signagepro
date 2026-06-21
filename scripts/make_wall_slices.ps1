# 멀티스크린 비디오월 테스트 슬라이스 생성기
#
# 원본 가로영상을 N개 세로조각으로 분할 → 각 STB가 자기 조각을 재생.
# 파일명에 "wallsync"를 넣어 앱이 wall-동기 비디오로 인식하게 한다(스파이크 트리거).
#
# 사용:
#   ./make_wall_slices.ps1 -Source D:\path\panorama.mp4 -Count 2
#   ./make_wall_slices.ps1 -Source ... -Count 5 -OutDir ..\sample_media
#
# 요구: ffmpeg PATH 등록 (winget install Gyan.FFmpeg  또는  choco install ffmpeg)

param(
    [Parameter(Mandatory = $true)][string]$Source,
    [int]$Count = 2,
    [string]$OutDir = "..\sample_media"
)

$ff = Get-Command ffmpeg -ErrorAction SilentlyContinue
if (-not $ff) { Write-Error "ffmpeg가 PATH에 없습니다. 'winget install Gyan.FFmpeg' 후 새 셸에서 재시도."; exit 1 }
if (-not (Test-Path $Source)) { Write-Error "원본 없음: $Source"; exit 1 }
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Force $OutDir | Out-Null }

# 원본 해상도 측정
$probe = & ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=p=0 $Source
$w, $h = $probe.Trim().Split(',')
$w = [int]$w; $h = [int]$h
if ($w % $Count -ne 0) { Write-Warning "폭 $w 가 $Count 로 안 나눠떨어짐 — 마지막 조각이 어긋날 수 있음" }
$sliceW = [int]($w / $Count)
Write-Host "원본 ${w}x${h} → $Count 조각, 조각폭 $sliceW"

for ($i = 0; $i -lt $Count; $i++) {
    $x = $i * $sliceW
    $out = Join-Path $OutDir ("wallsync_slice{0}.mp4" -f $i)
    Write-Host "[$i] crop ${sliceW}x${h}+${x}+0 -> $out"
    # -g 15: GOP 15프레임(≈0.5s) — HARD_SEEK 가 키프레임 근방에 정확히 안착하도록 촘촘하게
    # -an: 무음 (사이니지)   -pix_fmt yuv420p: STB 디코더 호환
    & ffmpeg -y -i $Source `
        -vf "crop=${sliceW}:${h}:${x}:0" `
        -c:v libx264 -preset medium -profile:v high -level 4.0 `
        -g 15 -keyint_min 15 -sc_threshold 0 `
        -pix_fmt yuv420p -an `
        $out 2>$null
    if ($LASTEXITCODE -ne 0) { Write-Error "조각 $i 인코딩 실패"; exit 1 }
}
Write-Host "완료. $OutDir 에 wallsync_slice0..$($Count-1).mp4 생성됨." -ForegroundColor Green
Write-Host "다음: 대시보드에서 각 조각 업로드 → 플레이리스트 항목별 targetDeviceId 배정." -ForegroundColor Cyan
