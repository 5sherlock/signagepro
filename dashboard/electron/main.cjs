const { app, BrowserWindow } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const fs = require('fs');
const isDev = process.env.NODE_ENV === 'development';

let mainWindow;
let serverProcess;

function startServer() {
  const serverPath = isDev
    ? path.join(__dirname, '../../server/index.js')
    : path.join(process.resourcesPath, 'server/index.js');

  const serverDir = path.dirname(serverPath);

  // 프로덕션: DB 파일 및 uploads 폴더를 사용자 데이터 폴더로 분리
  const userDataPath = app.getPath('userData');
  const dbPath = path.join(userDataPath, 'dev.db');
  const uploadsPath = path.join(userDataPath, 'uploads');

  // uploads 폴더 생성
  if (!fs.existsSync(uploadsPath)) {
    fs.mkdirSync(uploadsPath, { recursive: true });
  }

  // 최초 실행 시: 번들된 DB를 userData로 복사
  if (!isDev && !fs.existsSync(dbPath)) {
    const bundledDb = path.join(serverDir, 'prisma', 'dev.db');
    if (fs.existsSync(bundledDb)) {
      fs.copyFileSync(bundledDb, dbPath);
      console.log('[Main] DB 초기화: 번들 DB 복사 완료');
    }
  }

  const envVars = {
    ...process.env,
    PORT: process.env.PORT || '3300',
    NODE_ENV: isDev ? 'development' : 'production',
    DATABASE_URL: isDev
      ? `file:${path.resolve(serverDir, 'prisma/dev.db')}`
      : `file:${dbPath}`,
    UPLOADS_DIR: isDev
      ? path.join(serverDir, 'uploads')
      : uploadsPath,
  };

  console.log(`[Main] 백엔드 서버 시작: ${serverPath}`);
  console.log(`[Main] DB 경로: ${envVars.DATABASE_URL}`);

  serverProcess = spawn('node', [serverPath], {
    cwd: serverDir,
    env: envVars,
  });

  serverProcess.stdout.on('data', (data) => {
    console.log(`[Server] ${data}`);
  });

  serverProcess.stderr.on('data', (data) => {
    console.error(`[Server Error] ${data}`);
  });

  serverProcess.on('close', (code) => {
    console.log(`[Main] 서버 프로세스 종료 (code: ${code})`);
  });
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1024,
    minHeight: 700,
    title: 'SignagePro Admin',
    icon: path.join(__dirname, '../public/icon.png'),
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
    },
    backgroundColor: '#0f172a',
  });

  // 메뉴바 숨기기
  mainWindow.setMenuBarVisibility(false);

  if (isDev) {
    const devUrl = 'http://localhost:5173';

    const loadWithRetry = () => {
      mainWindow.loadURL(devUrl).catch(() => {
        console.log('[Main] Vite 서버 준비 중, 1초 후 재시도...');
        setTimeout(loadWithRetry, 1000);
      });
    };

    loadWithRetry();
    mainWindow.webContents.openDevTools();
  } else {
    // 서버 기동 대기 후 로드 (1.5초)
    setTimeout(() => {
      mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
    }, 1500);
  }

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  startServer();
  createWindow();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('will-quit', () => {
  if (serverProcess) {
    console.log('[Main] 백엔드 서버 종료 중...');
    serverProcess.kill();
  }
});

app.on('activate', () => {
  if (mainWindow === null) {
    createWindow();
  }
});

