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

const prisma = new PrismaClient();
const app = express();

const DEVICE_SECRET = process.env.DEVICE_SECRET || 'changeme';
if (DEVICE_SECRET === 'changeme') {
  console.warn('[WARN] DEVICE_SECRET이 기본값입니다. .env에서 변경하세요.');
}
app.use(cors());
app.use(express.json());
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
          transitionTime: item.transitionTime || 1000
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
    parts.forEach(p => {
      if (p.startsWith('cpu:')) cpu = parseFloat(p.substring(4));
      if (p.startsWith('mem:')) mem = parseFloat(p.substring(4));
    });
    // NaN 방어
    if (!Number.isFinite(cpu)) cpu = null;
    if (!Number.isFinite(mem)) mem = null;

    try {
      await prisma.device.upsert({
        where: { id: deviceId },
        update: { status: 'online', lastSeen: new Date(), ip: socket.remoteAddress, cpuUsage: cpu, memUsage: mem },
        create: { id: deviceId, name: `Device-${deviceId}`, status: 'online', lastSeen: new Date(), ip: socket.remoteAddress, cpuUsage: cpu, memUsage: mem }
      });
      io.emit('device_status_update', { deviceId, status: 'online', cpu, mem });
      socket.write('ok:\n');
    } catch (err) {
      console.error('[TCP] DB 에러:', err);
    }
  }
}

const tcpServer = net.createServer((socket) => {
  console.log(`[TCP] 보드 접속됨: ${socket.remoteAddress}:${socket.remotePort}`);
  let buffer = '';

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
