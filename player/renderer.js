const io = require('socket.io-client');
const axios = require('axios');
const { ipcRenderer } = require('electron');

let machineId = '';
let serverHost = '';
let API_URL = '';
let groupId = null;
let playlist = [];
let currentIndex = 0;
let playTimer = null;

let activeLayer = 1;

async function init() {
  machineId = await ipcRenderer.invoke('get-machine-id');
  serverHost = await ipcRenderer.invoke('get-server-host');
  API_URL = `http://${serverHost}:3000`; // HTTP 서버(API, Socket) 포트는 3000

  document.getElementById('device-id').innerText = machineId;
  document.getElementById('device-name-display').innerText = 'Device-' + machineId;

  // 소켓 통신 연결
  const socket = io(API_URL);
  
  socket.on('connect', () => {
    document.getElementById('status-text').innerText = '서버에 연결되었습니다. 동기화 중...';
    fetchDeviceConfig();
  });

  // 대시보드에서 그룹 할당 정보가 변경된 경우
  socket.on('group_assignment_changed', () => {
    fetchDeviceConfig();
  });

  // 해당 그룹의 재생목록이 업데이트된 경우
  socket.on('playlist_updated', (data) => {
    if (data.groupId === groupId) {
      fetchPlaylist();
    }
  });

  socket.on('disconnect', () => {
    showIdle('서버와의 연결이 끊어졌습니다. 재연결 대기 중...');
  });
}

// 1. 서버에 내 ID를 보내서 소속 그룹 확인
async function fetchDeviceConfig() {
  try {
    const res = await axios.get(`${API_URL}/api/devices`);
    const device = res.data.find(d => d.id === machineId);
    
    if (device) {
      if (device.name) {
        document.getElementById('device-name-display').innerText = device.name;
      }
      if (device.groupId) {
        groupId = device.groupId;
        fetchPlaylist();
      } else {
        showIdle('이 기기는 아직 구역(그룹)에 배정되지 않았습니다.');
      }
    } else {
      showIdle('이 기기는 아직 서버에 등록되지 않았습니다.');
    }
  } catch (err) {
    console.error('기기 정보 조회 실패:', err);
    setTimeout(fetchDeviceConfig, 5000); // 5초 뒤 재시도
  }
}

// 2. 그룹의 재생목록 가져오기
async function fetchPlaylist() {
  if (!groupId) return;
  try {
    const res = await axios.get(`${API_URL}/api/groups/${groupId}/playlist`);
    const data = res.data;
    
    if (data && data.medias && data.medias.length > 0) {
      // 내 기기 전용 미디어이거나, 전체 대상인 미디어만 필터링
      playlist = data.medias.filter(m => !m.targetDeviceId || m.targetDeviceId === machineId);
      
      if (playlist.length > 0) {
        document.getElementById('loader').style.display = 'none';
        currentIndex = 0;
        playMedia();
      } else {
        showIdle('현재 기기에 할당된 미디어가 없습니다.');
      }
    } else {
      showIdle('그룹에 재생목록이 비어있습니다.');
    }
  } catch (err) {
    console.error('재생목록 가져오기 실패:', err);
  }
}

function showIdle(msg) {
  document.getElementById('loader').style.display = 'flex';
  document.getElementById('status-text').innerText = msg;
  clearTimeout(playTimer);
  document.getElementById('media-layer-1').style.opacity = '0';
  document.getElementById('media-layer-2').style.opacity = '0';
}

// 3. 미디어 무한 반복 재생
function playMedia() {
  if (playlist.length === 0) return;
  
  clearTimeout(playTimer);

  const item = playlist[currentIndex];
  const media = item.media;
  const duration = (item.duration || 10) * 1000;
  
  const nextLayer = activeLayer === 1 ? 2 : 1;
  const currentLayerEl = document.getElementById(`media-layer-${activeLayer}`);
  const nextLayerEl = document.getElementById(`media-layer-${nextLayer}`);

  // 미디어 DOM 생성
  if (media.type === 'video') {
    nextLayerEl.innerHTML = `<video src="${API_URL}${media.path}" autoplay muted loop style="width:100%; height:100%; object-fit:cover;"></video>`;
  } else {
    nextLayerEl.innerHTML = `<img src="${API_URL}${media.path}" style="width:100%; height:100%; object-fit:cover;">`;
  }

  // 전환 효과 (Crossfade)
  nextLayerEl.style.opacity = '1';
  nextLayerEl.style.zIndex = '2';
  
  currentLayerEl.style.zIndex = '1';
  currentLayerEl.style.opacity = '0';
  
  setTimeout(() => {
    currentLayerEl.innerHTML = '';
  }, 1000); // 1초 뒤 숨겨진 레이어 비우기

  activeLayer = nextLayer;

  // 다음 순서 예약
  playTimer = setTimeout(() => {
    currentIndex = (currentIndex + 1) % playlist.length;
    playMedia();
  }, duration);
}

init();
