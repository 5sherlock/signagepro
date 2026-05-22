require('dotenv').config();
const express = require('express');
const cors = require('cors');
const net = require('net');
const { PrismaClient } = require('@prisma/client');
const http = require('http');
const { Server } = require('socket.io');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const { execFile } = require('child_process');
const os = require('os');

const prisma = new PrismaClient();
const app = express();

const DEVICE_SECRET = process.env.DEVICE_SECRET || 'changeme';
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

app.use(cors());
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
const ALLOWED_MIME = ['video/mp4', 'video/webm', 'image/jpeg', 'image/png', 'image/gif', 'image/webp'];
const upload = multer({
  storage,
  limits: { fileSize: 200 * 1024 * 1024 }, // 200MB
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
    console.log(`[API] 기기 목록 조회 요청됨. 현재 기기 수: ${devices.length}대`);
    res.json(devices);
  } catch (err) {
    console.error('[API] 기기 목록 조회 에러:', err);
    res.status(500).json({ error: '기기 조회 실패' });
  }
});

// 특정 기기 조회 (player 부팅 시 자신의 groupId 확인용)
app.get('/api/devices/:id', async (req, res) => {
  try {
    const device = await prisma.device.findUnique({
      where: { id: req.params.id },
      include: { group: true, store: true }
    });
    if (!device) return res.status(404).json({ error: '기기를 찾을 수 없습니다.' });
    res.json(device);
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
app.post('/api/media', upload.single('file'), async (req, res) => {
  if (!req.file) return res.status(400).json({ error: '파일이 없습니다.' });
  const { storeId } = req.body;

  const type = req.file.mimetype.startsWith('video/') ? 'video' : 'image';

  // SHA-256 해시 계산 (player 캐시 검증용)
  const hash = await new Promise((resolve, reject) => {
    const h = crypto.createHash('sha256');
    fs.createReadStream(req.file.path)
      .on('data', chunk => h.update(chunk))
      .on('end', () => resolve(h.digest('hex')))
      .on('error', reject);
  });

  try {
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
    res.status(500).json({ error: '미디어 저장 실패' });
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

// APK 파일 서빙 (server/update/app.apk 를 이 경로에 놓으면 됨)
app.get('/update/apk', (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) {
    return res.status(404).json({ error: 'APK 없음. server/update/app.apk 를 배치하세요.' });
  }
  res.download(apkPath, 'signagepro-player.apk');
});

// APK 배포 상태 확인
// APK 업로드 (대시보드에서 직접 업로드)
app.post('/api/update/apk', uploadApk.single('apk'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: '파일이 없습니다.' });
  const stat = fs.statSync(req.file.path);
  console.log(`[OTA] APK 업로드 완료: ${(stat.size / 1024 / 1024).toFixed(1)} MB`);
  res.json({ ok: true, size: stat.size, updatedAt: stat.mtime });
});

app.get('/api/update/status', (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) {
    return res.json({ available: false });
  }
  const stat = fs.statSync(apkPath);
  res.json({ available: true, size: stat.size, updatedAt: stat.mtime });
});

// APK 삭제 (배포 취소)
app.delete('/api/update/apk', (req, res) => {
  const apkPath = path.join(updateDir, 'app.apk');
  if (!fs.existsSync(apkPath)) return res.status(404).json({ error: 'APK 없음' });
  try {
    fs.unlinkSync(apkPath);
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

  const { deviceId } = req.body;
  const adbPath = process.env.ADB_PATH || 'adb';

  // deviceId 지정 시 해당 기기만, 없으면 DB에서 ip 있는 모든 기기
  let devices = [];
  try {
    if (deviceId) {
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

  const trackExec = (cmd, cmdArgs, opts = {}) => new Promise((resolve) => {
    const proc = execFile(cmd, cmdArgs, { windowsHide: true, ...opts }, (err, stdout, stderr) => {
      activeAdbProcs.delete(proc);
      resolve({ err, stdout, stderr });
    });
    activeAdbProcs.add(proc);
  });

  for (const device of devices) {
    if (adbCancelled) {
      results.push({ deviceId: device.id, ip: device.ip, success: false, output: '취소됨' });
      continue;
    }
    const target = `${device.ip}:5555`;
    try {
      // 1. adb connect
      const { stdout: cs } = await trackExec(adbPath, ['connect', target], { timeout: 8000 });
      console.log(`[ADB] connect ${target}: ${cs?.trim()}`);

      if (adbCancelled) {
        results.push({ deviceId: device.id, ip: device.ip, success: false, output: '취소됨' });
        continue;
      }

      // 2. adb install -r
      const { err, stdout, stderr } = await trackExec(
        adbPath, ['-s', target, 'install', '-r', apkPath], { timeout: 60000 }
      );
      const out = ((stdout || '') + (stderr || '')).trim();
      console.log(`[ADB] install ${target}: ${out}`);
      const installOk = !err && /success/i.test(out);

      // 3. 설치 성공 시 전체화면 안내 팝업 억제 + 앱 자동 실행
      if (installOk && !adbCancelled) {
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
      results.push({ deviceId: device.id, ip: device.ip, success: installOk, output: out });
    } catch (e) {
      results.push({ deviceId: device.id, ip: device.ip, success: false, output: e.message });
    }
  }

  res.json({ results });
});

// ADB 설치 취소
let adbCancelled = false;
const activeAdbProcs = new Set();

app.post('/api/update/adb-cancel', (req, res) => {
  adbCancelled = true;
  activeAdbProcs.forEach(p => { try { p.kill(); } catch (_) {} });
  activeAdbProcs.clear();
  console.log('[ADB] 설치 취소 요청');
  res.json({ ok: true });
});

// 기기 원격 재부팅 (ADB TCP 필요)
app.post('/api/devices/:id/reboot', async (req, res) => {
  const device = await prisma.device.findUnique({ where: { id: req.params.id } });
  if (!device || !device.ip) return res.status(404).json({ error: '기기 또는 IP 정보 없음' });
  const target = `${device.ip}:5555`;
  const adbPath = process.env.ADB_PATH || 'adb';
  try {
    await adbExec(adbPath, ['connect', target], { timeout: 8000 });
    execFile(adbPath, ['-s', target, 'shell', 'reboot'], { timeout: 10000, windowsHide: true }, () => {}); // 연결 끊김이 정상
    console.log(`[ADB] 재부팅 명령 전송: ${device.id} (${target})`);
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

// 볼륨 조절 (0-15, STREAM_MUSIC)
app.post('/api/devices/:id/volume', async (req, res) => {
  const adbPath = process.env.ADB_PATH || 'adb';
  try {
    const { target } = await getDeviceTarget(req.params.id);
    const level = Math.min(15, Math.max(0, parseInt(req.body.level) || 0));
    await adbExec(adbPath, ['-s', target, 'shell', 'media', 'volume', '--stream', '3', '--set', String(level)]);
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
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

async function handleTcpMessage(socket, msg) {
  // 인증: auth:<deviceId>:<secret>
  if (msg.startsWith('auth:')) {
    const [, deviceId, secret] = msg.split(':');
    if (secret !== DEVICE_SECRET) {
      socket.write('err:unauthorized\n');
      socket.destroy();
      return;
    }
    socketDeviceMap.set(socket, deviceId);
    socket.write('auth:ok\n');
    console.log(`[TCP] 인증 성공: ${deviceId}`);
    return;
  }

  // 하트비트: status:<deviceId>/cpu:<x>/mem:<x>
  if (msg.startsWith('status:')) {
    const deviceId = socketDeviceMap.get(socket);
    if (!deviceId) {
      socket.write('err:not_authenticated\n');
      return;
    }

    const parts = msg.substring(7).split('/');
    let cpu = null;
    let mem = null;
    let ver = null;
    parts.forEach(p => {
      if (p.startsWith('cpu:')) cpu = parseFloat(p.substring(4));
      if (p.startsWith('mem:')) mem = parseFloat(p.substring(4));
      if (p.startsWith('ver:')) ver = p.substring(4).trim() || null;
    });
    // NaN 방어
    if (!Number.isFinite(cpu)) cpu = null;
    if (!Number.isFinite(mem)) mem = null;

    try {
      await prisma.device.upsert({
        where: { id: deviceId },
        update: { status: 'online', lastSeen: new Date(), ip: normalizeIp(socket.remoteAddress), cpuUsage: cpu, memUsage: mem, ...(ver && { appVersion: ver }) },
        create: { id: deviceId, name: `Device-${deviceId}`, status: 'online', lastSeen: new Date(), ip: normalizeIp(socket.remoteAddress), cpuUsage: cpu, memUsage: mem, appVersion: ver }
      });
      io.emit('device_status_update', { deviceId, status: 'online', cpu, mem, ip: normalizeIp(socket.remoteAddress), appVersion: ver });
      socket.write('ok:\n');
    } catch (err) {
      console.error('[TCP] DB 에러:', err);
    }
  }
}

const HEARTBEAT_TIMEOUT_MS = 35000; // 하트비트 10초 간격 × 3 + 여유

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

  socket.on('close', async () => {
    const deviceId = socketDeviceMap.get(socket);
    socketDeviceMap.delete(socket);
    if (deviceId) {
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
    }
  });

  socket.on('error', (err) => {
    console.error(`[TCP] 에러:`, err.message);
  });
});

// 서버 실행
const HTTP_PORT = 3000;
const TCP_PORT = 10080;

httpServer.listen(HTTP_PORT, () => {
  console.log(`[Express] 대시보드 API 서버가 포트 ${HTTP_PORT}에서 실행 중입니다.`);
});

tcpServer.listen(TCP_PORT, () => {
  console.log(`[TCP] 사이니지 보드용 소켓 서버가 포트 ${TCP_PORT}에서 대기 중입니다.`);
});

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
