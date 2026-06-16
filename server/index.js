require('dotenv').config();
const express = require('express');
const cors = require('cors');
const net = require('net');
const { PrismaClient } = require('@prisma/client');
const http = require('http');
const { Server } = require('socket.io');
const multer = require('multer');
const Busboy = require('busboy');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const { execFile } = require('child_process');
const os = require('os');
const cron = require('node-cron');
const { S3Client, PutObjectCommand, DeleteObjectCommand } = require('@aws-sdk/client-s3');

// R2 클라이언트 (환경변수 미설정 시 null → 로컬 저장소 폴백)
const r2 = process.env.R2_ENDPOINT ? new S3Client({
  region: 'auto',
  endpoint: process.env.R2_ENDPOINT,
  credentials: {
    accessKeyId: process.env.R2_ACCESS_KEY,
    secretAccessKey: process.env.R2_SECRET_KEY,
  },
}) : null;

async function uploadToR2(localPath, key, mimeType) {
  if (!r2) return null;
  await r2.send(new PutObjectCommand({
    Bucket: process.env.R2_BUCKET,
    Key: key,
    Body: fs.createReadStream(localPath),
    ContentType: mimeType,
  }));
  return `${process.env.R2_PUBLIC_URL}/${key}`;
}

async function deleteFromR2(key) {
  if (!r2) return;
  try {
    await r2.send(new DeleteObjectCommand({ Bucket: process.env.R2_BUCKET, Key: key }));
  } catch (e) {
    console.error('[R2] 삭제 실패:', e.message);
  }
}

// ADB 실행 경로 계산 (프로젝트 로컬 server/bin/adb.exe가 존재하면 우선적으로 적용)
const getAdbPath = () => {
  if (process.env.ADB_PATH) {
    return process.env.ADB_PATH;
  }
  const localAdb = path.join(__dirname, 'bin', 'adb.exe');
  if (fs.existsSync(localAdb)) {
    return localAdb;
  }
  return 'adb';
};
const GLOBAL_ADB_PATH = getAdbPath();
console.log(`[ADB] 감지된 ADB 경로: ${GLOBAL_ADB_PATH}`);

const prisma = new PrismaClient();
const app = express();

// ── 프록시 신뢰 & 실제 클라이언트 IP 식별 ──────────────────────────────────────
// 운영은 Cloudflare 터널(cloudflared) 뒤에 있어 들어오는 연결의 출발지가 loopback이다.
// loopback만 신뢰하면 LAN 직접 접속 클라이언트의 X-Forwarded-For 위조는 무시된다.
// 실제 클라이언트 IP는 Cloudflare가 세팅하는 CF-Connecting-IP를 우선 사용한다.
// (전제: origin :PORT는 터널을 통해서만 외부 노출 — 직접 노출 시 CF-Connecting-IP 위조 가능)
app.set('trust proxy', 'loopback');
function getClientIp(req) {
  return req.headers['cf-connecting-ip'] || req.ip || req.socket?.remoteAddress || '';
}

// 메모리에 보관할 기기별 실시간 상태 캐시 (deviceTime, slide, dl, vol, vu)
const deviceLiveStateCache = new Map();
const pendingCmdCallbacks = new Map();

const DEVICE_SECRET = process.env.DEVICE_SECRET || 'changeme';
const HEARTBEAT_TIMEOUT_MS = 35000; // 하트비트 10초 간격 × 3 + 여유
if (DEVICE_SECRET === 'changeme') {
  console.warn('[WARN] DEVICE_SECRET이 기본값입니다. .env에서 변경하세요.');
}
// 웹 플레이어(브라우저 송출 화면) 전용 시크릿 — STB의 DEVICE_SECRET과 분리(브라우저 노출 격리)
const WEB_PLAYER_SECRET = process.env.WEB_PLAYER_SECRET || '';
if (!WEB_PLAYER_SECRET) {
  console.warn('[WARN] WEB_PLAYER_SECRET 미설정 — 웹 플레이어 Socket.io 연결이 거부됩니다. .env에서 설정하세요.');
}

let adminPassword = process.env.adminPassword || '';
if (!adminPassword) {
  console.warn('[WARN] adminPassword 미설정 — 대시보드 인증이 비활성화됩니다. .env에서 설정하세요.');
}

const envPath = path.join(__dirname, '.env');
function saveAdminPassword(newPw) {
  adminPassword = newPw;
  try {
    let content = fs.existsSync(envPath) ? fs.readFileSync(envPath, 'utf8') : '';
    if (/^adminPassword=/m.test(content)) {
      content = content.replace(/^adminPassword=.*/m, `adminPassword="${newPw}"`);
    } else {
      content += `\nadminPassword="${newPw}"`;
    }
    fs.writeFileSync(envPath, content, 'utf8');
  } catch (e) {
    console.error('[ERROR] .env 저장 실패:', e.message);
  }
}

// HMAC 서명 토큰 — 서버 재시작 후에도 adminPassword로 검증 가능
function createToken() {
  const expiry = (Date.now() + 7 * 24 * 60 * 60 * 1000).toString(16);
  const sig = crypto.createHmac('sha256', adminPassword).update(expiry).digest('hex');
  return `${expiry}.${sig}`;
}
function verifySignedToken(token) {
  const dot = token.indexOf('.');
  if (dot < 0) return false;
  const expiry = token.slice(0, dot);
  const sig = token.slice(dot + 1);
  const expected = crypto.createHmac('sha256', adminPassword).update(expiry).digest('hex');
  if (sig !== expected) return false;
  return Date.now() < parseInt(expiry, 16);
}

app.use(cors({
  origin: '*',
  methods: ['GET', 'HEAD', 'PUT', 'PATCH', 'POST', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
}));
app.options('/{*path}', cors()); // FormData + Authorization 등 복잡한 요청의 프리플라이트 OPTIONS 명시 처리
app.use((req, res, next) => {
  console.log(`[REQ] ${new Date().toISOString()} ${getClientIp(req)} ${req.method} ${req.url}`);
  next();
});
app.use(express.json());

// ── 인증 ─────────────────────────────────────────────────────────────────────

const loginAttempts = new Map(); // ip → { count, lockedUntil }
const MAX_ATTEMPTS  = 5;
const LOCKOUT_MS    = 15 * 60 * 1000; // 15분

app.post('/api/auth/login', (req, res) => {
  if (!adminPassword) return res.status(503).json({ error: 'adminPassword가 설정되지 않았습니다. server/.env에 adminPassword를 설정한 뒤 서버를 재시작하세요.' });

  const ip  = getClientIp(req);
  const now = Date.now();
  const rec = loginAttempts.get(ip) || { count: 0, lockedUntil: 0 };

  if (rec.lockedUntil > now) {
    const mins = Math.ceil((rec.lockedUntil - now) / 60000);
    return res.status(429).json({ error: `너무 많은 시도입니다. ${mins}분 후 다시 시도하세요.` });
  }

  if (req.body.password !== adminPassword) {
    rec.count += 1;
    if (rec.count >= MAX_ATTEMPTS) {
      rec.lockedUntil = now + LOCKOUT_MS;
      rec.count = 0;
      loginAttempts.set(ip, rec);
      return res.status(429).json({ error: '비밀번호 5회 오류. 15분 동안 잠금됩니다.' });
    }
    loginAttempts.set(ip, rec);
    return res.status(401).json({ error: `비밀번호가 틀렸습니다. (${MAX_ATTEMPTS - rec.count}회 남음)` });
  }

  loginAttempts.delete(ip);
  res.json({ token: createToken() });
});

app.post('/api/auth/change-password', (req, res) => {
  const { current, newPassword } = req.body;
  if (adminPassword && current !== adminPassword)
    return res.status(401).json({ error: '현재 비밀번호가 틀렸습니다.' });
  if (!newPassword || newPassword.length < 4)
    return res.status(400).json({ error: '새 비밀번호는 4자 이상이어야 합니다.' });
  saveAdminPassword(newPassword);
  // 비밀번호 변경 시 기존 토큰 자동 무효화 (HMAC 키가 바뀌므로 별도 처리 불필요)
  res.json({ ok: true });
});

app.post('/api/auth/logout', (req, res) => {
  res.json({ ok: true }); // 클라이언트에서 토큰 삭제; 서버 저장소 없음
});

const requireAuth = (req, res, next) => {
  if (!adminPassword) return res.status(503).json({ error: '서버 인증이 구성되지 않았습니다(adminPassword 미설정).' });
  const token = req.headers.authorization?.slice(7);
  if (token && verifySignedToken(token)) return next();
  return res.status(401).json({ error: '인증이 필요합니다.' });
};

// 기기 전용 GET API는 인증 제외 (Android 앱이 직접 호출)
// app.use('/api', ...) 내부에서 req.path는 /api 제거된 상대경로임
const DEVICE_OPEN = [
  '/time',
  /^\/devices\/[^/]+$/,
  /^\/groups\/[^/]+\/playlist$/,
];
app.use('/api', (req, res, next) => {
  if (req.path.startsWith('/auth/')) return next();
  const open = DEVICE_OPEN.some(p =>
    typeof p === 'string' ? req.path === p : p.test(req.path)
  );
  if (open && req.method === 'GET') return next();
  requireAuth(req, res, next);
});
app.get('/api/auth/verify', requireAuth, (req, res) => res.json({ ok: true }));

app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Ensure uploads folder exists
// 프로덕션(Electron)에서는 UPLOADS_DIR 환경변수로 userData 경로 사용
const uploadDir = process.env.UPLOADS_DIR || path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// Configure multer
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => cb(null, Date.now() + '-' + file.originalname)
});
const ALLOWED_MIME = [
  'video/mp4', 'video/webm', 'video/quicktime',   // mp4, webm, mov
  'video/x-msvideo', 'video/x-matroska',           // avi, mkv
  'image/jpeg', 'image/png', 'image/gif', 'image/webp'
];
const upload = multer({
  storage,
  limits: { fileSize: 2 * 1024 * 1024 * 1024 }, // 2GB
  fileFilter: (req, file, cb) => {
    if (ALLOWED_MIME.includes(file.mimetype)) cb(null, true);
    else cb(new Error(`허용되지 않는 파일 형식: ${file.mimetype}`));
  }
});

// APK 업로드용 multer (update 폴더에 app.apk로 저장)
const apkStorage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, updateDir),
  filename: (req, file, cb) => cb(null, 'app.apk'),
});
const uploadApk = multer({
  storage: apkStorage,
  limits: { fileSize: 500 * 1024 * 1024 }, // 500MB
  fileFilter: (req, file, cb) => {
    const ok = file.originalname.endsWith('.apk') || file.mimetype === 'application/vnd.android.package-archive' || file.mimetype === 'application/octet-stream';
    ok ? cb(null, true) : cb(new Error('APK 파일만 업로드 가능합니다.'));
  },
});

const httpServer = http.createServer(app);
// CORS: 운영은 SOCKET_CORS_ORIGINS(쉼표구분)로 화이트리스트 권장. 미설정 시 요청 Origin 반영.
const SOCKET_CORS = process.env.SOCKET_CORS_ORIGINS
  ? process.env.SOCKET_CORS_ORIGINS.split(',').map(s => s.trim()).filter(Boolean)
  : true;
