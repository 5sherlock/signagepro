const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const net = require('net');
const { machineIdSync } = require('node-machine-id');
const os = require('os');
const fs = require('fs');

let SERVER_HOST = '127.0.0.1'; // 기본 서버 IP
const TCP_PORT = 10080;

// 설정 파일에서 서버 IP 로드
const configPath = path.join(app.getPath('userData'), 'config.json');
if (!fs.existsSync(configPath)) {
  fs.writeFileSync(configPath, JSON.stringify({ serverIp: "127.0.0.1" }, null, 2));
} else {
  try {
    const config = JSON.parse(fs.readFileSync(configPath));
    if (config.serverIp) SERVER_HOST = config.serverIp;
  } catch(e){}
}

let mainWindow;
let tcpClient;
let mId = machineIdSync();

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1920,
    height: 1080,
    fullscreen: true, // 풀스크린 모드
    autoHideMenuBar: true,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  mainWindow.loadFile('index.html');
}

function getCpuUsage() {
  const cpus = os.cpus();
  let user = 0, nice = 0, sys = 0, idle = 0, irq = 0;
  for (let cpu in cpus) {
    user += cpus[cpu].times.user;
    nice += cpus[cpu].times.nice;
    sys += cpus[cpu].times.sys;
    irq += cpus[cpu].times.irq;
    idle += cpus[cpu].times.idle;
  }
  const total = user + nice + sys + idle + irq;
  const active = user + nice + sys + irq;
  return Math.round((active / total) * 100);
}

function getMemUsage() {
  const total = os.totalmem();
  const free = os.freemem();
  return Math.round(((total - free) / total) * 100);
}

function connectTCP() {
  tcpClient = new net.Socket();
  
  tcpClient.connect(TCP_PORT, SERVER_HOST, () => {
    console.log('Connected to TCP server');
    sendStatus();
    setInterval(sendStatus, 10000); // 10초마다 하트비트 전송
  });

  tcpClient.on('data', (data) => {
    const msg = data.toString().trim();
    console.log('TCP Received:', msg);
    
    // 서버로부터의 원격 제어 명령 처리
    if (msg.includes('cmd:reboot')) {
      console.log('Reboot command received!');
      // TODO: 시스템 재부팅 로직 구현 (os 권한 필요)
    }
    
    if (msg.includes('cmd:delete')) {
      console.log('Device deletion command received from server. Wiping local device name...');
      try {
        let serverIp = '127.0.0.1';
        try {
          if (fs.existsSync(configPath)) {
            const config = JSON.parse(fs.readFileSync(configPath));
            if (config.serverIp) serverIp = config.serverIp;
          }
        } catch(e) {}
        fs.writeFileSync(configPath, JSON.stringify({ serverIp }, null, 2));
      } catch(e) {
        console.error('Failed to clear config:', e);
      }
      mainWindow.reload();
    }
  });

  tcpClient.on('close', () => {
    console.log('TCP Connection closed. Reconnecting in 5s...');
    setTimeout(connectTCP, 5000);
  });

  tcpClient.on('error', (err) => {
    console.error('TCP Error:', err.message);
  });
}

function sendStatus() {
  if (tcpClient && !tcpClient.destroyed) {
    const cpu = getCpuUsage();
    const mem = getMemUsage();
    
    let deviceName = '';
    try {
      const config = JSON.parse(fs.readFileSync(configPath));
      deviceName = config.deviceName || '';
    } catch(e){}
    
    const namePart = deviceName ? `/name:${encodeURIComponent(deviceName)}` : '';
    // 기기 상태 정보 및 하트비트 전송
    tcpClient.write(`status:${mId}${namePart}/cpu:${cpu}/mem:${mem}\n`);
  }
}

app.whenReady().then(() => {
  // IPC를 통해 렌더러 프로세스에 정보 전달
  ipcMain.handle('get-machine-id', () => mId);
  ipcMain.handle('get-server-host', () => SERVER_HOST);
  
  ipcMain.handle('get-device-name', () => {
    try {
      const config = JSON.parse(fs.readFileSync(configPath));
      return config.deviceName || '';
    } catch(e) {
      return '';
    }
  });

  ipcMain.handle('save-device-config', (event, newIp, newName) => {
    SERVER_HOST = newIp;
    fs.writeFileSync(configPath, JSON.stringify({ serverIp: newIp, deviceName: newName }, null, 2));
    
    // 연결 리셋 및 창 새로고침
    if (tcpClient && !tcpClient.destroyed) {
      tcpClient.destroy();
    }
    connectTCP();
    mainWindow.reload();
    return true;
  });

  createWindow();
  connectTCP();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
