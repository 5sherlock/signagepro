$searchPaths = @(
    "C:\Program Files\Java",
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\Amazon Corretto",
    "C:\Program Files\Microsoft",
    "C:\Program Files\BellSoft"
)

$found = $null
foreach ($p in $searchPaths) {
    if (Test-Path $p) {
        $dirs = Get-ChildItem -Path $p -Directory
        # Java 11 이상 (17, 21 등) 폴더 찾기
        foreach ($d in $dirs) {
            if ($d.Name -match "11" -or $d.Name -match "17" -or $d.Name -match "21" -or $d.Name -match "jdk-") {
                $javaExe = Join-Path $d.FullName "bin\java.exe"
                if (Test-Path $javaExe) {
                    $found = $d.FullName
                    break
                }
            }
        }
    }
    if ($found) { break }
}

if ($found) {
    Write-Host "Found modern Java at: $found"
    $env:JAVA_HOME = $found
} else {
    Write-Host "Could not find Java in default Program Files. Attempting to use default system Java..."
    # 기존에 설정되어 있던 오래된 JAVA_HOME 제거
    Remove-Item Env:\JAVA_HOME -ErrorAction SilentlyContinue
}

# 새로 설정된 자바 버전 확인
& java -version

Write-Host "Starting Gradle Build..."
cd c:\WorkSpace\signagepro\android_player\android
.\gradlew --stop
.\gradlew assembleDebug "-Dorg.gradle.java.home=$found"
