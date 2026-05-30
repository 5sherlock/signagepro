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

const prisma = new PrismaClient();
const app = express();

// 메모리에 보관할 기기별 실시간 상태 캐시 (deviceTime, slide, dl, vol, vu)
const deviceLiveStateCache = new Map();

const DEVICE_SECRET = process.env.DEVICE_SECRET || 'changeme';
const HEARTBEAT_TIMEOUT_MS = 35000; // 하트비트 10초 간격 × 3 + 여유
if (DEVICE_SECRET === 'changeme') {
  console.warn('[WARN] DEVICE_SECRET이 기본값입니다. .env에서 변경하세요.');
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

// 세션 토큰 저장소 (메모리, 24시간 유효)
const sessions = new Map();
setInterval(() => {
  const now = Date.now();
  for (const [token, expiry] of sessions) if (expiry < now) sessions.delete(token);
}, 60 * 60 * 1000);

app.use(cors({
  origin: '*',
  methods: ['GET', 'HEAD', 'PUT', 'PATCH', 'POST', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
}));
app.options('/{*path}', cors()); // FormData + Authorization 등 복잡한 요청의 프리플라이트 OPTIONS 명시 처리
app.use((req, res, next) => {
  console.log(`[REQ] ${new Date().toISOString()} ${req.ip} ${req.method} ${req.url}`);
  next();
});
app.use(express.json());

// ── 인증 ─────────────────────────────────────────────────────────────────────

app.post('/api/auth/login', (req, res) => {
  if (!adminPassword) return res.json({ token: 'dev-mode' }); // 개발 모드
  if (req.body.password !== adminPassword)
    return res.status(401).json({ error: '비밀번호가 틀렸습니다.' });
  const token = crypto.randomBytes(32).toString('hex');
  sessions.set(token, Date.now() + 24 * 60 * 60 * 1000);
  res.json({ token });
});

app.post('/api/auth/change-password', (req, res) => {
  const { current, newPassword } = req.body;
  if (adminPassword && current !== adminPassword)
    return res.status(401).json({ error: '현재 비밀번호가 틀렸습니다.' });
  if (!newPassword || newPassword.length < 4)
    return res.status(400).json({ error: '새 비밀번호는 4자 이상이어야 합니다.' });
  saveAdminPassword(newPassword);
  sessions.clear(); // 기존 세션 모두 무효화
  res.json({ ok: true });
});

app.post('/api/auth/logout', (req, res) => {
  sessions.delete(req.headers.authorization?.slice(7));
  res.json({ ok: true });
});

const requireAuth = (req, res, next) => {
  if (!adminPassword) return next(); // adminPassword 미설정 시 개방
  const token = req.headers.authorization?.slice(7);
  if (token === 'dev-mode') return next();
  const expiry = sessions.get(token);
  if (!expiry || expiry < Date.now()) {
    sessions.delete(token);
    return res.status(401).json({ error: '인증이 필요합니다.' });
  }
  next();
};

// 기기 전용 GET API는 인증 제외 (Android 앱이 직접 호출)
const DEVICE_OPEN = [
  '/api/time',
  /^\/api\/devices\/[^/]+$/,
  /^\/api\/groups\/[^/]+\/playlist$/,
];
app.use('/api', (req, res, next) => {
  if (req.path.startsWith('/auth/')) return next();
  const open = DEVICE_OPEN.some(p =>
    typeof p === 'string' ? req.path === p : p.test(req.path)
  );
  if (open && req.method === 'GET') return next();
  requireAuth(req, res, next);
});
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
const io = new Server(httpServer, { cors: { origin: '*' } });

// --- Socket.io 웹 플레이어 연결 처리 ---
io.on('connection', (socket) => {
  // Android ControlChannel이 연결 직후 자신의 deviceId를 등록
  socket.on('register_device', (data) => {
    const deviceId = typeof data === 'object' ? data?.deviceId : String(data);
    if (!deviceId) return;
    socket.deviceId = deviceId;
    socket.join(`device:${deviceId}`);
    console.log(`[Socket.io] 기기 등록: ${deviceId}`);
  });

  socket.on('web_player_heartbeat', async (data) => {
    const deviceId = typeof data === 'string' ? data : (data?.deviceId || '');
    const name = typeof data === 'object' ? data?.name : null;
    if (!deviceId) return;
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
    const devices = await prisma.device.findMany({ include: { group: true, store: true } });
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
        screenOff: status === 'online' ? (cached.screenOff ?? false) : false
      };
    });
    console.log(`[API] 기기 목록 조회 요청됨. 현재 기기 수: ${devices.length}대`);
    res.json(result);
  } catch (err) {
    console.error('[API] 기기 목록 조회 에러:', err);
    res.status(500).json({ error: '기기 조회 실패' });
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
    // 이 기기에 적용되는 스케줄 (전체 대상 null + 기기 전용)
    const schedules = await prisma.screenSchedule.findMany({
      where: { enabled: true, OR: [{ deviceId: null }, { deviceId: req.params.id }] }
    });
    res.json({ ...device, schedules });
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
  const { storeId } = req.body;

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

    const media = await prisma.media.create({
      data: {
        filename: req.file.originalname,
        path: `/uploads/${req.file.filename}`,
        type,
        size: req.file.size,
        hash,
        storeId: storeId || null
      }
    });
    res.json(media);
  } catch (err) {
    // 실패 시 업로드된 파일 정리
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
  console.log(`[API] 미디어 전체 삭제 요청: storeId=${storeId}`);
  
  try {
    const medias = await prisma.media.findMany({ where });
    
    // 1. PlaylistMedia에서 참조 삭제 (Foreign Key 제약 조건 해결)
    const mediaIds = medias.map(m => m.id);
    await prisma.playlistMedia.deleteMany({
      where: { mediaId: { in: mediaIds } }
    });
    
    // 2. 파일시스템에서 삭제
    for (const media of medias) {
      // media.path가 '/uploads/...' 형태이므로 앞에 '.'을 붙이거나 path.join을 정확히 해야 함
      const filePath = path.join(__dirname, media.path);
      console.log(`[API] 파일 삭제 시도: ${filePath}`);
      if (fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
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
    
    // 3. 파일시스템에서 삭제
    const filePath = path.join(__dirname, media.path);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
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
  const playlist = await prisma.playlist.findFirst({
    where: { groupId },
    include: {
      medias: {
        include: { media: true },
        orderBy: { order: 'asc' }
      }
    }
  });
  res.json(playlist || { medias: [] });
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
          transition: item.transition || 'fade',
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

// --- OTA 업데이트 ---

const updateDir = path.join(__dirname, 'update');
if (!fs.existsSync(updateDir)) fs.mkdirSync(updateDir, { recursive: true });

// 배포 메타 (마지막 배포 시각) — 서버 재시작 후에도 유지
const deployMetaPath = path.join(updateDir, 'deploy-meta.json');
function loadDeployMeta() {
  try {
    if (fs.existsSync(deployMetaPath)) {
      const data = JSON.parse(fs.readFileSync(deployMetaPath, 'utf8'));
      return data.lastDeployedAt ? new Date(data.lastDeployedAt) : null;
    }
  } catch (e) { console.warn('[OTA] deploy-meta.json 읽기 실패:', e.message); }
  return null;
}
function saveDeployMeta(date) {
  try {
    fs.writeFileSync(deployMetaPath, JSON.stringify({ lastDeployedAt: date?.toISOString() ?? null }), 'utf8');
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

  bb.on('file', (fieldname, file, info) => {
    const { filename = '', mimeType = '' } = info;
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
        if (!res.headersSent) res.json({ ok: true, size: stat.size, updatedAt: stat.mtime });
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

let lastDeployedAt = loadDeployMeta(); // 마지막 배포(OTA 푸시 or ADB 설치) 완료 시각 — 재시작 후에도 유지

app.get('/api/update/status', (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) {
    return res.json({ available: false });
  }
  const stat = fs.statSync(apkPath);
  res.json({ available: true, size: stat.size, updatedAt: stat.mtime, lastDeployedAt });
});

// APK 삭제 (배포 취소)
app.delete('/api/update/apk', (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) return res.status(404).json({ error: 'APK 없음' });
  try {
    fs.unlinkSync(apkPath);
    lastDeployedAt = null;
    saveDeployMeta(null);
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
  saveDeployMeta(lastDeployedAt);
  console.log(`[OTA] 업데이트 푸시: ${deviceId ? deviceId : '전체 단말'}`);
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
  const adbPath = process.env.ADB_PATH || 'adb';

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

// 기기 원격 재부팅 (ADB TCP 필요)
app.post('/api/devices/:id/reboot', async (req, res) => {
  const device = await prisma.device.findUnique({ where: { id: req.params.id } });
  if (!device || !device.ip) return res.status(404).json({ error: '기기 또는 IP 정보 없음' });

  // 오프라인 기기는 ADB 연결 불가 — 미리 차단
  if (device.status !== 'online') {
    return res.status(400).json({ error: '기기가 오프라인입니다.\n전원과 네트워크 연결을 확인해주세요.' });
  }

  const target = `${device.ip}:5555`;
  const adbPath = process.env.ADB_PATH || 'adb';
  try {
    // 1. 앱에 검은 화면 준비 명령 → 렌더링 루프 중단
    io.to(`device:${device.id}`).emit('prepare_reboot', { deviceId: device.id });
    await new Promise(r => setTimeout(r, 600));

    await adbExec(adbPath, ['connect', target], { timeout: 8000 });

    // 2. 기기 쉘에서 한 번에: HDMI 끄기 → 2초 대기 → reboot
    //    네트워크 왕복 없이 기기 내에서 타이밍 보장
    //    (RK3229 U4X+CM: /sys/class/display/HDMI/enable = -rw-rw-rw-, su 불필요)
    execFile(adbPath, ['-s', target, 'shell',
      'echo 0 > /sys/class/display/HDMI/enable; sleep 3; reboot'
    ], { timeout: 15000, windowsHide: true }, () => {});
    console.log(`[ADB] 재부팅 명령 전송: ${device.id} (${target})`);
    res.json({ ok: true });
  } catch (e) {
    const friendly = e.message?.includes('connect')
      ? `ADB 연결 실패 (${device.ip}:5555)\nWi-Fi가 연결되어 있는지 확인해주세요.`
      : e.message;
    res.status(500).json({ error: friendly });
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
  const adbPath = process.env.ADB_PATH || 'adb';
  try {
    const { target } = await getDeviceTarget(req.params.id);
    await adbExec(adbPath, ['-s', target, 'shell', 'am', 'force-stop', 'com.signagepro.player']);
    await new Promise(r => setTimeout(r, 1500));
    await adbExec(adbPath, ['-s', target, 'shell', 'am', 'start', '-n', 'com.signagepro.player/.MainActivity']);
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// 화면 켜기/끄기 (KEYCODE_WAKEUP=224 / KEYCODE_SLEEP=223)
app.post('/api/devices/:id/screen', async (req, res) => {
  const adbPath = process.env.ADB_PATH || 'adb';
  try {
    const { target } = await getDeviceTarget(req.params.id);
    const keycode = req.body.on ? '224' : '223';
    await adbExec(adbPath, ['-s', target, 'shell', 'input', 'keyevent', keycode]);
    res.json({ ok: true });
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
    let cpu = null, mem = null, ver = null, dl = null, vol = null, deviceTime = null, slide = null, screen = null;
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
      dl: dl !== null ? dl : cached.dl,
      vol: vol !== null ? vol : cached.vol,
      cpu: cpu ?? cached.cpu,
      mem: mem ?? cached.mem,
      ver: ver ?? cached.ver,
      // screen: "on"/"off" — null이면 이전 값 유지 (구버전 앱 호환)
      screenOff: screen !== null ? (screen === 'off') : (cached.screenOff ?? false)
    });

    try {
      await prisma.device.upsert({
        where: { id: deviceId },
        update: { status: 'online', lastSeen: new Date(), ip: normalizeIp(socket.remoteAddress), cpuUsage: cpu, memUsage: mem, ...(ver && { appVersion: ver }) },
        create: { id: deviceId, name: deviceId, status: 'online', lastSeen: new Date(), ip: normalizeIp(socket.remoteAddress), cpuUsage: cpu, memUsage: mem, appVersion: ver }
      });
      const screenOff = deviceLiveStateCache.get(deviceId)?.screenOff ?? false;
      io.emit('device_status_update', { deviceId, status: 'online', cpu, mem, ip: normalizeIp(socket.remoteAddress), appVersion: ver, dl, vol, deviceTime, slide, screenOff });
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
  const { id, deviceId, onTime, offTime, days, enabled } = req.body;
  try {
    const data = {
      deviceId: deviceId || null,
      onTime: onTime || null,
      offTime: offTime || null,
      days: days || '1,2,3,4,5,6,0',
      enabled: enabled !== false,
    };
    // 중복 체크 — 같은 기기·켜는시간·끄는시간·요일 조합이 이미 존재하면 거부
    if (!id) {
      const dup = await prisma.screenSchedule.findFirst({
        where: {
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
    io.emit('screen_schedule'); // 앱이 기기 정보 재조회하여 스케줄 갱신
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

async function runScreenCommand(deviceId, on) {
  const adbPath = process.env.ADB_PATH || 'adb';
  const keycode = on ? '224' : '223';
  const devices = deviceId
    ? await prisma.device.findMany({ where: { id: deviceId, ip: { not: null } } })
    : await prisma.device.findMany({ where: { ip: { not: null } } });

  for (const device of devices) {
    const target = `${device.ip}:5555`;
    try {
      await adbExec(adbPath, ['connect', target], { timeout: 8000 });
      await adbExec(adbPath, ['-s', target, 'shell', 'input', 'keyevent', keycode]);
      console.log(`[SCHED] 화면 ${on ? 'ON' : 'OFF'} → ${device.id} (${target})`);
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
      const job = cron.schedule(`${m} ${h} * * ${cronDays}`, () => runScreenCommand(s.deviceId, true), { timezone: 'Asia/Seoul' });
      activeCrons.push(job);
    }
    if (s.offTime) {
      const [h, m] = s.offTime.split(':');
      const job = cron.schedule(`${m} ${h} * * ${cronDays}`, () => runScreenCommand(s.deviceId, false), { timezone: 'Asia/Seoul' });
      activeCrons.push(job);
    }
  }
  console.log(`[SCHED] ${activeCrons.length}개 cron 등록됨`);
}

// 서버 시작 시 스케줄 로드
reloadCrons();

// 대시보드 정적 파일 서빙 (vite build 결과물)
const dashboardDist = path.join(__dirname, '../dashboard/dist');
if (fs.existsSync(dashboardDist)) {
  app.use(express.static(dashboardDist));
  app.get('/{*splat}', (req, res) => res.sendFile(path.join(dashboardDist, 'index.html')));
  console.log('[Express] 대시보드 정적 파일 서빙 활성화');
}

// 서버 실행
const HTTP_PORT = process.env.PORT || 3300;
const TCP_PORT = 10080;

httpServer.listen(HTTP_PORT, () => {
  console.log(`[Express] 대시보드 API 서버가 포트 ${HTTP_PORT}에서 실행 중입니다.`);
});

tcpServer.listen(TCP_PORT, () => {
  console.log(`[TCP] 사이니지 보드용 소켓 서버가 포트 ${TCP_PORT}에서 대기 중입니다.`);
});

// 기기 이름 정리 — "Device-" 접두어 제거 (1회성 마이그레이션)
(async () => {
  try {
    const devices = await prisma.device.findMany({ where: { name: { startsWith: 'Device-' } } });
    for (const d of devices) {
      const newName = d.name.replace(/^Device-/, '');
      await prisma.device.update({ where: { id: d.id }, data: { name: newName } });
      console.log(`[Migrate] 기기 이름 정리: "${d.name}" → "${newName}"`);
    }
  } catch (e) {
    console.error('[Migrate] 기기 이름 정리 오류:', e.message);
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