const io = new Server(httpServer, {
  cors: { origin: SOCKET_CORS, credentials: true },
  allowEIO3: true
});

// --- Socket.io 인증 미들웨어 (무인증 연결 즉시 거부) ───────────────────────────
// 기기(STB)=DEVICE_SECRET · 웹 플레이어=WEB_PLAYER_SECRET · 대시보드=관리자 HMAC 토큰
// 자격증명은 핸드셰이크 auth 로 전달: io(url, { auth: { deviceId, secret } | { token } })
io.use((socket, next) => {
  const a = socket.handshake.auth || {};
  if (a.secret && a.secret === DEVICE_SECRET) {
    socket.role = 'device';
    socket.authDeviceId = a.deviceId ? String(a.deviceId) : null;
    return next();
  }
  if (a.secret && WEB_PLAYER_SECRET && a.secret === WEB_PLAYER_SECRET) {
    socket.role = 'webplayer';
    socket.authDeviceId = a.deviceId ? String(a.deviceId) : null;
    return next();
  }
  if (a.token && verifySignedToken(a.token)) {
    socket.role = 'admin';
    return next();
  }
  console.warn(`[Socket.io] 인증 거부 id=${socket.id} origin=${socket.handshake.headers.origin}`);
  return next(new Error('unauthorized'));
});

// --- Socket.io 웹 플레이어 연결 처리 ---
io.on('connection', (socket) => {
  console.log(`[Socket.io] 연결됨 id=${socket.id} origin=${socket.handshake.headers.origin}`);
  socket.on('disconnect', (reason) => {
    console.log(`[Socket.io] 끊김 id=${socket.id} reason=${reason}`);
  });
  // Android ControlChannel이 연결 직후 자신의 deviceId를 등록
  socket.on('register_device', async (data) => {
    if (socket.role !== 'device') return; // STB만 기기 룸 등록 가능
    const deviceId = typeof data === 'object' ? data?.deviceId : String(data);
    if (!deviceId) return;
    if (socket.authDeviceId && deviceId !== socket.authDeviceId) {
      console.warn(`[Socket.io] register_device 불일치 차단: auth=${socket.authDeviceId} req=${deviceId}`);
      return;
    }
    socket.deviceId = deviceId;
    socket.join(`device:${deviceId}`);
    console.log(`[Socket.io] 기기 등록: ${deviceId}`);

    // 재연결 시 OTA 버전 자동 체크 — 구버전이면 즉시 update_apk 재발송
    // (배포 중 오프라인이었거나 설치 실패한 기기 자동 복구)
    try {
      const apkPath = path.join(updateDir, 'app.apk');
      const meta = loadDeployMeta();
      const targetVersion = meta.apkVersion;
      if (targetVersion && fs.existsSync(apkPath)) {
        const device = await prisma.device.findUnique({ where: { id: deviceId } });
        if (device) {
          const raw = device.appVersion || '';
          const currentVer = raw.includes(' (') ? raw.slice(0, raw.indexOf(' (')) : raw;
          if (currentVer !== targetVersion) {
            console.log(`[OTA-Reconnect] ${deviceId}: 구버전 감지 (${currentVer || 'unknown'} → v${targetVersion}) — update_apk 자동 발송`);
            socket.emit('update_apk', { url: '/update/apk', deviceId });
          } else {
            console.log(`[OTA-Reconnect] ${deviceId}: 최신 버전 확인 (v${currentVer})`);
          }
        }
      }
    } catch (e) {
      console.warn(`[OTA-Reconnect] ${deviceId} 버전 체크 실패: ${e.message}`);
    }
  });

  socket.on('web_player_heartbeat', async (data) => {
    if (socket.role !== 'webplayer') return; // 웹 플레이어만 허용
    const deviceId = typeof data === 'string' ? data : (data?.deviceId || '');
    const name = typeof data === 'object' ? data?.name : null;
    if (!deviceId) return;
    if (socket.authDeviceId && deviceId !== socket.authDeviceId) return; // 자기 자신만
    socket.deviceId = deviceId;
    
    // 웹 플레이어 실시간 상태 캐시 갱신
    const cached = deviceLiveStateCache.get(deviceId) || {};
    deviceLiveStateCache.set(deviceId, {
      ...cached,
      deviceTime: Date.now()
    });

    try {
      await prisma.device.upsert({
        where: { id: deviceId },
        update: { status: 'online', lastSeen: new Date(), ip: socket.handshake.address },
        create: { id: deviceId, name: name || `Web-${deviceId}`, status: 'online', lastSeen: new Date(), ip: socket.handshake.address }
      });
      io.emit('device_status_update', { deviceId, status: 'online' });
    } catch (e) {
      console.error('[Socket] heartbeat DB 에러:', e);
    }
  });

  socket.on('run_cmd_result', (data) => {
    if (socket.role !== 'device') return; // 기기만 명령 결과 전송 가능
    if (data && data.deviceId && data.cmd) {
      const key = `${data.deviceId}:${data.cmd}`;
      const cb = pendingCmdCallbacks.get(key);
      if (cb) {
        cb(data.output);
        pendingCmdCallbacks.delete(key);
      }
    }
  });

  socket.on('disconnect', () => {
    if (socket.deviceId) {
      prisma.device.update({ where: { id: socket.deviceId }, data: { status: 'offline' } })
        .then(() => io.emit('device_status_update', { deviceId: socket.deviceId, status: 'offline' }))
        .catch(() => {});
    }
  });
});

// --- REST API (대시보드 통신용) ---

// 모든 사업장 조회
app.get('/api/stores', async (req, res) => {
  const stores = await prisma.store.findMany({
    orderBy: { order: 'asc' }
  });
  res.json(stores);
});

// 사업장 추가
app.post('/api/stores', async (req, res) => {
  try {
    // 가장 큰 order 값 찾기
    const lastStore = await prisma.store.findFirst({
      orderBy: { order: 'desc' }
    });
    const nextOrder = lastStore ? lastStore.order + 1 : 0;
    
    const store = await prisma.store.create({ 
      data: { name: req.body.name, order: nextOrder } 
    });
    res.json(store);
  } catch (err) {
    console.error('[API] 사업장 추가 에러:', err);
    res.status(500).json({ error: '사업장 추가 실패', details: err.message });
  }
});

// 사업장 순서 변경
app.post('/api/stores/reorder', async (req, res) => {
  const { storeIds } = req.body; // [id1, id2, id3, ...]
  try {
    await prisma.$transaction(
      storeIds.map((id, index) => 
        prisma.store.update({
          where: { id },
          data: { order: index }
        })
      )
    );
    res.json({ success: true });
  } catch (err) {
    console.error('[API] 사업장 순서 변경 에러:', err);
    res.status(500).json({ error: '순서 변경 실패' });
  }
});

// 사업장 삭제 (연관 데이터 정리 포함)
app.delete('/api/stores/:id', async (req, res) => {
  const { id } = req.params;
  console.log(`[API] 사업장 삭제 요청: id=${id}`);
  try {
    await prisma.$transaction(async (tx) => {
      // 1. 미디어 파일 삭제 준비 (조회만)
      const medias = await tx.media.findMany({ where: { storeId: id } });
      const mediaIds = medias.map(m => m.id);
      
      // 2. 기기 해제 (먼저 수행하여 Group 참조 제거)
      const devices = await tx.device.findMany({ where: { storeId: id } });
      for (const device of devices) {
        await tx.device.update({
          where: { id: device.id },
          data: { groupId: null, storeId: null }
        });
      }

      // 3. 재생목록 관계(PlaylistMedia) 삭제
      // 해당 사업장의 모든 재생목록 ID를 찾음
      const playlists = await tx.playlist.findMany({ where: { storeId: id } });
      const playlistIds = playlists.map(p => p.id);
      
      // 해당 사업장의 재생목록에 속한 미디어 관계 삭제
      await tx.playlistMedia.deleteMany({ where: { playlistId: { in: playlistIds } } });
      
      // 해당 사업장의 미디어가 다른(혹은 같은) 재생목록에 참조된 경우 삭제
      await tx.playlistMedia.deleteMany({ where: { mediaId: { in: mediaIds } } });

      // 4. 재생목록 삭제
      // 해당 사업장의 모든 그룹 ID를 찾음
      const groups = await tx.group.findMany({ where: { storeId: id } });
      const groupIds = groups.map(g => g.id);

      // 사업장 ID로 삭제하거나, 소속 그룹 ID로 삭제 (더 확실함)
      await tx.playlist.deleteMany({ 
        where: { 
          OR: [
            { storeId: id },
            { groupId: { in: groupIds } }
          ]
        } 
      });

      // 5. 미디어 삭제
      await tx.media.deleteMany({ where: { storeId: id } });
      
      // 6. 그룹 삭제
      await tx.group.deleteMany({ where: { storeId: id } });

      // 7. 사업장 삭제
      await tx.store.delete({ where: { id } });

      // 실제 파일 삭제 (트랜잭션 완료 후 처리가 좋으나 여기선 단순하게 진행)
      for (const media of medias) {
        const filePath = path.join(__dirname, media.path);
        if (fs.existsSync(filePath)) {
          try { fs.unlinkSync(filePath); } catch (e) { console.error('파일 삭제 실패:', filePath, e); }
        }
      }
    });

    res.json({ success: true });
  } catch (err) {
    console.error('[API] 사업장 삭제 에러:', err);
    res.status(500).json({ error: '사업장 삭제 실패', details: err.message });
  }
});

// 모든 그룹 조회
app.get('/api/groups', async (req, res) => {
  const { storeId } = req.query;
  const where = storeId ? { storeId } : {};
  const groups = await prisma.group.findMany({ where, include: { devices: true } });
  res.json(groups);
});

// 기기 그룹 추가
app.post('/api/groups', async (req, res) => {
  const { name, storeId } = req.body;
  const group = await prisma.group.create({ data: { name, storeId } });
  res.json(group);
});

