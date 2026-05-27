@echo off
chcp 65001 >nul

:: 기존 프로세스 정리
taskkill /f /im node.exe >nul 2>&1
taskkill /f /im electron.exe >nul 2>&1
timeout /t 2 /nobreak >nul

:: 서버 실행 (숨김)
start "" /min cmd /c "cd /d C:\WorkSpace\signagepro\server && node index.js"

:: 서버 뜰 때까지 대기 (최대 30초)
set /a cnt=0
:wait_server
timeout /t 1 /nobreak >nul
set /a cnt+=1
powershell -NoProfile -Command "try { (New-Object Net.Sockets.TcpClient('localhost',3000)).Close(); exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel%==0 goto server_ok
if %cnt% geq 30 goto fail
goto wait_server
:server_ok

:: Vite 실행 (숨김)
start "" /min cmd /c "cd /d C:\WorkSpace\signagepro\dashboard && npx vite"

:: Vite 뜰 때까지 대기 (최대 30초)
set /a cnt=0
:wait_vite
timeout /t 1 /nobreak >nul
set /a cnt+=1
powershell -NoProfile -Command "try { (New-Object Net.Sockets.TcpClient('localhost',5173)).Close(); exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel%==0 goto vite_ok
if %cnt% geq 30 goto fail
goto wait_vite
:vite_ok

:: 브라우저 열기
start http://localhost:5173
exit

:fail
powershell -NoProfile -Command "Add-Type -AssemblyName PresentationFramework; [System.Windows.MessageBox]::Show('서버 또는 Vite 시작 실패. 로그를 확인하세요.', 'SignagePro 오류')"
exit
