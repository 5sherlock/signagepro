import { useState, useEffect } from 'react';
import { Activity, Monitor, Film, Settings, LayoutGrid, Plus } from 'lucide-react';
import { io } from 'socket.io-client';
import GroupManager from './components/GroupManager';
import MediaManager from './components/MediaManager';

const SOCKET_URL = 'http://localhost:3000';

function DevicePreview({ groupId, deviceId }) {
  const [playlist, setPlaylist] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [prevIndex, setPrevIndex] = useState(-1);
  const [isTransitioning, setIsTransitioning] = useState(false);

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
  
  // 현재 아이템의 전환 효과 정보 (루프 시 첫 아이템의 정보를 사용하게 됨)
  const transType = activeItem?.transition || 'fade';
  const transTime = activeItem?.transitionTime || 1000;

  useEffect(() => {
    if (!playlist.length) return;
    
    // 전환 시작
    setIsTransitioning(true);
    
    // 전환 완료 처리
    const transTimer = setTimeout(() => {
      setIsTransitioning(false);
    }, transTime);

    // 다음 미디어로 이동
    const timer = setTimeout(() => {
      setPrevIndex(currentIndex);
      setCurrentIndex((prev) => (prev + 1) % playlist.length);
    }, (activeItem?.duration || 10) * 1000);

    return () => {
      clearTimeout(timer);
      clearTimeout(transTimer);
    };
  }, [currentIndex, playlist.length, transTime, activeItem?.duration]);

  if (playlist.length === 0) {
    return <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#444' }}>No Media</div>;
  }

  let transitionClass = '';
  if (transType === 'slide') transitionClass = 'preview-slide';
  else if (transType === 'fade') transitionClass = 'preview-fade';
  else if (transType === 'crossfade') transitionClass = 'preview-crossfade';

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
      {/* 이전 미디어 (전환 중일 때 서서히 사라짐) */}
      {prevItem && isTransitioning && transType === 'crossfade' && (
        <div 
          className="preview-fade-out"
          style={{ 
            position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 1,
            '--trans-time': `${transTime}ms`
          }}
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

  const fetchDevices = () => {
    fetch(`${SOCKET_URL}/api/devices`)
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
                </select>
                <button className="btn btn-primary" style={{ marginLeft: '12px' }}>
                  <Plus size={18} />
                  기기 추가
                </button>
              </div>
            </header>

            <div className="content-area">
              <div className={gridClass}>
                {filteredDevices.map((device, index) => (
                  <div 
                    key={device.id} 
                    className="glass-card device-card animate-fade-in"
                    style={{ animationDelay: `${index * 0.1}s` }}
                  >
                    {/* PiP 화면 영역 */}
                    <div className="device-thumbnail">
                      <div className="pip-badge">
                        <Monitor size={10} color={device.status === 'online' ? '#10B981' : '#EF4444'} /> 
                        {device.status === 'online' ? 'LIVE' : 'NO SIGNAL'}
                      </div>
                      {device.status === 'online' ? (
                        <div style={{ width: '100%', height: '100%', background: '#000' }}>
                          <DevicePreview groupId={device.groupId} deviceId={device.id} />
                        </div>
                      ) : (
                        <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#666', background: '#111' }}>
                          신호 없음
                        </div>
                      )}
                    </div>

                    <div className="device-header">
                      <div>
                        <h3 className="device-name">{device.name}</h3>
                        <div className="device-group" style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                          {device.storeName} &gt; {device.groupName}
                        </div>
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
            <GroupManager devices={devices} fetchDevices={fetchDevices} stores={stores} fetchStores={fetchStores} groups={groups} fetchGroups={fetchGroups} />
          </div>
        )}
        {activeTab === 'media' && (
          <div className="content-area" style={{ paddingTop: '10px' }}>
            <MediaManager stores={stores} groups={groups} devices={devices} fetchDevices={fetchDevices} />
          </div>
        )}
      </main>
    </div>
  );
}

export default App;