// 모든 기기 조회
app.get('/api/devices', async (req, res) => {
  try {
    const [devices, tickers] = await Promise.all([
      prisma.device.findMany({ include: { group: true, store: true }, orderBy: { order: 'asc' } }),
      prisma.ticker.findMany(),
    ]);
    const tickerByGroup = Object.fromEntries(tickers.map(t => [t.groupId, t]));
    const now = Date.now();
    // DB status 를 그대로 믿지 않고, lastSeen 기준으로 실시간 재계산
    // → 스윕/소켓 close 이벤트가 미처 처리되지 않았을 때도 정확한 상태 반환
    const result = devices.map(d => {
      const stale = !d.lastSeen || (now - new Date(d.lastSeen).getTime()) > HEARTBEAT_TIMEOUT_MS;
      const status = stale ? 'offline' : d.status;
      const cached = deviceLiveStateCache.get(d.id) || {};
      return {
        ...d,
        status,
        deviceTime: status === 'online' ? (cached.deviceTime || null) : null,
        slide: status === 'online' ? (cached.slide || null) : null,
        dl: status === 'online' ? (cached.dl || null) : null,
        vol: cached.vol !== undefined ? cached.vol : (d.vol !== undefined ? d.vol : null),
        vu: status === 'online' ? (cached.vu || 0) : 0,
        // 오프라인이면 screenOff 초기화 (기기 재연결 시 새 상태로 갱신됨)
        screenOff: status === 'online' ? (cached.screenOff ?? false) : false,
        // HDMI 연결 상태 (오프라인이면 true)
        hdmiConnected: status === 'online' ? (cached.hdmiConnected ?? true) : true,
        cpuTemp: status === 'online' ? (cached.cpuTemp ?? null) : null,
        diskSpace: status === 'online' ? (cached.diskSpace ?? null) : null,
        ramSpace: status === 'online' ? (cached.ramSpace ?? null) : null,
        tvEdid: status === 'online' ? (cached.tvEdid ?? null) : null,
        tvCec: status === 'online' ? (cached.tvCec ?? null) : null,
        stbSpec: status === 'online' ? (cached.stbSpec ?? null) : null,
        tickerSync: status === 'online' ? (cached.tickerSync ?? null) : null,
        tickerConfig: (() => {
          const t = d.groupId ? tickerByGroup[d.groupId] : null;
          if (!t || !t.enabled) return null;
          const order = JSON.parse(t.deviceOrder || '[]');
          const deviceIndex = order.indexOf(d.id);
          const totalDevices = order.length || 1;
          return { enabled: true, text: t.text, mode: t.mode, position: t.position, speed: t.speed, bgColor: t.bgColor, bgOpacity: t.bgOpacity, textColor: t.textColor, deviceIndex: deviceIndex < 0 ? 0 : deviceIndex, totalDevices, fontSize: t.fontSize || 48, fontBold: t.fontBold || false };
        })(),
      };
    });
    console.log(`[API] 기기 목록 조회 요청됨. 현재 기기 수: ${devices.length}대`);
    res.json(result);
  } catch (err) {
    console.error('[API] 기기 목록 조회 에러:', err);
    res.status(500).json({ error: '기기 조회 실패' });
  }
});

// 기기 순서 변경
app.post('/api/devices/reorder', async (req, res) => {
  const { deviceIds } = req.body; // [id1, id2, id3, ...]
  try {
    await prisma.$transaction(
      deviceIds.map((id, index) => 
        prisma.device.update({
          where: { id },
          data: { order: index }
        })
      )
    );
    res.json({ success: true });
  } catch (err) {
    console.error('[API] 기기 순서 변경 에러:', err);
    res.status(500).json({ error: '기기 순서 변경 실패' });
  }
});

// 특정 기기 조회 (player 부팅 시 자신의 groupId + 스케줄 확인용)
app.get('/api/devices/:id', async (req, res) => {
  try {
    const device = await prisma.device.findUnique({
      where: { id: req.params.id },
      include: { group: true, store: true }
    });
    if (!device) return res.status(404).json({ error: '기기를 찾을 수 없습니다.' });
    // 이 기기에 적용되는 스케줄 (전체 null + 사업장 전용 + 기기 전용)
    const schedules = await prisma.screenSchedule.findMany({
      where: {
        enabled: true,
        OR: [
          { storeId: null,           deviceId: null },
          { storeId: device.storeId, deviceId: null },
          {                          deviceId: req.params.id },
        ]
      }
    });
    // 그룹 자막 설정 + 이 기기의 물리 순서(deviceIndex) 계산
    let tickerConfig = null;
    if (device.groupId) {
      const ticker = await prisma.ticker.findUnique({ where: { groupId: device.groupId } });
      if (ticker && ticker.enabled) {
        const order = JSON.parse(ticker.deviceOrder || '[]');
        const deviceIndex = order.indexOf(device.id);
        tickerConfig = {
          enabled:      ticker.enabled,
          mode:         ticker.mode,
          text:         ticker.text,
          speed:        ticker.speed,
          direction:    ticker.direction,
          position:     ticker.position,
          fontFamily:   ticker.fontFamily,
          fontSize:     ticker.fontSize,
          fontBold:     ticker.fontBold,
          fontItalic:   ticker.fontItalic,
          textColor:    ticker.textColor,
          bgColor:      ticker.bgColor,
          bgOpacity:    ticker.bgOpacity,
          deviceIndex:  deviceIndex >= 0 ? deviceIndex : 0,
          totalDevices: order.length > 0 ? order.length : 1,
        };
      }
    }
    res.json({ ...device, schedules, tickerConfig });
  } catch (err) {
    res.status(500).json({ error: '기기 조회 실패' });
  }
});

// 기기 수동 등록 (물리적 주소/ID 기반)
app.post('/api/devices', async (req, res) => {
  const { id, name, storeId } = req.body; // id = MAC Address or serial
  try {
    const device = await prisma.device.upsert({
      where: { id: id },
      update: { storeId, name },
      create: { id, name, storeId }
    });
    io.emit('group_assignment_changed', { deviceId: device.id, storeId });
    res.json(device);
  } catch (err) {
    res.status(500).json({ error: '기기 등록 실패' });
  }
});

// 기기 그룹/스토어 변경
app.put('/api/devices/:id/group', async (req, res) => {
  const { groupId, storeId } = req.body;
  try {
    const device = await prisma.device.update({
      where: { id: req.params.id },
      data: { 
        groupId: groupId !== undefined ? groupId : undefined,
        storeId: storeId !== undefined ? storeId : undefined
      }
    });
    io.emit('group_assignment_changed', { deviceId: device.id, groupId, storeId });
    res.json(device);
  } catch (err) {
    res.status(500).json({ error: 'DB 업데이트 실패' });
  }
});

// 기기 삭제
app.delete('/api/devices/:id', async (req, res) => {
  try {
    const { id } = req.params;
    await prisma.device.delete({
      where: { id: id }
    });
    console.log(`[API] 기기 삭제 완료: ${id}`);
    io.emit('group_assignment_changed'); // 목록 갱신 트리거
    res.json({ success: true });
  } catch (err) {
    console.error('[API] 기기 삭제 에러:', err);
    res.status(500).json({ error: '기기 삭제 실패', details: err.message });
  }
});

// --- REST API: 미디어 및 재생목록 ---

// 1. 미디어 업로드 및 생성
app.post('/api/media', (req, res, next) => {
  upload.single('file')(req, res, (err) => {
    if (err) {
      if (err.code === 'LIMIT_FILE_SIZE') {
        return res.status(413).json({ error: '파일이 너무 큽니다. 최대 2GB까지 업로드 가능합니다.' });
      }
      return res.status(400).json({ error: err.message || '업로드 오류' });
    }
    next();
  });
}, async (req, res) => {
  if (!req.file) return res.status(400).json({ error: '파일이 없습니다.' });

  const type = req.file.mimetype.startsWith('video/') ? 'video' : 'image';

  try {
    // SHA-256 해시 계산 (player 캐시 검증용)
    const hash = await new Promise((resolve, reject) => {
      const h = crypto.createHash('sha256');
      fs.createReadStream(req.file.path)
        .on('data', chunk => h.update(chunk))
        .on('end', () => resolve(h.digest('hex')))
        .on('error', reject);
    });

    // R2 업로드 시도 (설정 있으면 R2, 없으면 로컬 경로 사용)
    const r2Url = await uploadToR2(req.file.path, req.file.filename, req.file.mimetype);
    const mediaPath = r2Url || `/uploads/${req.file.filename}`;

    // R2 업로드 성공 시 로컬 임시 파일 삭제
    if (r2Url) fs.unlink(req.file.path, () => {});

    const media = await prisma.media.create({
      data: {
        filename: req.file.originalname,
        path: mediaPath,
        type,
        size: req.file.size,
        hash,
        storeId: req.body.storeId || null
      }
    });
    res.json(media);
  } catch (err) {
    if (req.file?.path) fs.unlink(req.file.path, () => {});
    console.error('[API] 미디어 저장 실패:', err.message);
    res.status(500).json({ error: `미디어 저장 실패: ${err.message}` });
  }
});

// 2. 미디어 목록 조회 (사업장별)
// 단말 시각 동기용 — 5대 보드가 같은 서버 시각을 기준으로 동기 재생
app.get('/api/time', (req, res) => {
  res.json({ epochMs: Date.now() });
});

app.get('/api/diagnostics/sockets', (req, res) => {
  const sockets = Array.from(io.sockets.sockets.values());
  const connectedDevices = sockets.map(s => ({
    id: s.id,
    deviceId: s.deviceId || null,
    address: s.handshake.address,
    rooms: Array.from(s.rooms)
  }));
  res.json({
    totalConnected: connectedDevices.length,
    devices: connectedDevices
  });
});

