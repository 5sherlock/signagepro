const express = require('express');
const cors = require('cors');
const net = require('net');
const { PrismaClient } = require('@prisma/client');
const http = require('http');
const { Server } = require('socket.io');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const prisma = new PrismaClient();
const app = express();
app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Ensure uploads folder exists
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir);
}

// Configure multer
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => cb(null, Date.now() + '-' + file.originalname)
});
const upload = multer({ storage });

const httpServer = http.createServer(app);
const io = new Server(httpServer, { cors: { origin: '*' } });

// --- REST API (대시보드 통신용) ---

// 모든 사업장 조회
app.get('/api/stores', async (req, res) => {
  const stores = await prisma.store.findMany();
  res.json(stores);
});

// 사업장 추가
app.post('/api/stores', async (req, res) => {
  const store = await prisma.store.create({ data: { name: req.body.name } });
  res.json(store);
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
  const devices = await prisma.device.findMany({ include: { group: true, store: true } });
  res.json(devices);
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

// --- REST API: 미디어 및 재생목록 ---

// 1. 미디어 업로드 및 생성
app.post('/api/media', upload.single('file'), async (req, res) => {
  if (!req.file) return res.status(400).json({ error: '파일이 없습니다.' });
  const { storeId } = req.body;
  
  const type = req.file.mimetype.startsWith('video/') ? 'video' : 'image';
  
  try {
    const media = await prisma.media.create({
      data: {
        filename: req.file.originalname,
        path: `/uploads/${req.file.filename}`,
        type,
        size: req.file.size,
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
  try {
    const medias = await prisma.media.findMany({ where });
    for (const media of medias) {
      const filePath = path.join(__dirname, media.path);
      if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
    }
    await prisma.media.deleteMany({ where });
    res.json({ success: true, count: medias.length });
  } catch (err) {
    res.status(500).json({ error: '전체 삭제 실패' });
  }
});

// 2.5 미디어 삭제
app.delete('/api/media/:id', async (req, res) => {
  try {
    const media = await prisma.media.findUnique({ where: { id: req.params.id } });
    if (!media) return res.status(404).json({ error: '미디어를 찾을 수 없습니다.' });

    // DB 삭제
    await prisma.media.delete({ where: { id: media.id } });
    
    // 파일시스템에서 삭제
    const filePath = path.join(__dirname, media.path);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: '미디어 삭제 실패' });
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
          transition: item.transition || 'fade'
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

// --- TCP 소켓 서버 (10080 포트, 레거시 및 하트비트 통신용) ---

const tcpServer = net.createServer((socket) => {
  console.log(`[TCP] 보드 접속됨: ${socket.remoteAddress}:${socket.remotePort}`);

  socket.on('data', async (data) => {
    const msg = data.toString('utf-8').trim();
    console.log(`[TCP RX] ${msg}`);
    
    // 임시 하트비트/상태 처리 로직
    // 예: "status:device123/cpu:12/mem:45"
    if (msg.startsWith('status:')) {
      const parts = msg.substring(7).split('/');
      const deviceId = parts[0];
      
      let cpu = null;
      let mem = null;
      
      parts.forEach(p => {
        if (p.startsWith('cpu:')) cpu = parseFloat(p.substring(4));
        if (p.startsWith('mem:')) mem = parseFloat(p.substring(4));
      });
      
      try {
        // DB에 기기가 없으면 생성, 있으면 업데이트
        await prisma.device.upsert({
          where: { id: deviceId },
          update: { 
            status: 'online', 
            lastSeen: new Date(),
            ip: socket.remoteAddress,
            cpuUsage: cpu,
            memUsage: mem
          },
          create: {
            id: deviceId,
            name: `Device-${deviceId}`,
            status: 'online',
            lastSeen: new Date(),
            ip: socket.remoteAddress,
            cpuUsage: cpu,
            memUsage: mem
          }
        });
        
        // 대시보드(웹)로 실시간 상태 브로드캐스트
        io.emit('device_status_update', { deviceId, status: 'online', cpu, mem });
        
        // 보드에 응답 (하트비트 ACK)
        socket.write('ok:\n');
      } catch (err) {
        console.error('[TCP] DB 에러:', err);
      }
    }
  });

  socket.on('close', () => {
    console.log(`[TCP] 보드 접속 해제: ${socket.remoteAddress}`);
    // 여기서 해당 소켓에 매핑된 기기의 status를 offline으로 변경하는 로직이 들어갈 수 있습니다.
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
