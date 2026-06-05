# SignagePro ?쒖옉 ?ㅽ겕由쏀듃

# 湲곗〈 ?꾨줈?몄뒪 ?뺣━
Get-Process -Name "node","electron" -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

# ?쒕쾭 ?ㅽ뻾 (?④?)
Start-Process -FilePath "node" -ArgumentList "D:\WorkSpace\signagepro\server\index.js" -WorkingDirectory "D:\WorkSpace\signagepro\server" -WindowStyle Hidden

# ?쒕쾭 ?湲?(理쒕? 30珥?
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
    [System.Windows.Forms.MessageBox]::Show("?쒕쾭 ?쒖옉 ?ㅽ뙣", "SignagePro")
    exit
}

# Vite ?ㅽ뻾 (?④?)
Start-Process -FilePath "cmd" -ArgumentList "/c npx vite" -WorkingDirectory "D:\WorkSpace\signagepro\dashboard" -WindowStyle Hidden

# Vite ?湲?(理쒕? 30珥?
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
    [System.Windows.Forms.MessageBox]::Show("Vite ?쒖옉 ?ㅽ뙣", "SignagePro")
    exit
}

# 釉뚮씪?곗? ?닿린
Start-Process "http://localhost:5173"