// OTA 배포 현황 상세 진단 — 기기별 현재 버전 vs 목표 버전 비교
app.get('/api/diagnostics/ota', async (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  const meta = loadDeployMeta();
  const targetVersion = meta.apkVersion || null;
  const threshold = new Date(Date.now() - HEARTBEAT_TIMEOUT_MS);

  try {
    const devices = await prisma.device.findMany({ orderBy: { name: 'asc' } });
    const deviceStatus = await Promise.all(devices.map(async dev => {
      const raw = dev.appVersion || '';
      const currentVer = raw.includes(' (') ? raw.slice(0, raw.indexOf(' (')) : raw;
      const isOnline = !!(dev.lastSeen && new Date(dev.lastSeen) >= threshold);
      const room = `device:${dev.id}`;
      const socketsInRoom = await io.in(room).allSockets();
      const socketConnected = socketsInRoom.size > 0;
      const needsUpdate = !!(targetVersion && currentVer && currentVer !== targetVersion);
      return {
        id: dev.id,
        name: dev.name,
        appVersion: raw || null,
        currentVer: currentVer || null,
        targetVersion,
        needsUpdate,
        isOnline,
        socketConnected,
        socketCount: socketsInRoom.size,
        ip: dev.ip || null,
        lastSeen: dev.lastSeen || null,
      };
    }));

    const summary = {
      total: devices.length,
      upToDate: deviceStatus.filter(d => d.currentVer && d.currentVer === targetVersion).length,
      outdated: deviceStatus.filter(d => d.needsUpdate).length,
      unknown: deviceStatus.filter(d => !d.currentVer).length,
    };

    res.json({
      apkAvailable: fs.existsSync(apkPath),
      targetVersion,
      lastDeployedAt: lastDeployedAt || null,
      summary,
      devices: deviceStatus,
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/diagnostics/logs', (req, res) => {
  const logDir = path.join(os.homedir(), '.pm2', 'logs');
  const outLogPath = path.join(logDir, 'signagepro-out.log');
  const errorLogPath = path.join(logDir, 'signagepro-error.log');
  const limit = parseInt(req.query.limit) || 300;

  const readLastLines = (filePath, maxLines = 100) => {
    try {
      if (!fs.existsSync(filePath)) return `[Log file does not exist: ${filePath}]`;
      const content = fs.readFileSync(filePath, 'utf8');
      const lines = content.split('\n');
      return lines.slice(-maxLines).join('\n');
    } catch (e) {
      return `[Error reading log ${filePath}: ${e.message}]`;
    }
  };

  res.json({
    outLog: readLastLines(outLogPath, limit),
    errorLog: readLastLines(errorLogPath, limit)
  });
});

app.get('/api/media', async (req, res) => {
  const { storeId } = req.query;
  const where = storeId ? { storeId } : {};
  const media = await prisma.media.findMany({ where, orderBy: { createdAt: 'desc' } });
  res.json(media);
});

// 2.3 미디어 전체 삭제 (사업장별)
app.delete('/api/media', async (req, res) => {
  const { storeId } = req.query;
  const where = storeId ? { storeId } : {};
  console.log(`[API] 미디어 삭제 요청 (storeId=${storeId || '전체'})`);
  
  try {
    const medias = await prisma.media.findMany({ where });
    
    // 1. PlaylistMedia에서 참조 삭제 (Foreign Key 제약 조건 해결)
    const mediaIds = medias.map(m => m.id);
    await prisma.playlistMedia.deleteMany({
      where: { mediaId: { in: mediaIds } }
    });
    
    // 2. 파일 삭제 (R2 또는 로컬)
    for (const media of medias) {
      if (media.path.startsWith('http')) {
        const key = media.path.split('/').pop();
        await deleteFromR2(key);
      } else {
        const filePath = path.join(__dirname, media.path);
        if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
      }
    }
    
    // 3. DB에서 미디어 삭제
    const deleteResult = await prisma.media.deleteMany({ where });
    
    res.json({ success: true, count: deleteResult.count });
  } catch (err) {
    console.error('[API] 전체 삭제 에러:', err);
    res.status(500).json({ error: '전체 삭제 실패', details: err.message });
  }
});

// 2.5 미디어 삭제
app.delete('/api/media/:id', async (req, res) => {
  try {
    const media = await prisma.media.findUnique({ where: { id: req.params.id } });
    if (!media) return res.status(404).json({ error: '미디어를 찾을 수 없습니다.' });

    // 1. PlaylistMedia에서 참조 삭제
    await prisma.playlistMedia.deleteMany({
      where: { mediaId: media.id }
    });

    // 2. DB에서 미디어 삭제
    await prisma.media.delete({ where: { id: media.id } });
    
    // 3. R2 또는 로컬 파일 삭제
    if (media.path.startsWith('http')) {
      const key = media.path.split('/').pop();
      await deleteFromR2(key);
    } else {
      const filePath = path.join(__dirname, media.path);
      if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
    }
    res.json({ success: true });
  } catch (err) {
    console.error('[API] 미디어 삭제 에러:', err);
    res.status(500).json({ error: '미디어 삭제 실패', details: err.message });
  }
});

// 3. 특정 그룹의 재생목록 조회
app.get('/api/groups/:groupId/playlist', async (req, res) => {
  const { groupId } = req.params;
  const [playlist, ticker] = await Promise.all([
    prisma.playlist.findFirst({
      where: { groupId },
      include: { medias: { include: { media: true }, orderBy: { order: 'asc' } } }
    }),
    prisma.ticker.findUnique({ where: { groupId } }),
  ]);

  const tickerConfig = (ticker && ticker.enabled) ? {
    enabled:    true,
    text:       ticker.text,
    mode:       ticker.mode,
    speed:      ticker.speed,
    direction:  ticker.direction,
    position:   ticker.position,
    fontSize:   ticker.fontSize,
    fontFamily: ticker.fontFamily,
    fontBold:   ticker.fontBold,
    fontItalic: ticker.fontItalic,
    textColor:  ticker.textColor,
    bgColor:    ticker.bgColor,
    bgOpacity:  ticker.bgOpacity,
    deviceOrder:  ticker.deviceOrder, // 기기가 자신의 index를 직접 계산
    repeatCount:  ticker.repeatCount ?? 0,
  } : { enabled: false };

  res.json({ ...(playlist || { medias: [] }), tickerConfig });
});

// 4. 그룹 재생목록 저장 (덮어쓰기 방식)
app.post('/api/groups/:groupId/playlist', async (req, res) => {
  const { groupId } = req.params;
  const { items } = req.body; // items: [{ mediaId, duration }]

  try {
    // 트랜잭션으로 안전하게 덮어쓰기
    const result = await prisma.$transaction(async (tx) => {
      let playlist = await tx.playlist.findFirst({ where: { groupId } });
      
      // 재생목록이 없으면 생성
      if (!playlist) {
        playlist = await tx.playlist.create({
          data: { name: 'Default Playlist', groupId }
        });
      }

      // 기존 연결(PlaylistMedia) 전체 삭제
      await tx.playlistMedia.deleteMany({
        where: { playlistId: playlist.id }
      });

      // 새 연결 데이터 삽입
      if (items && items.length > 0) {
        const createData = items.map((item, idx) => ({
          playlistId: playlist.id,
          mediaId: item.mediaId,
          order: idx,
          duration: item.duration || 10,
          targetDeviceId: item.targetDeviceId || null,
          transition: item.transition || 'dissolve',
          transitionTime: item.transitionTime || 1000,
          slideDirection: item.slideDirection || 'right'
        }));
        await tx.playlistMedia.createMany({ data: createData });
      }

      return playlist;
    });

    // 보드들에게 재생목록 변경됨을 알림 (Socket.io)
    io.emit('playlist_updated', { groupId });
    
    res.json({ success: true, result });
  } catch (err) {
    res.status(500).json({ error: '재생목록 저장 실패' });
  }
});

// --- 자막(Ticker) API ---

// 그룹 자막 조회
app.get('/api/groups/:groupId/ticker', async (req, res) => {
  const { groupId } = req.params;
  try {
    const ticker = await prisma.ticker.findUnique({ where: { groupId } });
    res.json(ticker || { groupId, enabled: false, mode: 'individual', text: '', speed: 150,
      direction: 'rtl', position: 'bottom', fontFamily: 'NotoSansKR-Regular',
      fontSize: 48, fontBold: false, fontItalic: false, textColor: '#FFFFFF',
      bgColor: '#000000', bgOpacity: 65, deviceOrder: '[]' });
  } catch (err) { res.status(500).json({ error: err.message }); }
});

// 그룹 자막 저장 (upsert)
app.post('/api/groups/:groupId/ticker', async (req, res) => {
  const { groupId } = req.params;
  const data = req.body;
  try {
    const ticker = await prisma.ticker.upsert({
      where:  { groupId },
      update: { ...data, updatedAt: new Date() },
      create: { groupId, ...data },
    });
    // 각 기기에 ticker_config 직접 전송
    const order = JSON.parse(ticker.deviceOrder || '[]');
    if (ticker.enabled) {
      const totalDevices = order.length || 1;
      order.forEach((devId, idx) => {
        io.to(`device:${devId}`).emit('ticker_config', {
          enabled: true,
          text: ticker.text,
          mode: ticker.mode,
          position: ticker.position,
          speed: ticker.speed,
          direction: ticker.direction || 'rtl',
          fontSize: ticker.fontSize || 48,
          fontFamily: ticker.fontFamily || 'NotoSansKR-Regular',
          fontBold: ticker.fontBold || false,
          fontItalic: ticker.fontItalic || false,
          textColor: ticker.textColor || '#FFFFFF',
          bgColor: ticker.bgColor || '#000000',
          bgOpacity: ticker.bgOpacity ?? 65,
          repeatCount: ticker.repeatCount ?? 0,
          deviceIndex: idx,
          totalDevices,
        });
      });
    } else {
      // 끄기: deviceOrder + 그룹 소속 기기 모두에 전송 (order가 비어있어도 동작)
      const group = await prisma.group.findUnique({ where: { id: groupId }, include: { devices: true } });
      const allIds = [...new Set([...order, ...(group?.devices || []).map(d => d.id)])];
      allIds.forEach(devId => {
        io.to(`device:${devId}`).emit('ticker_config', { enabled: false });
      });
    }
    // Android refreshPlaylist() + 대시보드 양쪽 트리거
    io.emit('playlist_updated', { groupId });
    io.emit('ticker_updated', { groupId });
    res.json(ticker);
  } catch (err) { res.status(500).json({ error: err.message }); }
});

// --- OTA 업데이트 ---

const updateDir = path.join(__dirname, 'update');
if (!fs.existsSync(updateDir)) fs.mkdirSync(updateDir, { recursive: true });

// 배포 메타 (마지막 배포 시각) — 서버 재시작 후에도 유지
const deployMetaPath = path.join(updateDir, 'deploy-meta.json');
function loadDeployMeta() {
  try {
    if (fs.existsSync(deployMetaPath)) {
      return JSON.parse(fs.readFileSync(deployMetaPath, 'utf8'));
    }
  } catch (e) { console.warn('[OTA] deploy-meta.json 읽기 실패:', e.message); }
  return {};
}
function saveDeployMeta(data) {
  try {
    const current = loadDeployMeta();
    fs.writeFileSync(deployMetaPath, JSON.stringify({ ...current, ...data }), 'utf8');
  } catch (e) { console.warn('[OTA] deploy-meta.json 저장 실패:', e.message); }
}

// APK 파일 서빙 (server/update/app.apk 를 이 경로에 놓으면 됨)
app.get('/update/apk', (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) {
    return res.status(404).json({ error: 'APK 없음. server/update/app.apk 를 배치하세요.' });
  }
  res.download(apkPath, 'signagepro-player.apk');
});

// APK 배포 상태 확인
// APK 업로드 (대시보드에서 직접 업로드) — busboy 직접 사용 (multer+Express5 호환 이슈 우회)
app.post('/api/update/apk', (req, res) => {
  let bb;
  try {
    bb = Busboy({ headers: req.headers, limits: { fileSize: 500 * 1024 * 1024 } });
  } catch (e) {
    return res.status(400).json({ error: '잘못된 요청입니다: ' + e.message });
  }

  const destPath = path.join(updateDir, 'app.apk');
  const tmpPath  = destPath + '.tmp';
  let writePromise = null; // 디스크 쓰기 완료 Promise
  let hasFile = false;
  let filterError = null;
  let origName = '';

  bb.on('file', (fieldname, file, info) => {
    const { filename = '', mimeType = '' } = info;
    origName = filename;
    const ok = filename.toLowerCase().endsWith('.apk')
      || mimeType === 'application/vnd.android.package-archive'
      || mimeType === 'application/octet-stream';

    if (!ok) {
      file.resume();
      filterError = 'APK 파일만 업로드 가능합니다.';
      return;
    }

    hasFile = true;
    // 임시 파일에 먼저 쓰고, 완료 후 rename → 기존 APK 손상 방지
    const dest = fs.createWriteStream(tmpPath);
    file.pipe(dest);

    writePromise = new Promise((resolve, reject) => {
      dest.on('finish', resolve);
      dest.on('error', reject);
      file.on('error', reject);
    });
  });

  bb.on('finish', () => {
    if (filterError) return res.status(400).json({ error: filterError });
    if (!hasFile)    return res.status(400).json({ error: '파일이 없습니다.' });

    // busboy 'finish'는 디스크 쓰기 완료보다 먼저 올 수 있으므로 Promise 대기
    Promise.resolve(writePromise)
      .then(() => {
        // 임시 파일 → 최종 파일로 rename (원자적 교체)
        try {
          fs.renameSync(tmpPath, destPath);
        } catch (_) {
          fs.copyFileSync(tmpPath, destPath);
          try { fs.unlinkSync(tmpPath); } catch (_) {}
        }
        const stat = fs.statSync(destPath);
        console.log(`[OTA] APK 업로드 완료: ${(stat.size / 1024 / 1024).toFixed(1)} MB`);
        const verMatch = (origName || '').match(/(\d+\.\d+\.\d+(?:\.\d+)?)/);
        const apkVersion = verMatch ? verMatch[1] : null;
        // 업로드 시에는 pendingVersion만 저장 — apkVersion(Watchdog 트리거)은 Push 시점에 확정
        if (apkVersion) saveDeployMeta({ pendingVersion: apkVersion });
        if (!res.headersSent) res.json({ ok: true, size: stat.size, updatedAt: stat.mtime, apkVersion });
      })
      .catch(err => {
        try { fs.unlinkSync(tmpPath); } catch (_) {}
        console.error('[OTA] APK 저장 실패:', err.message);
        if (!res.headersSent) res.status(500).json({ error: '저장 실패: ' + err.message });
      });
  });

  bb.on('error', (err) => {
    try { fs.unlinkSync(tmpPath); } catch (_) {}
    console.error('[OTA] APK 업로드 에러:', err.message);
    if (!res.headersSent) res.status(500).json({ error: err.message });
  });

  req.pipe(bb);
});

let deployMeta = loadDeployMeta();
let lastDeployedAt = deployMeta.lastDeployedAt ? new Date(deployMeta.lastDeployedAt) : null;

app.get('/api/update/status', (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) {
    return res.json({ available: false });
  }
  const stat = fs.statSync(apkPath);
  const meta = loadDeployMeta();
  res.json({ available: true, size: stat.size, updatedAt: stat.mtime, lastDeployedAt, apkVersion: meta.apkVersion || null, pendingVersion: meta.pendingVersion || null });
});

// APK 삭제 (배포 취소)
app.delete('/api/update/apk', (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) return res.status(404).json({ error: 'APK 없음' });
  try {
    fs.unlinkSync(apkPath);
    lastDeployedAt = null;
    saveDeployMeta({ apkVersion: null, pendingVersion: null, lastDeployedAt: null });
    console.log('[OTA] app.apk 삭제됨');
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// 단말에 업데이트 푸시 (deviceId 없으면 전체 배포)
app.post('/api/update/push', (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) {
    return res.status(404).json({ error: 'server/update/app.apk 가 없습니다.' });
  }
  const { deviceId } = req.body;
  const payload = { url: '/update/apk', deviceId: deviceId || '' };
  io.emit('update_apk', payload);
  lastDeployedAt = new Date();
  // Push 시점에 pendingVersion → apkVersion 확정 (이 시점부터 Watchdog/재접속 자동복구 활성화)
  const staged = loadDeployMeta();
  const committedVersion = staged.pendingVersion || staged.apkVersion || null;
  saveDeployMeta({ apkVersion: committedVersion, pendingVersion: null, lastDeployedAt: lastDeployedAt.toISOString() });
  console.log(`[OTA] 업데이트 푸시: ${deviceId ? deviceId : '전체 단말'} (v${committedVersion})`);

  // ── [Self-Healing] 배포 60초 후 좀비 구버전 자동 깨우기 타이머 가동 ──────────────────────
  setTimeout(async () => {
    try {
      const meta = loadDeployMeta();
      const targetVersion = meta.apkVersion;
      if (!targetVersion) return;

      console.log(`[Self-Healing] 배포 60초 경과 - 좀비 기기 스캔 개시... (목표 버전: v${targetVersion})`);
      
      // 대상 기기 조회 (지정 기기 또는 온라인 상태 기기 전체)
      let targetDevices = [];
      if (deviceId) {
        const d = await prisma.device.findUnique({ where: { id: deviceId } });
        if (d) targetDevices = [d];
      } else {
        targetDevices = await prisma.device.findMany({ where: { status: 'online' } });
      }

      for (const dev of targetDevices) {
        // DB에 보고된 현재 버전 파싱 ("0.4.5 (2026-05-29 16:46)" -> "0.4.5")
        const currentVerStr = dev.appVersion || '';
        const parenIdx = currentVerStr.indexOf(' (');
        const currentVer = parenIdx >= 0 ? currentVerStr.slice(0, parenIdx) : currentVerStr;

        if (currentVer !== targetVersion) {
          console.log(`[Self-Healing] 기기 ${dev.id}가 여전히 구버전(${currentVer || 'unknown'}) 상태. update_apk 재발송 + restart_app 폴백 예약`);
          const room = `device:${dev.id}`;
          // 1순위: update_apk 재발송 — 신버전 앱은 이것으로 OTA 재시도
          io.to(room).emit('update_apk', { url: '/update/apk', deviceId: dev.id });
          console.log(`[Self-Healing] ${dev.id}: update_apk 재발송 완료`);
          // 2순위: 3초 후 restart_app 폴백 — 구버전 앱(update_apk 미지원) 복구용
          setTimeout(() => {
            io.to(room).emit('restart_app', { deviceId: dev.id });
            console.log(`[Self-Healing] ${dev.id}: restart_app 폴백 전송 완료`);
          }, 3000);
        } else {
          console.log(`[Self-Healing] 기기 ${dev.id}는 정상적으로 최신 버전(v${targetVersion})이 갱신되었습니다.`);
        }
      }
    } catch (e) {
      console.error('[Self-Healing] 백그라운드 점검 에러:', e.message);
    }
  }, 60000); // 60초 후 동작

  res.json({ success: true, pushed: deviceId || 'all' });
});

// ADB over WiFi 직접 설치 (서버 PC에서 adb install 실행 → 확인창 없음)
// 전제: 단말에서 ADB TCP 모드(5555)가 활성화되어 있어야 함
app.post('/api/update/adb-install', async (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) {
    return res.status(404).json({ error: 'server/update/app.apk 가 없습니다.' });
  }

  const { deviceId, deviceIds } = req.body;
  const adbPath = GLOBAL_ADB_PATH;

  // deviceIds 배열 > deviceId 단일 > 전체(ip 있는 기기)
  let devices = [];
  try {
    if (deviceIds?.length) {
      devices = await prisma.device.findMany({ where: { id: { in: deviceIds }, ip: { not: null } } });
      // 요청 순서 유지
      devices.sort((a, b) => deviceIds.indexOf(a.id) - deviceIds.indexOf(b.id));
    } else if (deviceId) {
      const d = await prisma.device.findUnique({ where: { id: deviceId } });
      if (d?.ip) devices = [d];
    } else {
      devices = await prisma.device.findMany({ where: { ip: { not: null } } });
    }
  } catch (e) {
    return res.status(500).json({ error: 'DB 조회 실패' });
  }

  if (devices.length === 0) {
    return res.status(404).json({ error: '설치 대상 기기(IP 있는 온라인 기기)가 없습니다.' });
  }

  adbCancelled = false;
  const results = [];
  adbInstallStatus = { running: true, results: null, startedAt: Date.now(), deviceIds: devices.map(d => d.id) };

  const trackExec = (cmd, cmdArgs, opts = {}) => new Promise((resolve) => {
    const proc = execFile(cmd, cmdArgs, { windowsHide: true, ...opts }, (err, stdout, stderr) => {
      activeAdbProcs.delete(proc);
      resolve({ err, stdout, stderr });
    });
    activeAdbProcs.add(proc);
  });

  const emitProgress = (deviceId, stage, pct, message) => {
    io.emit('adb_install_progress', { deviceId, stage, pct, message });
  };

  for (const device of devices) {
    if (adbCancelled) {
      emitProgress(device.id, 'cancelled', 0, '취소됨');
      results.push({ deviceId: device.id, ip: device.ip, success: false, output: '취소됨' });
      continue;
    }
    const target = `${device.ip}:5555`;
    try {
      // 1. adb connect (10%)
      emitProgress(device.id, 'connecting', 10, 'ADB 연결 중…');
      const { stdout: cs } = await trackExec(adbPath, ['connect', target], { timeout: 8000 });
      console.log(`[ADB] connect ${target}: ${cs?.trim()}`);

      if (adbCancelled) {
        emitProgress(device.id, 'cancelled', 0, '취소됨');
        results.push({ deviceId: device.id, ip: device.ip, success: false, output: '취소됨' });
        continue;
      }

      // 2. adb install -r (10% → 80%)
      emitProgress(device.id, 'installing', 30, 'APK 전송 및 설치 중…');
      const { err, stdout, stderr } = await trackExec(
        adbPath, ['-s', target, 'install', '-r', apkPath], { timeout: 180000 }
      );
      const out = ((stdout || '') + (stderr || '')).trim();
      console.log(`[ADB] install ${target}: ${out}`);
      const installOk = !err && /success/i.test(out);

      // 3. 설치 성공 시 전체화면 안내 팝업 억제 + 앱 자동 실행 (80% → 100%)
      if (installOk && !adbCancelled) {
        emitProgress(device.id, 'finalizing', 85, '앱 재시작 중…');
        await trackExec(
          adbPath, ['-s', target, 'shell', 'settings', 'put', 'secure', 'immersive_mode_confirmations', 'confirmed'],
          { timeout: 5000 }
        );
        await new Promise(r => setTimeout(r, 3000)); // 앱 완전 종료 대기
        const { stdout: as } = await trackExec(
          adbPath, ['-s', target, 'shell', 'am', 'start', '-n', 'com.signagepro.player/.MainActivity'],
          { timeout: 10000 }
        );
        console.log(`[ADB] am start ${target}: ${as?.trim()}`);
      }
      emitProgress(device.id, installOk ? 'success' : 'failed', 100, installOk ? '✅ 완료' : `❌ ${out}`);
      results.push({ deviceId: device.id, ip: device.ip, success: installOk, output: out });
    } catch (e) {
      emitProgress(device.id, 'failed', 100, `❌ ${e.message}`);
      results.push({ deviceId: device.id, ip: device.ip, success: false, output: e.message });
    }
  }

  adbInstallStatus = { running: false, results, startedAt: adbInstallStatus.startedAt };
  if (results.some(r => r.success)) { lastDeployedAt = new Date(); saveDeployMeta(lastDeployedAt); }
  res.json({ results });
});

// ADB 설치 상태 (페이지 이탈 후 복귀 시 복원용)
let adbCancelled = false;
const activeAdbProcs = new Set();
let adbInstallStatus = { running: false, results: null, startedAt: null };

// ADB 설치 상태 조회
app.get('/api/update/adb-status', (req, res) => {
  // 3분 이상 된 running 상태는 비정상 종료로 간주하고 자동 초기화
  if (adbInstallStatus.running && adbInstallStatus.startedAt) {
    const elapsed = Date.now() - adbInstallStatus.startedAt;
    if (elapsed > 3 * 60 * 1000) {
      adbInstallStatus = { running: false, results: null, startedAt: null };
    }
  }
  res.json(adbInstallStatus);
});

app.post('/api/update/adb-cancel', (req, res) => {
  adbCancelled = true;
  activeAdbProcs.forEach(p => { try { p.kill(); } catch (_) {} });
  activeAdbProcs.clear();
  adbInstallStatus = { running: false, results: null, startedAt: null };
  console.log('[ADB] 설치 취소 요청');
  res.json({ ok: true });
});

// 기기 원격 재부팅 (소켓 기반 + ADB 폴백)
app.post('/api/devices/:id/reboot', async (req, res) => {
  const device = await prisma.device.findUnique({ where: { id: req.params.id } });
  if (!device) return res.status(404).json({ error: '기기를 찾을 수 없습니다.' });

  // lastSeen 기반으로 온라인 여부 재계산 (DB status는 sweep 타이밍에 따라 stale할 수 있음)
  const isOnline = device.lastSeen && (Date.now() - new Date(device.lastSeen).getTime()) <= HEARTBEAT_TIMEOUT_MS;
  if (!isOnline) {
    return res.status(400).json({ error: '기기가 오프라인입니다.\n전원과 네트워크 연결을 확인해주세요.' });
  }

  const room = `device:${device.id}`;
  try {
    const socketsInRoom = await io.in(room).allSockets();

    if (socketsInRoom.size > 0) {
      // v0.4.6+: reboot_device 처리 가능
      const rawVer = device.appVersion || '';
      const ver = rawVer.includes(' (') ? rawVer.slice(0, rawVer.indexOf(' (')) : rawVer;
      const parts = ver.split('.').map(Number);
      const supportsReboot = parts[0] > 0 || parts[1] > 4 || (parts[1] === 4 && (parts[2] || 0) >= 6);

      if (supportsReboot) {
        io.to(room).emit('prepare_reboot', { deviceId: device.id });
        await new Promise(r => setTimeout(r, 600));
        io.to(room).emit('reboot_device', { deviceId: device.id });
        console.log(`[Reboot] Socket.io 재부팅 명령 전송: ${device.id} (v${ver})`);
      } else {
        // v0.4.5 이하: reboot_device 미지원 → update_apk 강제 재시도로 설치 후 프로세스 종료 유도
        const apkPath = path.join(updateDir, 'app.apk');
        if (fs.existsSync(apkPath)) {
          io.to(room).emit('update_apk', { url: '/update/apk', deviceId: device.id });
          console.log(`[Reboot] v0.4.5 폴백: update_apk 재전송 → ${device.id} (v${ver})`);
        } else {
          return res.status(400).json({ error: `기기 앱(v${ver})이 원격 재부팅을 지원하지 않습니다. 현장에서 전원을 재시작해 주세요.` });
        }
      }
    } else if (device.ip) {
      // Socket.io 룸 비어있음 → ADB 직접 재부팅 폴백 (v0.4.5 이하 또는 소켓 미연결 기기)
      const target = `${device.ip}:5555`;
      console.log(`[Reboot] Socket.io 룸 없음 → ADB 폴백 재부팅: ${device.id} (${target})`);
      try {
        await adbExec(GLOBAL_ADB_PATH, ['connect', target], { timeout: 8000 });
        await adbExec(GLOBAL_ADB_PATH, ['-s', target, 'shell', 'reboot'], { timeout: 10000 });
        console.log(`[Reboot] ADB 재부팅 명령 전송 완료: ${device.id}`);
      } catch (adbErr) {
        console.warn(`[Reboot] ADB 폴백 실패 (${device.id}): ${adbErr.message}`);
        return res.status(500).json({ error: `소켓 미연결 + ADB 재부팅 실패: ${adbErr.message}` });
      }
    } else {
      return res.status(400).json({ error: '소켓 미연결 상태이며 기기 IP도 없습니다.' });
    }

    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── 원격 제어 공통 헬퍼 ──────────────────────────────────────────────────────
function adbExec(adbPath, args, opts = {}) {
  return new Promise((resolve, reject) => {
    execFile(adbPath, args, { timeout: 15000, windowsHide: true, ...opts }, (err, stdout, stderr) => {
      if (err) return reject(err);
      resolve({ stdout, stderr });
    });
  });
}

async function getDeviceTarget(id) {
  const device = await prisma.device.findUnique({ where: { id } });
  if (!device || !device.ip) throw new Error('기기 또는 IP 정보 없음');
  return { device, target: `${device.ip}:5555` };
}

// 원격 스크린샷 (screencap → pull → PNG 반환)
app.post('/api/devices/:id/screenshot', async (req, res) => {
  const adbPath = process.env.ADB_PATH || 'adb';
  try {
    const { target } = await getDeviceTarget(req.params.id);
    const tmpFile = path.join(os.tmpdir(), `snap_${req.params.id}.png`);
    await adbExec(adbPath, ['-s', target, 'shell', 'screencap', '-p', '/sdcard/snap_signage.png']);
    await adbExec(adbPath, ['-s', target, 'pull', '/sdcard/snap_signage.png', tmpFile]);
    const data = fs.readFileSync(tmpFile);
    fs.unlink(tmpFile, () => {});
    res.set('Content-Type', 'image/png');
    res.send(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// 앱 재시작
app.post('/api/devices/:id/restart-app', async (req, res) => {
  try {
    const device = await prisma.device.findUnique({ where: { id: req.params.id } });
    if (!device) return res.status(404).json({ error: '기기를 찾을 수 없습니다.' });

    const isOnline = device.lastSeen && (Date.now() - new Date(device.lastSeen).getTime()) <= HEARTBEAT_TIMEOUT_MS;
    if (!isOnline) {
      return res.status(400).json({ error: '기기가 오프라인입니다.' });
    }

    const room = `device:${device.id}`;
    const socketsInRoom = await io.in(room).allSockets();

    if (socketsInRoom.size > 0) {
      io.to(room).emit('restart_app', { deviceId: device.id });
      console.log(`[Restart] Socket.io 앱 재시작 명령 전송: ${device.id} (소켓 ${socketsInRoom.size}개)`);
    } else if (device.ip) {
      // ADB 폴백 — 앱 강제 재시작 (v0.4.5 이하 또는 소켓 미연결)
      const target = `${device.ip}:5555`;
      console.log(`[Restart] Socket.io 룸 없음 → ADB 폴백 앱 재시작: ${device.id} (${target})`);
      try {
        await adbExec(GLOBAL_ADB_PATH, ['connect', target], { timeout: 8000 });
        await adbExec(GLOBAL_ADB_PATH, ['-s', target, 'shell', 'am', 'force-stop', 'com.signagepro.player'], { timeout: 8000 });
        await new Promise(r => setTimeout(r, 1000));
        await adbExec(GLOBAL_ADB_PATH, ['-s', target, 'shell', 'am', 'start', '-n', 'com.signagepro.player/.MainActivity'], { timeout: 8000 });
        console.log(`[Restart] ADB 앱 재시작 완료: ${device.id}`);
      } catch (adbErr) {
        console.warn(`[Restart] ADB 폴백 실패 (${device.id}): ${adbErr.message}`);
        return res.status(500).json({ error: `소켓 미연결 + ADB 재시작 실패: ${adbErr.message}` });
      }
    } else {
      return res.status(400).json({ error: '소켓 미연결 상태이며 기기 IP도 없습니다.' });
    }

    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// 디버그용 쉘 명령어 실행
app.post('/api/debug/run-cmd', async (req, res) => {
  try {
    const { deviceId, cmd, su = false } = req.body;
    if (!deviceId || !cmd) {
      return res.status(400).json({ error: 'deviceId and cmd are required' });
    }
    const device = await prisma.device.findUnique({ where: { id: deviceId } });
    if (!device) return res.status(404).json({ error: '기기를 찾을 수 없습니다.' });

    const isOnline = device.lastSeen && (Date.now() - new Date(device.lastSeen).getTime()) <= HEARTBEAT_TIMEOUT_MS;
    if (!isOnline) {
      return res.status(400).json({ error: '기기가 오프라인입니다.' });
    }

    const room = `device:${device.id}`;
    const socketsInRoom = await io.in(room).allSockets();
    if (socketsInRoom.size === 0) {
      return res.status(400).json({ error: '기기 소켓 연결이 없습니다.' });
    }

    const key = `${deviceId}:${cmd}`;
    let resolved = false;

    pendingCmdCallbacks.set(key, (output) => {
      if (!resolved) {
        resolved = true;
        res.json({ output });
      }
    });

    io.to(room).emit('run_cmd', { deviceId, cmd, su });

    setTimeout(() => {
      if (!resolved) {
        resolved = true;
        pendingCmdCallbacks.delete(key);
        res.status(504).json({ error: '기기 응답 시간 초과' });
      }
    }, 8000);

  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// 화면 켜기/끄기 — Socket.io 우선, ADB 폴백
app.post('/api/devices/:id/screen', async (req, res) => {
  const deviceId = req.params.id;
  const on = !!req.body.on;
  const payload = { deviceId, on };

  // 1. Socket.io로 기기에 직접 전송
  const room = `device:${deviceId}`;
  const socketsInRoom = await io.in(room).allSockets();
  if (socketsInRoom.size > 0) {
    io.to(room).emit('screen_control', payload);
    console.log(`[Screen] Socket.io 전송: ${deviceId} → ${on ? 'ON' : 'OFF'}`);
    return res.json({ ok: true, method: 'socketio' });
  }

  // 2. ADB 폴백 (같은 네트워크일 때)
  const adbPath = process.env.ADB_PATH || 'adb';
  try {
    const { target } = await getDeviceTarget(deviceId);
    const keycode = on ? '224' : '223';
    await adbExec(adbPath, ['-s', target, 'shell', 'input', 'keyevent', keycode]);
    console.log(`[Screen] ADB 전송: ${deviceId} → ${on ? 'ON' : 'OFF'}`);
    res.json({ ok: true, method: 'adb' });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// 볼륨 조절 (0-15, STREAM_MUSIC) — Socket.io 우선, ADB 폴백
app.post('/api/devices/:id/volume', async (req, res) => {
  const deviceId = req.params.id;
  const level = Math.min(15, Math.max(0, parseInt(req.body.level) || 0));
  const payload = { deviceId, level };

  // 1. Socket.io로 기기에 직접 전송 (빠름, ADB 불필요)
  const room = `device:${deviceId}`;
  const socketsInRoom = await io.in(room).allSockets();
  if (socketsInRoom.size > 0) {
    io.to(room).emit('set_volume', payload);
    console.log(`[Volume] Socket.io 전송: ${deviceId} → ${level}`);
    return res.json({ ok: true, method: 'socketio' });
  }

  // 2. ADB 폴백 (Socket.io 미연결 시)
  const adbPath = process.env.ADB_PATH || 'adb';
  try {
    const { target } = await getDeviceTarget(deviceId);
    await adbExec(adbPath, ['-s', target, 'shell', 'media', 'volume', '--stream', '3', '--set', String(level)]);
    console.log(`[Volume] ADB 전송: ${deviceId} → ${level}`);
    res.json({ ok: true, method: 'adb' });
  } catch (e) {
    res.status(500).json({ error: '볼륨 설정 실패: ' + e.message });
  }
});

// IPv4-mapped IPv6 주소(::ffff:x.x.x.x) → 순수 IPv4 변환
function normalizeIp(ip) {
  if (!ip) return ip;
  return ip.replace(/^::ffff:/i, '');
}

// --- TCP 소켓 서버 (10080 포트, 하트비트 통신용) ---

// socket → deviceId 매핑 (오프라인 감지용)
const socketDeviceMap = new Map();

// deviceId → 오프라인 처리 대기 타이머 (순간 재연결 감지용 debounce)
const offlineTimers = new Map();

async function handleTcpMessage(socket, msg) {
  // 인증: auth:<deviceId>:<secret>
  if (msg.startsWith('auth:')) {
    const [, deviceId, secret] = msg.split(':');
    if (secret !== DEVICE_SECRET) {
      console.log(`[TCP] 인증 실패: deviceId="${deviceId}" secret="${secret}" expected="${DEVICE_SECRET}"`);
      socket.write('err:unauthorized\n');
      socket.destroy();
      return;
    }
    socketDeviceMap.set(socket, deviceId);
    socket.write('auth:ok\n');
    console.log(`[TCP] 인증 성공: ${deviceId}`);
    return;
  }

  // VU 레벨: vu:<deviceId>/<level 0~100> — 300ms 주기, ack 없음
  if (msg.startsWith('vu:')) {
    const parts = msg.substring(3).split('/');
    const deviceId = parts[0];
    const vu = parseInt(parts[1]) || 0;
    if (deviceId) {
      io.emit('device_vu_update', { deviceId, vu });
      const cached = deviceLiveStateCache.get(deviceId) || {};
      cached.vu = vu;
      deviceLiveStateCache.set(deviceId, cached);
    }
    return; // ack 없음
  }

  // 하트비트: status:<deviceId>/cpu:<x>/mem:<x>
  if (msg.startsWith('status:')) {
    const deviceId = socketDeviceMap.get(socket);
    if (!deviceId) {
      socket.write('err:not_authenticated\n');
      return;
    }

    const parts = msg.substring(7).split('/');
    let cpu = null, mem = null, ver = null, dl = null, vol = null, deviceTime = null, slide = null, screen = null, hdmi = null;
    let cpuTemp = null, diskSpace = null, ramSpace = null, tvEdid = null, tvCec = null, stbSpec = null, tickerCycleMs = null;
    parts.forEach(p => {
      if (p.startsWith('cpu:')) cpu = parseFloat(p.substring(4));
      if (p.startsWith('mem:')) mem = parseFloat(p.substring(4));
      if (p.startsWith('ver:')) ver = p.substring(4).trim() || null;
      if (p.startsWith('vol:')) vol = parseInt(p.substring(4));
      if (p.startsWith('time:')) { const t = parseInt(p.substring(5)); if (!isNaN(t) && t > 0) deviceTime = t; }
      if (p.startsWith('dl:')) {
        // 형식: dl:cur/total/pct → 이미 split('/')로 나뉘어 있으므로 다음 두 파트가 total, pct
        // 실제론 dl:cur 로만 파싱됨. 전체 dl 필드는 'dl:1', '3', '67' 세 파트로 분리됨
        // → 인덱스로 처리
      }
      // slide: "<index>|<total>|<filename>" (1-based index, '|' 구분자)
      if (p.startsWith('slide:')) {
        const sp = p.substring(6).split('|');
        if (sp.length >= 2) {
          slide = { index: parseInt(sp[0]) || 0, total: parseInt(sp[1]) || 0, filename: sp[2] || '' };
        }
      }
      // screen: "on" 또는 "off" — 스케줄에 의한 화면 상태
      if (p.startsWith('screen:')) screen = p.substring(7).trim();
      // hdmi: "1" 또는 "0" — HDMI 연결 여부
      if (p.startsWith('hdmi:')) hdmi = p.substring(5).trim();
      if (p.startsWith('temp:')) cpuTemp = parseFloat(p.substring(5));
      if (p.startsWith('disk:')) {
        const dp = p.substring(5).split('_');
        if (dp.length >= 2) diskSpace = { free: parseInt(dp[0]) || 0, total: parseInt(dp[1]) || 0 };
      }
      if (p.startsWith('ram:')) {
        const rp = p.substring(4).split('_');
        if (rp.length >= 2) ramSpace = { free: parseInt(rp[0]) || 0, total: parseInt(rp[1]) || 0 };
      }
      if (p.startsWith('edid:')) {
        const ep = p.substring(5).split('|');
        if (ep.length >= 3) {
          tvEdid = {
            brand: ep[0],
            model: ep[1],
            serial: ep[2],
            maxRes: ep[3] || 'Unknown',
            hdmiVer: ep[4] || 'Unknown'
          };
        }
      }
      if (p.startsWith('cec:')) tvCec = p.substring(4).trim();
      if (p.startsWith('stb:')) {
        const sp = p.substring(4).split('|');
        if (sp.length >= 2) stbSpec = { hdmiVer: sp[0], maxRes: sp[1] };
      }
      if (p.startsWith('ticker:')) { const t = parseInt(p.substring(7)); if (!isNaN(t)) tickerCycleMs = t; }
    });
    // dl: cur/total/pct 가 '/'로 분리되어 parts에 ['dl:1','3','67'] 형태로 들어옴
    const dlIdx = parts.findIndex(p => p.startsWith('dl:'));
    if (dlIdx !== -1) {
      const cur = parseInt(parts[dlIdx].substring(3));
      const total = parseInt(parts[dlIdx + 1] ?? '0');
      const pct = parseInt(parts[dlIdx + 2] ?? '0');
      if (!isNaN(cur) && !isNaN(total) && !isNaN(pct)) dl = { cur, total, pct };
    }
    if (!Number.isFinite(cpu)) cpu = null;
    if (!Number.isFinite(mem)) mem = null;
    if (!Number.isFinite(vol)) vol = null;

    // 실시간 상태 캐시 갱신
    const cached = deviceLiveStateCache.get(deviceId) || {};
    deviceLiveStateCache.set(deviceId, {
      ...cached,
      deviceTime: deviceTime ?? cached.deviceTime,
      slide: slide !== null ? slide : cached.slide,
      dl: dl,
      vol: vol !== null ? vol : cached.vol,
      cpu: cpu ?? cached.cpu,
      mem: mem ?? cached.mem,
      ver: ver ?? cached.ver,
      // screen: "on"/"off" — null이면 이전 값 유지 (구버전 앱 호환)
      screenOff: screen !== null ? (screen === 'off') : (cached.screenOff ?? false),
      // hdmi: "1"/"0" — null이면 이전 값 유지 (구버전 앱 호환)
      hdmiConnected: hdmi !== null ? (hdmi === '1') : (cached.hdmiConnected ?? true),
      cpuTemp: cpuTemp !== null ? cpuTemp : (cached.cpuTemp ?? null),
      diskSpace: diskSpace !== null ? diskSpace : (cached.diskSpace ?? null),
      ramSpace: ramSpace !== null ? ramSpace : (cached.ramSpace ?? null),
      tvEdid: tvEdid !== null ? tvEdid : (cached.tvEdid ?? null),
      tvCec: tvCec !== null ? tvCec : (cached.tvCec ?? null),
      stbSpec: stbSpec !== null ? stbSpec : (cached.stbSpec ?? null),
      // tickerCycleMs=0 → 자막 꺼짐(null로 초기화), null → 미포함(이전값 유지)
      tickerSync: tickerCycleMs === null ? (cached.tickerSync ?? null)
                : tickerCycleMs > 0 ? { cycleMs: tickerCycleMs, ntpMs: deviceTime ?? Date.now(), receivedAt: Date.now() }
                : null
    });

    try {
      await prisma.device.upsert({
        where: { id: deviceId },
        update: { status: 'online', lastSeen: new Date(), ip: normalizeIp(socket.remoteAddress), cpuUsage: cpu, memUsage: mem, ...(ver && { appVersion: ver }) },
        create: { id: deviceId, name: deviceId, status: 'online', lastSeen: new Date(), ip: normalizeIp(socket.remoteAddress), cpuUsage: cpu, memUsage: mem, appVersion: ver }
      });
      const cachedState = deviceLiveStateCache.get(deviceId) || {};
      const { screenOff = false, hdmiConnected = true, cpuTemp = null, diskSpace = null, ramSpace = null, tvEdid = null, tvCec = null, stbSpec = null } = cachedState;
      io.emit('device_status_update', { deviceId, status: 'online', cpu, mem, ip: normalizeIp(socket.remoteAddress), appVersion: ver, dl, vol, deviceTime, slide, screenOff, hdmiConnected, cpuTemp, diskSpace, ramSpace, tvEdid, tvCec, stbSpec });
      socket.write(`ok:${Date.now()}\n`);
    } catch (err) {
      console.error('[TCP] DB 에러:', err);
    }
  }
}

const tcpServer = net.createServer((socket) => {
  console.log(`[TCP] 보드 접속됨: ${socket.remoteAddress}:${socket.remotePort}`);
  let buffer = '';

  // 무응답(전원 차단 등) 감지 — HEARTBEAT_TIMEOUT_MS 동안 데이터 없으면 연결 정리
  socket.setTimeout(HEARTBEAT_TIMEOUT_MS);
  socket.on('timeout', () => {
    console.log(`[TCP] 하트비트 타임아웃: ${socketDeviceMap.get(socket) || socket.remoteAddress}`);
    socket.destroy(); // → 'close' 이벤트 → offline 처리
  });

  socket.on('data', (data) => {
    buffer += data.toString('utf-8');
    let idx;
    // \n 단위로 메시지 분리 (TCP 스트림 프레이밍)
    while ((idx = buffer.indexOf('\n')) !== -1) {
      const msg = buffer.slice(0, idx).trim();
      buffer = buffer.slice(idx + 1);
      if (msg) handleTcpMessage(socket, msg);
    }
  });

  socket.on('close', () => {
    const deviceId = socketDeviceMap.get(socket);
    socketDeviceMap.delete(socket);
    if (!deviceId) return;

    // 기존 대기 타이머가 있으면 취소 (동일 기기 소켓 중복 종료 시)
    if (offlineTimers.has(deviceId)) {
      clearTimeout(offlineTimers.get(deviceId));
    }

    // 3초 debounce — Android 재연결 backoff(1s) 흡수하여 순간 끊김 시 UI 깜빡임 방지
    // 진짜 오프라인(전원 차단 등)이면 3초 후 정상 offline 처리됨
    const timer = setTimeout(async () => {
      offlineTimers.delete(deviceId);

      // 같은 기기가 이미 새 소켓으로 재연결했으면 offline 처리 건너뜀
      const alreadyReconnected = [...socketDeviceMap.values()].includes(deviceId);
      if (alreadyReconnected) {
        console.log(`[TCP] 재연결 감지 — offline 처리 건너뜀: ${deviceId}`);
        return;
      }
      console.log(`[TCP] 보드 오프라인: ${deviceId}`);
      try {
        await prisma.device.update({
          where: { id: deviceId },
          data: { status: 'offline' }
        });
        io.emit('device_status_update', { deviceId, status: 'offline' });
      } catch (err) {
        console.error('[TCP] 오프라인 처리 에러:', err);
      }
    }, 3000);

    offlineTimers.set(deviceId, timer);
  });

  socket.on('error', (err) => {
    console.error(`[TCP] 에러:`, err.message);
  });
});

// ── 화면 스케줄 API ────────────────────────────────────────────────────────

app.get('/api/schedules', async (req, res) => {
  const schedules = await prisma.screenSchedule.findMany({ orderBy: { createdAt: 'asc' } });
  // SQLite는 boolean을 0/1로 저장하므로 명시적으로 변환
  res.json(schedules.map(s => ({ ...s, enabled: !!s.enabled })));
});

app.post('/api/schedules', async (req, res) => {
  const { id, storeId, deviceId, onTime, offTime, days, enabled } = req.body;
  try {
    const data = {
      storeId:  storeId  || null,
      deviceId: deviceId || null,
      onTime:   onTime   || null,
      offTime:  offTime  || null,
      days:     days     || '1,2,3,4,5,6,0',
      enabled:  enabled  !== false,
    };
    if (!id) {
      const dup = await prisma.screenSchedule.findFirst({
        where: {
          storeId:  data.storeId,
          deviceId: data.deviceId,
          onTime:   data.onTime,
          offTime:  data.offTime,
          days:     data.days,
        },
      });
      if (dup) return res.status(409).json({ error: '동일한 스케줄이 이미 존재합니다.' });
    }
    const schedule = id
      ? await prisma.screenSchedule.update({ where: { id }, data })
      : await prisma.screenSchedule.create({ data });
    reloadCrons();
    io.emit('screen_schedule');
    res.json(schedule);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.delete('/api/schedules/:id', async (req, res) => {
  await prisma.screenSchedule.delete({ where: { id: req.params.id } });
  reloadCrons();
  io.emit('screen_schedule');
  res.json({ ok: true });
});

// 스케줄 수동 전송 — 현재 연결된 모든 기기에 재조회 신호 발송
app.post('/api/schedules/push', requireAuth, async (req, res) => {
  const onlineCount = await prisma.device.count({ where: { status: 'online' } });
  io.emit('screen_schedule');
  console.log(`[SCHED] 수동 전송 → 온라인 기기 ${onlineCount}대`);
  res.json({ ok: true, devices: onlineCount });
});

// ── 화면 스케줄 cron 실행 ──────────────────────────────────────────────────

const activeCrons = [];

async function runScreenCommand(deviceId, storeId, on) {
  const adbPath = process.env.ADB_PATH || 'adb';
  const keycode = on ? '224' : '223';
  const where = deviceId ? { id: deviceId }
              : storeId  ? { storeId }
              : {};
  const devices = await prisma.device.findMany({ where: { ...where, ip: { not: null } } });

  for (const device of devices) {
    const room = `device:${device.id}`;
    const socketsInRoom = await io.in(room).allSockets();
    if (socketsInRoom.size > 0) {
      io.to(room).emit('screen_control', { deviceId: device.id, on });
      console.log(`[SCHED] 화면 ${on ? 'ON' : 'OFF'} → ${device.id} (Socket.io)`);
      continue;
    }
    // Socket.io 룸 없을 때 ADB 폴백
    const target = `${device.ip}:5555`;
    try {
      await adbExec(adbPath, ['connect', target], { timeout: 8000 });
      await adbExec(adbPath, ['-s', target, 'shell', 'input', 'keyevent', keycode]);
      console.log(`[SCHED] 화면 ${on ? 'ON' : 'OFF'} → ${device.id} (ADB ${target})`);
    } catch (e) {
      console.warn(`[SCHED] ${device.id} 명령 실패: ${e.message}`);
    }
  }
}

async function reloadCrons() {
  activeCrons.forEach(c => c.stop());
  activeCrons.length = 0;

  const schedules = await prisma.screenSchedule.findMany({ where: { enabled: true } });
  for (const s of schedules) {
    const dayList = s.days.split(',').map(Number);
    const cronDays = dayList.join(',');

    if (s.onTime) {
      const [h, m] = s.onTime.split(':');
      const job = cron.schedule(`${m} ${h} * * ${cronDays}`, () => runScreenCommand(s.deviceId, s.storeId, true), { timezone: 'Asia/Seoul' });
      activeCrons.push(job);
    }
    if (s.offTime) {
      const [h, m] = s.offTime.split(':');
      const job = cron.schedule(`${m} ${h} * * ${cronDays}`, () => runScreenCommand(s.deviceId, s.storeId, false), { timezone: 'Asia/Seoul' });
      activeCrons.push(job);
    }
  }
  console.log(`[SCHED] ${activeCrons.length}개 cron 등록됨`);
}

// 서버 시작 시 스케줄 로드
reloadCrons();

// 모바일 대시보드 정적 파일 서빙 (vite build 결과물)
const mobileDist = path.join(__dirname, '../mobile/dist');
if (fs.existsSync(mobileDist)) {
  app.use('/mobile', (req, res, next) => {
    const pathOnly = req.originalUrl.split('?')[0];
    if (req.path === '/' && !pathOnly.endsWith('/')) {
      const query = req.originalUrl.substring(pathOnly.length);
      return res.redirect(301, pathOnly + '/' + query);
    }
    next();
  });
  app.use('/mobile', express.static(mobileDist));
  app.get('/mobile/{*splat}', (req, res) => res.sendFile(path.join(mobileDist, 'index.html')));
  console.log('[Express] 모바일 대시보드 정적 파일 서빙 활성화');
}

// 대시보드 정적 파일 서빙 (vite build 결과물)
const dashboardDist = path.join(__dirname, '../dashboard/dist');
if (fs.existsSync(dashboardDist)) {
  app.use(express.static(dashboardDist));
  app.get('/{*splat}', (req, res) => res.sendFile(path.join(dashboardDist, 'index.html')));
  console.log('[Express] 대시보드 정적 파일 서빙 활성화');
}


// 서버 실행
const HTTP_PORT = process.env.PORT || 3300;
const TCP_PORT = process.env.TCP_PORT || 10080;

httpServer.listen(HTTP_PORT, () => {
  console.log(`[Express] 대시보드 API 서버가 포트 ${HTTP_PORT}에서 실행 중입니다.`);
});

tcpServer.listen(TCP_PORT, () => {
  console.log(`[TCP] 사이니지 보드용 소켓 서버가 포트 ${TCP_PORT}에서 대기 중입니다.`);
});

// 기기 ID 및 이름 정리 — "Device-" 접두어 완전 제거 마이그레이션 (1회성)
(async () => {
  try {
    const devices = await prisma.device.findMany({
      where: {
        OR: [
          { id: { startsWith: 'Device-' } },
          { name: { startsWith: 'Device-' } }
        ]
      }
    });
    for (const d of devices) {
      const oldId = d.id;
      const newId = d.id.replace(/^Device-/, '');
      const newName = d.name.replace(/^Device-/, '');
      console.log(`[Migrate] 기기 ID/이름 정리 시작: ID="${oldId}" Name="${d.name}" -> ID="${newId}" Name="${newName}"`);
      if (oldId !== newId) {
        await prisma.$transaction(async (tx) => {
          await tx.$executeRawUnsafe(`UPDATE "Device" SET "id" = ? , "name" = ? WHERE "id" = ?`, newId, newName, oldId);
          await tx.$executeRawUnsafe(`UPDATE "ScreenSchedule" SET "deviceId" = ? WHERE "deviceId" = ?`, newId, oldId);
          await tx.$executeRawUnsafe(`UPDATE "PlaylistMedia" SET "targetDeviceId" = ? WHERE "targetDeviceId" = ?`, newId, oldId);
        });
      } else {
        await prisma.device.update({
          where: { id: d.id },
          data: { name: newName }
        });
      }
      console.log(`[Migrate] 기기 ID/이름 정리 완료: "${newId}"`);
    }
  } catch (e) {
    console.error('[Migrate] 기기 ID/이름 정리 오류:', e.message);
  }
})();

// 하트비트 lastSeen 주기 스윕 — 소켓 close를 못 받는 경우(전원 차단 등)와
// 서버 재시작 후 잔존 online 상태까지 DB 기준으로 정리.
setInterval(async () => {
  const threshold = new Date(Date.now() - HEARTBEAT_TIMEOUT_MS);
  try {
    const stale = await prisma.device.findMany({
      where: { status: 'online', OR: [{ lastSeen: { lt: threshold } }, { lastSeen: null }] }
    });
    for (const d of stale) {
      await prisma.device.update({ where: { id: d.id }, data: { status: 'offline' } });
      io.emit('device_status_update', { deviceId: d.id, status: 'offline' });
      console.log(`[Sweep] 오프라인 처리: ${d.id} (lastSeen=${d.lastSeen})`);
    }
  } catch (err) {
    console.error('[Sweep] 에러:', err);
  }
}, 10000);

// OTA Watchdog — 5분마다 온라인 기기 중 구버전 기기에 update_apk 재발송
setInterval(async () => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) return;
  const meta = loadDeployMeta();
  const targetVersion = meta.apkVersion;
  if (!targetVersion) return;

  try {
    const threshold = new Date(Date.now() - HEARTBEAT_TIMEOUT_MS);
    const onlineDevices = await prisma.device.findMany({
      where: { lastSeen: { gte: threshold } }
    });
    let outdatedCount = 0;
    for (const dev of onlineDevices) {
      const raw = dev.appVersion || '';
      const currentVer = raw.includes(' (') ? raw.slice(0, raw.indexOf(' (')) : raw;
      if (!currentVer || currentVer === targetVersion) continue;

      const room = `device:${dev.id}`;
      const socketsInRoom = await io.in(room).allSockets();
      if (socketsInRoom.size > 0) {
        io.to(room).emit('update_apk', { url: '/update/apk', deviceId: dev.id });
        console.log(`[OTA-Watchdog] ${dev.id}: 구버전 (${currentVer}) → v${targetVersion} update_apk 재발송 (소켓 ${socketsInRoom.size}개 연결)`);
        outdatedCount++;
      }
    }
    if (outdatedCount > 0) {
      console.log(`[OTA-Watchdog] 총 ${outdatedCount}대 구버전 기기에 OTA 재발송 완료`);
    }
  } catch (e) {
    console.warn('[OTA-Watchdog] 에러:', e.message);
  }
}, 5 * 60 * 1000);
