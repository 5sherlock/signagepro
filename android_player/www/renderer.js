// Android / Browser Renderer
const io = window.io;
const axios = window.axios;

let machineId = localStorage.getItem('device_id');
if (!machineId) {
  machineId = 'ANDROID-' + Math.random().toString(36).substring(2, 8).toUpperCase();
  localStorage.setItem('device_id', machineId);
}

let API_URL = localStorage.getItem('server_url') || 'http://172.30.1.26:3000';
if (API_URL === 'http://192.168.0.10:3000') {
  API_URL = 'http://172.30.1.26:3000';
  localStorage.setItem('server_url', API_URL);
}
let groupId = null;
let playlist = [];
let currentIndex = 0;
let playTimer = null;
let activeLayer = 1;
let socket = null;

async function init() {
  document.getElementById('device-id').innerText = machineId;
  const savedName = localStorage.getItem('device_name') || 'Web-' + machineId;
  document.getElementById('device-name-display').innerText = savedName;
  document.getElementById('ip-input').value = API_URL;

  connectServer();
}

function connectServer() {
  if (socket) socket.disconnect();
  socket = io(API_URL);
  
  socket.on('connect', () => {
    document.getElementById('status-text').innerText = '서버에 연결되었습니다. 동기화 중...';
    
    setInterval(() => {
      const curName = localStorage.getItem('device_name') || 'Web-' + machineId;
      socket.emit('web_player_heartbeat', { deviceId: machineId, name: curName });
    }, 5000);
    const curName = localStorage.getItem('device_name') || 'Web-' + machineId;
    socket.emit('web_player_heartbeat', { deviceId: machineId, name: curName });
    
    setTimeout(fetchDeviceConfig, 1000);
  });

  socket.on('group_assignment_changed', () => {
    fetchDeviceConfig();
  });

  socket.on('playlist_updated', (data) => {
    if (data.groupId === groupId) {
      fetchPlaylist();
    }
  });

  socket.on('device_deleted', () => {
    console.log('이 기기는 서버에서 영구 삭제되었습니다. 로컬 정보를 초기화합니다.');
    groupId = null;
    playlist = [];
    localStorage.removeItem('device_name');
    localStorage.removeItem('device_id');
    showIdle('이 기기는 관리 시스템에서 삭제되어 설정이 초기화되었습니다. 잠시 후 새로운 상태로 재시작합니다.');
    setTimeout(() => {
      window.location.reload();
    }, 3000);
  });

  socket.on('connect_error', (err) => {
    console.error('소켓 연결 오류:', err);
    document.getElementById('status-text').innerHTML = `서버 연결 실패: <span style="color: #f87171;">${err.message}</span><br><span style="font-size: 0.8rem; color: #aaa;">(${API_URL})</span>`;
  });

  socket.on('disconnect', () => {
    showIdle('서버와의 연결이 끊어졌습니다. 재연결 대기 중...');
  });
}

