import { useState, useEffect } from 'react';
import { Activity, Monitor, Film, Settings, LayoutGrid, Plus } from 'lucide-react';
import { io } from 'socket.io-client';
import GroupManager from './components/GroupManager';
import { SOCKET_URL } from './config';
import MediaManager from './components/MediaManager';

function DevicePreview({ groupId, deviceId, onUpdate }) {
  const [playlist, setPlaylist] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [prevIndex, setPrevIndex] = useState(-1);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  useEffect(() => {
    if (!groupId) return;
    
    const loadPlaylist = () => {
      fetch(`${SOCKET_URL}/api/groups/${groupId}/playlist`)
        .then(res => res.json())
        .then(data => {
          if (data && data.medias) {
            const filtered = data.medias.filter(m => m.targetDeviceId === deviceId || !m.targetDeviceId);
            setPlaylist(filtered);
            setCurrentIndex(0);
            setPrevIndex(-1);
            setIsTransitioning(false);
          } else {
            setPlaylist([]);
          }
        })
        .catch(err => console.error('미리보기 불러오기 실패:', err));
    };

    loadPlaylist();

    const socket = io(SOCKET_URL);
    socket.on('playlist_updated', (data) => {
      if (data.groupId === groupId) {
        loadPlaylist();
      }
    });

    return () => socket.disconnect();
  }, [groupId, deviceId]);

  const activeItem = playlist[currentIndex];
  const prevItem = prevIndex !== -1 ? playlist[prevIndex] : null;
  
  // 전환 효과는 현재 나타날 미디어(activeItem)의 설정을 따름
  const transType = activeItem?.transition || 'fade';
  const transTime = activeItem?.transitionTime || 1000;

  useEffect(() => {
    if (!playlist.length) return;
    
    // 전환 완료 처리 (transTime 이후 전환 클래스 제거 및 이전 미디어 언마운트)
    const transTimer = setTimeout(() => {
      setIsTransitioning(false);
    }, transTime);

    // 다음 미디어로 이동
    const timer = setTimeout(() => {
      setIsTransitioning(true);
      setPrevIndex(currentIndex);
      setCurrentIndex((prev) => (prev + 1) % playlist.length);
    }, (activeItem?.duration || 10) * 1000);

    return () => {
      clearTimeout(timer);
      clearTimeout(transTimer);
    };
  }, [currentIndex, playlist.length, transTime, activeItem?.duration]);

  useEffect(() => {
    setCurrentTime(0);
    const interval = setInterval(() => {
      setCurrentTime(prev => {
        const next = prev + 1;
        return next > (activeItem?.duration || 10) ? prev : next;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [currentIndex, activeItem?.duration]);

  useEffect(() => {
    if (onUpdate) {
      onUpdate({
        filename: activeItem?.media?.filename || 'No Media',
        currentTime,
        duration: activeItem?.duration || 10,
        transType,
        transTime
      });
    }
  }, [activeItem, currentTime, transType, transTime, onUpdate]);

  if (playlist.length === 0) {
    return <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#444' }}>No Media</div>;
  }

  let transitionClass = '';
  if (transType === 'slide') transitionClass = 'preview-slide';
  else if (transType === 'fade') transitionClass = 'preview-fade';
  else if (transType === 'dissolve') transitionClass = 'preview-dissolve';

  const renderMedia = (item) => {
    if (!item) return null;
    const media = item.media;
    if (media.type === 'video') {
      return (
        <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#000', color: '#fff', fontSize: '0.7rem' }}>
          VIDEO: {media.filename}
        </div>
      );
    }
    return (
      <img 
        src={`${SOCKET_URL}${media.path}`} 
        alt="Preview" 
        style={{ width: '100%', height: '100%', objectFit: 'cover' }} 
      />
    );
  };

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%', overflow: 'hidden', background: '#000' }}>
      {/* 이전 미디어 (전환 중일 때 사라짐) */}
      {/* 이전 미디어 (전환 중일 때) */}
      {isTransitioning && prevItem && (
        <div 
          style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 1 }}
        >
          {renderMedia(prevItem)}
        </div>
      )}
      
      {/* 현재 미디어 */}
      <div 
        key={`${activeItem.id}-${currentIndex}`} 
        className={transitionClass} 
        style={{ 
          position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 2,
          '--trans-time': `${transTime}ms`
        }}
      >
        {renderMedia(activeItem)}
      </div>
    </div>
  );
}

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [devices, setDevices] = useState([]);
  const [groups, setGroups] = useState([]);
  const [stores, setStores] = useState([]);
  const [selectedStoreId, setSelectedStoreId] = useState('');
  const [gridLayout, setGridLayout] = useState('auto');
  const [deviceMeta, setDeviceMeta] = useState({});
  const [customGrid, setCustomGrid] = useState({ rows: 2, cols: 3 });

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const fetchDevices = () => {
    fetch(`${SOCKET_URL}/api/devices`, { cache: 'no-store' })
      .then(res => res.json())
      .then(data => {
        const mappedDevices = data.map(d => ({
          ...d,
          groupName: d.group?.name || '미배정 기기',
          storeName: d.store?.name || '미배정 사업장'
        }));
        setDevices(mappedDevices);
      })
      .catch(err => console.error('기기 목록 불러오기 실패:', err));
  };

  const fetchGroups = () => {
    fetch(`${SOCKET_URL}/api/groups`)
      .then(res => res.json())
      .then(setGroups)
      .catch(err => console.error('그룹 목록 불러오기 실패:', err));
  };

  const fetchStores = () => {
    fetch(`${SOCKET_URL}/api/stores`)
      .then(res => res.json())
      .then(data => {
        setStores(data);
        if (data.length > 0 && !selectedStoreId) {
          setSelectedStoreId(data[0].id);
        }
      })
      .catch(err => console.error('사업장 목록 불러오기 실패:', err));
  };

  useEffect(() => {
    fetchDevices();
    fetchGroups();
    fetchStores();

    const socket = io(SOCKET_URL);

    socket.on('device_status_update', (update) => {
      setDevices(prev => {
        const exists = prev.find(d => d.id === update.deviceId);
        if (exists) {
          return prev.map(d => 
            d.id === update.deviceId 
              ? { ...d, status: update.status, cpuUsage: update.cpu || d.cpuUsage, memUsage: update.mem || d.memUsage }
              : d
          );
        } else {
          fetchDevices();
          return prev;
        }
      });
    });

    socket.on('group_assignment_changed', () => {
      fetchDevices();
      fetchGroups();
    });

    return () => socket.disconnect();
  }, []);

  const filteredDevices = devices.filter(d => {
    // 그룹에 미배정된 기기는 대시보드에서 제외
    if (!d.groupId) return false;
    if (selectedStoreId === 'all') return true;
    if (selectedStoreId === 'unassigned') return !d.storeId;
    return d.storeId === selectedStoreId;
  });

  const gridClass = gridLayout === 'auto' 
    ? `dashboard-grid count-${filteredDevices.length > 0 && filteredDevices.length <= 6 ? filteredDevices.length : filteredDevices.length > 6 ? 'many' : '1'}`
    : `dashboard-grid layout-${gridLayout}`;

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="logo-container">
          <Activity size={32} color="#3B82F6" />
          <span className="logo-text">SignagePro</span>
        </div>
        
        <nav className="nav-links">
          <a className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`} onClick={() => setActiveTab('dashboard')}>
            <LayoutGrid size={20} />
            <span>대시보드 (관제)</span>
          </a>
          <a className={`nav-item ${activeTab === 'groups' ? 'active' : ''}`} onClick={() => setActiveTab('groups')}>
            <Monitor size={20} />
            <span>사업장 및 그룹 관리</span>
          </a>
          <a className={`nav-item ${activeTab === 'media' ? 'active' : ''}`} onClick={() => setActiveTab('media')}>
            <Film size={20} />
            <span>미디어 스케줄링</span>
          </a>
          <a className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => setActiveTab('settings')}>
            <Settings size={20} />
            <span>환경설정</span>
          </a>
        </nav>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        {activeTab === 'dashboard' && (
          <>
            <header className="header">
              <h1 className="header-title">실시간 기기 관제</h1>
              <div className="layout-controls">
                <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>사업장:</span>
                <select 
                  value={selectedStoreId} 
                  onChange={(e) => setSelectedStoreId(e.target.value)}
                  className="glass-select"
                >
                  {stores.map(s => (
                    <option key={s.id} value={s.id}>{s.name}</option>
                  ))}
                </select>

                <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginLeft: '12px' }}>그리드:</span>
                <select 
                  value={gridLayout} 
                  onChange={(e) => setGridLayout(e.target.value)}
                  className="glass-select"
                >
                  <option value="auto">자동 조절 (Auto)</option>
                  <option value="1x1">1 x 1 (크게 보기)</option>
                  <option value="2x1">2 x 1</option>
                  <option value="2x2">2 x 2</option>
                  <option value="3x2">3 x 2</option>
                  <option value="3x3">3 x 3</option>
                  <option value="4x3">4 x 3</option>
                  <option value="custom">직접 입력</option>
                </select>
                {gridLayout === 'custom' && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginLeft: '12px', background: 'rgba(255,255,255,0.05)', padding: '4px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.1)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>가로:</span>
                      <input 
                        type="number" 
                        className="glass-input" 
                        style={{ width: '50px', height: '28px', padding: '0 6px', fontSize: '0.8rem' }}
                        value={customGrid.cols}
                        onChange={(e) => setCustomGrid(prev => ({ ...prev, cols: parseInt(e.target.value) || 1 }))}
                        min="1"
                      />
                    </div>
                    <span style={{ color: 'rgba(255,255,255,0.2)' }}>|</span>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>세로:</span>
                      <input 
                        type="number" 
                        className="glass-input" 
                        style={{ width: '50px', height: '28px', padding: '0 6px', fontSize: '0.8rem' }}
                        value={customGrid.rows}
                        onChange={(e) => setCustomGrid(prev => ({ ...prev, rows: parseInt(e.target.value) || 1 }))}
                        min="1"
                      />
                    </div>
                  </div>
                )}
                <button className="btn btn-primary" style={{ marginLeft: '12px' }} onClick={() => setActiveTab('groups')}>
                  <Plus size={18} />
                  기기 추가
                </button>
              </div>
            </header>

            <div className="content-area">
              <div 
                className={`dashboard-grid ${gridLayout !== 'custom' ? (gridLayout === 'auto' ? `count-${filteredDevices.length > 0 && filteredDevices.length <= 6 ? filteredDevices.length : filteredDevices.length > 6 ? 'many' : '1'}` : `layout-${gridLayout}`) : ''}`}
                style={gridLayout === 'custom' ? {
                  gridTemplateColumns: `repeat(${customGrid.cols}, 1fr)`,
                  gridTemplateRows: `repeat(${customGrid.rows}, auto)`,
                  gridAutoFlow: 'row',
                  justifyContent: 'flex-start',
                  justifyItems: 'flex-start',
                  maxWidth: 'none',
                  width: '100%'
                } : {}}
              >
                {filteredDevices.map((device, index) => (
                  <div 
                    key={device.id} 
                    className="glass-card device-card animate-fade-in"
                    style={{ animationDelay: `${index * 0.1}s` }}
                  >
                    {/* PiP 화면 영역 */}
                    <div className="thumbnail-wrapper">
                      <div className="device-thumbnail">
                        <div className="pip-badge">
                          <Monitor size={10} color={device.status === 'online' ? '#10B981' : '#EF4444'} /> 
                          {device.status === 'online' ? 'LIVE' : 'NO SIGNAL'}
                        </div>
                        {device.status === 'online' ? (
                          <DevicePreview 
                            groupId={device.groupId} 
                            deviceId={device.id} 
                            onUpdate={(meta) => setDeviceMeta(prev => ({...prev, [device.id]: meta}))}
                          />
                        ) : (
                          <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#666', background: '#111' }}>
                            신호 없음
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="device-header">
                      <div>
                        <h3 className="device-name">{device.name}</h3>
                        <div className="device-group" style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                          {device.storeName} &gt; {device.groupName}
                        </div>
                        {deviceMeta[device.id] && (
                          <div style={{ marginTop: '4px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', marginBottom: '2px', overflow: 'hidden' }}>
                              <Film size={10} style={{ color: 'var(--text-secondary)' }} />
                              <span style={{ fontSize: '0.65rem', color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                {deviceMeta[device.id].filename}
                              </span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <span style={{ fontFamily: 'monospace', fontSize: '0.65rem', color: 'var(--text-secondary)' }}>
                                {formatTime(deviceMeta[device.id].currentTime)} / {formatTime(deviceMeta[device.id].duration)}
                              </span>
                              <span style={{ fontSize: '0.6rem', color: '#60a5fa', fontWeight: 600, textTransform: 'uppercase' }}>
                                {deviceMeta[device.id].transType}
                              </span>
                            </div>
                          </div>
                        )}
                      </div>
                      <div className={`status-badge ${device.status}`}>
                        <span className="status-dot"></span>
                        {device.status === 'online' ? '온라인' : '오프라인'}
                      </div>
                    </div>

                    {device.status === 'online' && (
                      <div className="metrics-container">
                        <div className="metric-box">
                          <div className="metric-label">CPU 사용량</div>
                          <div className="metric-value">{device.cpuUsage || 0}%</div>
                          <div className="progress-bg">
                            <div 
                              className={`progress-fill ${device.cpuUsage > 80 ? 'danger' : ''}`} 
                              style={{ width: `${device.cpuUsage || 0}%` }}
                            ></div>
                          </div>
                        </div>
                        <div className="metric-box">
                          <div className="metric-label">메모리 사용량</div>
                          <div className="metric-value">{device.memUsage || 0}%</div>
                          <div className="progress-bg">
                            <div 
                              className={`progress-fill ${device.memUsage > 80 ? 'danger' : ''}`} 
                              style={{ width: `${device.memUsage || 0}%` }}
                            ></div>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </>
        )}

        {activeTab === 'groups' && (
          <div className="content-area" style={{ paddingTop: '10px' }}>
            <GroupManager 
              devices={devices} 
              fetchDevices={fetchDevices} 
              stores={stores} 
              fetchStores={fetchStores} 
              groups={groups} 
              fetchGroups={fetchGroups} 
              selectedStoreId={selectedStoreId}
              setSelectedStoreId={setSelectedStoreId}
            />
          </div>
        )}
        {activeTab === 'media' && (
          <div className="content-area" style={{ paddingTop: '10px' }}>
            <MediaManager 
              stores={stores} 
              groups={groups} 
              devices={devices}
              fetchDevices={fetchDevices}
              fetchGroups={fetchGroups}
              selectedStoreId={selectedStoreId}
              setSelectedStoreId={setSelectedStoreId}
            />
          </div>
        )}
        {activeTab === 'settings' && (
          <div className="content-area">
            <header className="header">
              <h1 className="header-title">환경설정</h1>
            </header>
            <div className="glass-card" style={{ maxWidth: '600px', padding: '30px', marginTop: '20px' }}>
              <h2 style={{ marginBottom: '20px', fontSize: '1.2rem' }}>서버 연결 설정</h2>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)' }}>중앙 서버 주소 (API/Socket)</label>
                <input 
                  type="text" 
                  className="glass-input" 
                  placeholder="예: http://192.168.0.10:3000"
                  defaultValue={localStorage.getItem('SIGNAGE_SERVER_URL') || SOCKET_URL}
                  id="server-url-input"
                  style={{ width: '100%', padding: '12px' }}
                />
                <p style={{ marginTop: '8px', fontSize: '0.8rem', color: '#94a3b8' }}>
                  입력된 주소로 모든 데이터 요청 및 실시간 통신이 이루어집니다. 변경 후 앱이 다시 시작됩니다.
                </p>
              </div>
              <button 
                className="btn btn-primary"
                onClick={() => {
                  const val = document.getElementById('server-url-input').value;
                  localStorage.setItem('SIGNAGE_SERVER_URL', val);
                  alert('서버 주소가 저장되었습니다. 앱을 재시작합니다.');
                  window.location.reload();
                }}
              >
                설정 저장 및 적용
              </button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

export default App;
