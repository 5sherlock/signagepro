# SignagePro 시작 스크립트

# 기존 프로세스 정리
Get-Process -Name "node","electron" -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

# 서버 실행 (숨김)
Start-Process -FilePath "node" -ArgumentList "C:\WorkSpace\signagepro\server\index.js" -WorkingDirectory "C:\WorkSpace\signagepro\server" -WindowStyle Hidden

# 서버 대기 (최대 30초)
$ok = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient("localhost", 3000)
        $tcp.Close()
        $ok = $true
        break
    } catch {}
}
if (-not $ok) {
    [System.Windows.Forms.MessageBox]::Show("서버 시작 실패", "SignagePro")
    exit
}

# Vite 실행 (숨김)
Start-Process -FilePath "cmd" -ArgumentList "/c npx vite" -WorkingDirectory "C:\WorkSpace\signagepro\dashboard" -WindowStyle Hidden

# Vite 대기 (최대 30초)
$ok = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient("localhost", 5173)
        $tcp.Close()
        $ok = $true
        break
    } catch {}
}
if (-not $ok) {
    [System.Windows.Forms.MessageBox]::Show("Vite 시작 실패", "SignagePro")
    exit
}

# 브라우저 열기
Start-Process "http://localhost:5173"