async function fetchDeviceConfig() {
  try {
    const res = await axios.get(`${API_URL}/api/devices`);
    const device = res.data.find(d => d.id === machineId);
    
    if (device) {
      // 대시보드(관리자 페이지)에서 수정된 이름을 기기에 실시간 동기화
      if (device.name && device.name !== localStorage.getItem('device_name')) {
        localStorage.setItem('device_name', device.name);
        const nameDisplay = document.getElementById('device-name-display');
        if (nameDisplay) {
          nameDisplay.innerText = device.name;
        }
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
    setTimeout(fetchDeviceConfig, 5000);
  }
}

async function fetchPlaylist() {
  if (!groupId) return;
  try {
    const res = await axios.get(`${API_URL}/api/groups/${groupId}/playlist`);
    const data = res.data;
    
    if (data && data.medias && data.medias.length > 0) {
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

function playMedia() {
  if (playlist.length === 0) return;
  
  clearTimeout(playTimer);

  const totalDuration = playlist.reduce((acc, item) => acc + (item.duration || 10) * 1000, 0);
  const nowMs = Date.now();
  const timeIntoLoop = nowMs % totalDuration;
  
  let accumulated = 0;
  let targetIndex = 0;
  let timeIntoItem = 0;
  let itemDuration = 10000;
  
  for (let i = 0; i < playlist.length; i++) {
    const itemDur = (playlist[i].duration || 10) * 1000;
    if (timeIntoLoop >= accumulated && timeIntoLoop < accumulated + itemDur) {
      targetIndex = i;
      timeIntoItem = timeIntoLoop - accumulated;
      itemDuration = itemDur;
      break;
    }
    accumulated += itemDur;
  }

  currentIndex = targetIndex;
  const item = playlist[currentIndex];
  const media = item.media;
  
  const nextLayer = activeLayer === 1 ? 2 : 1;
  const currentLayerEl = document.getElementById(`media-layer-${activeLayer}`);
  const nextLayerEl = document.getElementById(`media-layer-${nextLayer}`);

  if (media.type === 'video') {
    nextLayerEl.innerHTML = `<video src="${API_URL}${media.path}" autoplay muted loop style="width:100%; height:100%; object-fit:cover;"></video>`;
  } else {
    nextLayerEl.innerHTML = `<img src="${API_URL}${media.path}" style="width:100%; height:100%; object-fit:cover;">`;
  }

  // 전환 효과 설정 (아이템에 설정된 transitionTime 및 transition 형식 사용)
  const transTime = item.transitionTime || 1000;
  const transType = (item.transition || 'fade').toLowerCase();

  if (transType === 'fade') {
    // FADE: 검정 화면으로 완전히 사라진 후 다음 미디어 나타남 (2단계 진행)
    const halfTime = transTime / 2;
    
    // 1단계: 현재 레이어 서서히 페이드 아웃
    currentLayerEl.style.transition = `opacity ${halfTime}ms ease-in-out`;
    currentLayerEl.style.opacity = '0';
    currentLayerEl.style.zIndex = '2';
    
    nextLayerEl.style.transition = 'none';
    nextLayerEl.style.opacity = '0';
    nextLayerEl.style.zIndex = '1';

    // 2단계: 절반 시간 후 다음 레이어 페이드 인 시작
    setTimeout(() => {
      nextLayerEl.style.transition = `opacity ${halfTime}ms ease-in-out`;
      nextLayerEl.style.opacity = '1';
      nextLayerEl.style.zIndex = '2';
      currentLayerEl.style.zIndex = '1';
      currentLayerEl.innerHTML = '';
    }, halfTime);
  } else if (transType === 'dissolve') {
    // DISSOLVE: 교차 페이드 (동시 진행)
    nextLayerEl.style.transition = `opacity ${transTime}ms ease-in-out`;
    currentLayerEl.style.transition = `opacity ${transTime}ms ease-in-out`;

    nextLayerEl.style.opacity = '1';
    nextLayerEl.style.zIndex = '2';
    
    currentLayerEl.style.zIndex = '1';
    currentLayerEl.style.opacity = '0';
    
    setTimeout(() => {
      currentLayerEl.innerHTML = '';
    }, transTime);
  } else {
    // NONE 또는 기타: 즉시 전환 (Cut)
    nextLayerEl.style.transition = 'none';
    currentLayerEl.style.transition = 'none';
    
    nextLayerEl.style.opacity = '1';
    nextLayerEl.style.zIndex = '2';
    
    currentLayerEl.style.zIndex = '1';
    currentLayerEl.style.opacity = '0';
    currentLayerEl.innerHTML = '';
  }

  activeLayer = nextLayer;

  const remaining = itemDuration - timeIntoItem;
  playTimer = setTimeout(() => {
    playMedia();
  }, Math.max(50, remaining));
}

document.addEventListener('DOMContentLoaded', init);

// TV Remote D-pad Navigation Controller
let focusedElementIndex = 0;
document.addEventListener('keydown', (e) => {
  const modal = document.getElementById('settings-modal');
  const ipInput = document.getElementById('ip-input');
  const saveBtn = document.getElementById('save-btn');
  const cancelBtn = document.getElementById('cancel-btn');

  // If modal is closed
  if (modal.style.display !== 'flex') {
    // Pressing Enter (D-pad Center) or D-pad buttons opens settings
    if (e.key === 'Enter' || e.key === 'ArrowUp' || e.key === 'ArrowDown' || e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
      ipInput.value = API_URL;
      modal.style.display = 'flex';
      ipInput.focus();
      focusedElementIndex = 0;
      e.preventDefault();
    }
    return;
  }

  // If modal is open
  if (e.key === 'Backspace' && focusedElementIndex === 0) {
    return; // Allow backspace inside input
  }

  if (e.key === 'ArrowDown') {
    if (focusedElementIndex === 0) {
      focusedElementIndex = 1;
      saveBtn.focus();
    }
    e.preventDefault();
  } else if (e.key === 'ArrowUp') {
    if (focusedElementIndex > 0) {
      focusedElementIndex = 0;
      ipInput.focus();
    }
    e.preventDefault();
  } else if (e.key === 'ArrowLeft') {
    if (focusedElementIndex === 2) {
      focusedElementIndex = 1;
      saveBtn.focus();
    }
    e.preventDefault();
  } else if (e.key === 'ArrowRight') {
    if (focusedElementIndex === 1) {
      focusedElementIndex = 2;
      cancelBtn.focus();
    }
    e.preventDefault();
  } else if (e.key === 'Enter') {
    if (focusedElementIndex === 0) {
      focusedElementIndex = 1;
      saveBtn.focus();
    } else if (focusedElementIndex === 1) {
      saveBtn.click();
    } else if (focusedElementIndex === 2) {
      cancelBtn.click();
    }
    e.preventDefault();
  } else if (e.key === 'Escape') {
    modal.style.display = 'none';
    e.preventDefault();
  }
});
