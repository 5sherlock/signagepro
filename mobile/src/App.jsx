import { useState, useEffect, useCallback } from 'react';
import { LayoutGrid, Calendar, LogOut, Activity, RotateCw, RefreshCw, Sun, Moon, Plus, Trash2, Check, Edit, ChevronUp, ChevronDown } from 'lucide-react';
import { io } from 'socket.io-client';
import { SOCKET_URL, apiFetch, getToken } from './config';
import LoginScreen from './components/LoginScreen';
import DevicePreview from './components/DevicePreview';
import './App.css';

function App() {
  const [authed, setAuthed] = useState(!!getToken());
  const [activeTab, setActiveTab] = useState('dashboard');
  const [devices, setDevices] = useState([]);
  const [stores, setStores] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [selectedStoreId, setSelectedStoreId] = useState(() => {
    return localStorage.getItem('SIGNAGE_MOBILE_STORE_ID') || '';
  });
  const [serverOnline, setServerOnline] = useState(null);
  const [currentTime, setCurrentTime] = useState(new Date());

  // 실시간 시계 타이머
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // 스케줄 수정용 상태
  const [editingId, setEditingId] = useState(null);
  const [editOnHour, setEditOnHour] = useState('');
  const [editOnMinute, setEditOnMinute] = useState('');
  const [editOffHour, setEditOffHour] = useState('');
  const [editOffMinute, setEditOffMinute] = useState('');
  const [editDays, setEditDays] = useState([]);

  // 데이터 로딩
  const fetchInitialData = useCallback(async () => {
    try {
      // 기기 조회
      const resDev = await apiFetch(`${SOCKET_URL}/api/devices`);
      if (resDev.status === 401) { setAuthed(false); return; }
      const dataDev = await resDev.json();
      if (dataDev) setDevices(dataDev);

      // 사업장 조회
      const resStores = await apiFetch(`${SOCKET_URL}/api/stores`);
      if (resStores.ok) {
        const dataStores = await resStores.json();
        setStores(dataStores);
      }

      // 스케줄 조회
      const resSched = await apiFetch(`${SOCKET_URL}/api/schedules`);
      if (resSched.ok) {
        const dataSched = await resSched.json();
        setSchedules(dataSched);
      }
    } catch (err) {
      setServerOnline(false);
    }
  }, []);

  useEffect(() => {
    if (!authed) return;
    fetchInitialData();

    const socket = io(SOCKET_URL, { auth: { token: getToken() } });
    socket.on('connect', () => setServerOnline(true));
    socket.on('device_status_update', () => fetchInitialData());
    socket.on('screen_schedule', () => {
      // 스케줄 변경 시 자동 갱신
      apiFetch(`${SOCKET_URL}/api/schedules`)
        .then(res => res.ok && res.json())
        .then(data => data && setSchedules(data))
        .catch(() => {});
    });

    return () => socket.disconnect();
  }, [authed, fetchInitialData]);

  // 기기 제어 명령 전송
  const sendCommand = async (deviceId, endpoint, body = {}) => {
    try {
      const r = await apiFetch(`${SOCKET_URL}/api/devices/${deviceId}/${endpoint}`, {
        method: 'POST',
        body: JSON.stringify(body),
      });
      const d = await r.json();
      if (!d.ok) alert(`오류: ${d.error}`);
    } catch {
      alert('명령 전송 실패');
    }
  };

  // 스케줄 수정 핸들러
  const startEditing = (sched) => {
    setEditingId(sched.id);
    if (sched.onTime) {
      const [h, m] = sched.onTime.split(':');
      setEditOnHour(h);
      setEditOnMinute(m);
    } else {
      setEditOnHour('');
      setEditOnMinute('');
    }
    if (sched.offTime) {
      const [h, m] = sched.offTime.split(':');
      setEditOffHour(h);
      setEditOffMinute(m);
    } else {
      setEditOffHour('');
      setEditOffMinute('');
    }
    setEditDays(sched.days ? sched.days.split(',').map(Number) : []);
  };

  const cancelEditing = () => {
    setEditingId(null);
  };

  const toggleEditDay = (dayNum) => {
    setEditDays(prev =>
      prev.includes(dayNum) ? prev.filter(d => d !== dayNum) : [...prev, dayNum]
    );
  };
  const adjustTimeValue = (field, direction) => {
    let currentVal = 0;
    let max = 23;
    let setter = null;

    if (field === 'onHour') {
      currentVal = parseInt(editOnHour, 10) || 0;
      max = 23;
      setter = setEditOnHour;
    } else if (field === 'onMinute') {
      currentVal = parseInt(editOnMinute, 10) || 0;
      max = 59;
      setter = setEditOnMinute;
    } else if (field === 'offHour') {
      currentVal = parseInt(editOffHour, 10) || 0;
      max = 23;
      setter = setEditOffHour;
    } else if (field === 'offMinute') {
      currentVal = parseInt(editOffMinute, 10) || 0;
      max = 59;
      setter = setEditOffMinute;
    }

    if (!setter) return;

    let newVal = currentVal;
    if (direction === 'up') {
      newVal = currentVal + 1;
      if (newVal > max) newVal = 0;
    } else {
      newVal = currentVal - 1;
      if (newVal < 0) newVal = max;
    }

    setter(String(newVal).padStart(2, '0'));
  };

  const handleSaveEdit = async (id) => {
    // 켜기 시간 검증
    let finalOnTime = null;
    if (editOnHour !== '' || editOnMinute !== '') {
      const h = editOnHour === '' ? '0' : editOnHour;
      const m = editOnMinute === '' ? '0' : editOnMinute;
      const hNum = parseInt(h, 10);
      const mNum = parseInt(m, 10);
      if (isNaN(hNum) || hNum < 0 || hNum > 23 || isNaN(mNum) || mNum < 0 || mNum > 59) {
        alert('켜기 시간은 0~23시, 분은 0~59분 사이여야 합니다.');
        return;
      }
      finalOnTime = `${String(hNum).padStart(2, '0')}:${String(mNum).padStart(2, '0')}`;
    }

    // 끄기 시간 검증
    let finalOffTime = null;
    if (editOffHour !== '' || editOffMinute !== '') {
      const h = editOffHour === '' ? '0' : editOffHour;
      const m = editOffMinute === '' ? '0' : editOffMinute;
      const hNum = parseInt(h, 10);
      const mNum = parseInt(m, 10);
      if (isNaN(hNum) || hNum < 0 || hNum > 23 || isNaN(mNum) || mNum < 0 || mNum > 59) {
        alert('끄기 시간은 0~23시, 분은 0~59분 사이여야 합니다.');
        return;
      }
      finalOffTime = `${String(hNum).padStart(2, '0')}:${String(mNum).padStart(2, '0')}`;
    }

    if (!finalOnTime && !finalOffTime) {
      alert('켜기 시간 또는 끄기 시간 중 하나는 입력해야 합니다.');
      return;
    }

    try {
      const existing = schedules.find(s => s.id === id);
      if (!existing) return;

      const payload = {
        id,
        storeId: existing.storeId,
        deviceId: existing.deviceId,
        onTime: finalOnTime,
        offTime: finalOffTime,
        days: editDays.sort().join(','),
        enabled: existing.enabled
      };

      const res = await apiFetch(`${SOCKET_URL}/api/schedules`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        setEditingId(null);
        const data = await apiFetch(`${SOCKET_URL}/api/schedules`).then(r => r.json());
        setSchedules(data);
      } else {
        const errData = await res.json();
        alert(errData.error || '스케줄 수정 실패');
      }
    } catch {
      alert('네트워크 오류');
    }
  };

  // 스케줄 삭제
  const handleDeleteSchedule = async (id) => {
    if (!window.confirm('선택한 스케줄을 삭제하시겠습니까?')) return;
    try {
      const res = await apiFetch(`${SOCKET_URL}/api/schedules/${id}`, {
        method: 'DELETE'
      });
      if (res.ok) {
        setSchedules(prev => prev.filter(s => s.id !== id));
      } else {
        alert('삭제 실패');
      }
    } catch {
      alert('네트워크 오류');
    }
  };

  // 스케줄 사용 여부(활성화) 토글
  const handleToggleSchedule = async (schedule) => {
    try {
      const payload = {
        ...schedule,
        enabled: !schedule.enabled
      };
      const res = await apiFetch(`${SOCKET_URL}/api/schedules`, {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        setSchedules(prev => prev.map(s => s.id === schedule.id ? { ...s, enabled: !s.enabled } : s));
      }
    } catch {
      alert('상태 업데이트 실패');
    }
  };

  // 기기에 스케줄 동기화 명령 전송
  const handleSyncSchedules = async () => {
    try {
      const res = await apiFetch(`${SOCKET_URL}/api/schedules/push`, {
        method: 'POST'
      });
      if (res.ok) {
        alert('기기 스케줄 동기화 명령을 성공적으로 전송했습니다!');
      } else {
        alert('동기화 명령 전송 실패');
      }
    } catch {
      alert('네트워크 에러');
    }
  };

  // 요일 토글 헬퍼
  const toggleDay = (dayNum) => {
    setSelectedDays(prev => 
      prev.includes(dayNum) ? prev.filter(d => d !== dayNum) : [...prev, dayNum]
    );
  };

  const filteredDevices = devices.filter(device => 
    !selectedStoreId || device.storeId === selectedStoreId
  );



  const getGridClass = () => {
    const len = filteredDevices.length;
    if (len <= 1) return 'cols-1';
    if (len === 2) return 'cols-2';
    return 'cols-3';
  };

  // 스케줄 타겟 문자열 변환 헬퍼
  const getScheduleTargetName = (sched) => {
    if (sched.deviceId) {
      const dev = devices.find(d => d.id === sched.deviceId);
      return `기기: ${dev ? dev.name : sched.deviceId}`;
    }
    if (sched.storeId) {
      const st = stores.find(s => s.id === sched.storeId);
      return `사업장: ${st ? st.name : sched.storeId}`;
    }
    return '전체 사업장';
  };

  // 요일 텍스트 헬퍼
  const getDaysText = (daysStr) => {
    const dayMap = { '0': '일', '1': '월', '2': '화', '3': '수', '4': '목', '5': '금', '6': '토' };
    const list = daysStr.split(',');
    if (list.length === 7) return '매일';
    return list.map(d => dayMap[d] || '').join(', ');
  };

  if (!authed) return <LoginScreen onLogin={() => setAuthed(true)} />;

  return (
    <div className="mobile-container">
      {/* 상단 헤더 */}
      <header className="mobile-header">
        <div className="mobile-logo">
          <Activity size={22} />
          <span>SignagePro Mobile</span>
        </div>
        
        <div className="header-right">
          <span className={`status-dot ${serverOnline ? 'online' : 'offline'}`}>
            {serverOnline ? '● 서버 연결됨' : '● 서버 연결 끊김'}
          </span>
          <button className="header-logout-btn" onClick={() => {
            localStorage.removeItem('SIGNAGE_TOKEN');
            window.location.reload();
          }}>
            <LogOut size={16} />
          </button>
        </div>
      </header>

      {/* 메인 콘텐츠 영역 */}
      <main className="mobile-content">
        
        {/* 탭 1: 관제 대시보드 */}
        {activeTab === 'dashboard' && (
          <div className="tab-dashboard-view">
            {/* 사업장 필터 셀렉터 */}
            <div className="store-filter-container">
              <label htmlFor="store-select">사업장 선택</label>
              <select
                id="store-select"
                value={selectedStoreId}
                onChange={(e) => {
                  const val = e.target.value;
                  setSelectedStoreId(val);
                  localStorage.setItem('SIGNAGE_MOBILE_STORE_ID', val);
                }}
                className="mobile-select"
              >
                <option value="">전체 사업장</option>
                {stores.map(store => (
                  <option key={store.id} value={store.id}>{store.name}</option>
                ))}
              </select>
            </div>

            {/* 기기 현황 리스트 */}
            <div className="device-status-section">
              <h2 className="section-title">기기 접속 현황 ({filteredDevices.length}대)</h2>
              
              {filteredDevices.length === 0 ? (
                <div className="empty-devices">등록된 기기가 없습니다.</div>
              ) : (
                <div className={`simple-device-list ${getGridClass()}`}>
                  {filteredDevices.map(device => (
                    <div key={device.id} className="simple-device-card">
                      <div className="device-card-header">
                        <div className="device-name-area">
                          <span className="dev-name">{device.name}</span>
                          <span className="dev-location">
                            {device.group?.name || '미할당'}
                          </span>
                        </div>
                        <div className={`simple-badge ${device.status}`}>
                          {device.status === 'online' ? 'ON' : 'OFF'}
                        </div>
                      </div>

                      {/* 썸네일 미리보기 */}
                      <div className="device-thumbnail-wrapper">
                        {device.status === 'online' ? (
                          <>
                            <DevicePreview
                              groupId={device.groupId}
                              deviceId={device.id}
                              liveSlide={device.slide ?? null}
                            />
                            {device.screenOff && (
                              <div className="screen-off-overlay">
                                <span className="sleep-icon">🌙</span>
                                <span className="sleep-title">화면 꺼짐</span>
                                <span className="sleep-desc">스케줄에 의해 꺼짐</span>
                              </div>
                            )}
                          </>
                        ) : (
                          <div className="no-signal">
                            신호 없음
                          </div>
                        )}
                      </div>

                      {/* 제어 패널 */}
                      <div className="device-card-footer">
                        <button
                          className="control-icon-btn on"
                          onClick={() => sendCommand(device.id, 'screen', { on: true })}
                          disabled={device.status === 'offline'}
                        >
                          <Sun size={18} />
                          <span>화면 켜기</span>
                        </button>
                        
                        <button
                          className="control-icon-btn off"
                          onClick={() => sendCommand(device.id, 'screen', { on: false })}
                          disabled={device.status === 'offline'}
                        >
                          <Moon size={18} />
                          <span>화면 끄기</span>
                        </button>

                        <button
                          className="control-icon-btn restart-app"
                          onClick={() => {
                            if (window.confirm(`${device.name} 앱을 재시작할까요?`)) {
                              sendCommand(device.id, 'restart-app');
                            }
                          }}
                          disabled={device.status === 'offline'}
                        >
                          <RefreshCw size={18} />
                          <span>앱 재시작</span>
                        </button>

                        <button
                          className="control-icon-btn reboot"
                          onClick={() => {
                            if (window.confirm(`${device.name} 기기를 원격 재부팅 하시겠습니까?`)) {
                              sendCommand(device.id, 'reboot');
                            }
                          }}
                          disabled={device.status === 'offline'}
                        >
                          <RotateCw size={18} />
                          <span>재부팅</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* 아이콘 범례 */}
            {filteredDevices.length > 0 && (
              <div className="control-legend">
                <div className="legend-row">
                  <div className="legend-item">
                    <span className="legend-icon on"><Sun size={16} /></span>
                    <span className="legend-name">화면 켜기</span>
                  </div>
                  <div className="legend-item">
                    <span className="legend-icon off"><Moon size={16} /></span>
                    <span className="legend-name">화면 끄기</span>
                  </div>
                  <div className="legend-item">
                    <span className="legend-icon restart-app"><RefreshCw size={16} /></span>
                    <span className="legend-name">앱 재시작</span>
                  </div>
                  <div className="legend-item">
                    <span className="legend-icon reboot"><RotateCw size={16} /></span>
                    <span className="legend-name">재부팅</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* 탭 2: 스케줄 설정 */}
        {activeTab === 'schedules' && (
          <div className="tab-schedule-view">
            {/* 현재 시간 표시 바 */}
            <div className="current-time-bar">
              <span className="time-label">현재 시간</span>
              <span className="time-value">
                {currentTime.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' })} {currentTime.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })}
              </span>
            </div>

            {/* 스케줄 목록 */}
            <div className="schedule-list-section">
              <div className="section-header-row">
                <h2 className="section-title">등록된 화면 스케줄 ({schedules.length}개)</h2>
                <button className="sync-btn" onClick={handleSyncSchedules}>
                  <RotateCw size={14} />
                  <span>즉시 동기화</span>
                </button>
              </div>

              {schedules.length === 0 ? (
                <div className="empty-devices">등록된 스케줄이 없습니다.</div>
              ) : (
                <div className="schedules-list">
                  {schedules.map(sched => {
                    const isEditing = editingId === sched.id;
                    if (isEditing) {
                      return (
                        <div key={sched.id} className="schedule-item-card editing">
                          <div className="sched-target">{getScheduleTargetName(sched)} (수정 중)</div>
                          
                          <div className="time-inputs-row">
                            <div className="form-group">
                              <label>화면 켜기 시간</label>
                              <div className="digital-time-picker">
                                <div className="time-input-col">
                                  <button type="button" className="time-arrow-btn" onClick={() => adjustTimeValue('onHour', 'up')}>
                                    <ChevronUp size={16} />
                                  </button>
                                  <input
                                    type="number"
                                    min="0"
                                    max="23"
                                    placeholder="시"
                                    value={editOnHour}
                                    onChange={(e) => setEditOnHour(e.target.value)}
                                    className="time-digit-input"
                                    inputMode="numeric"
                                    pattern="[0-9]*"
                                  />
                                  <button type="button" className="time-arrow-btn" onClick={() => adjustTimeValue('onHour', 'down')}>
                                    <ChevronDown size={16} />
                                  </button>
                                </div>
                                <span className="time-separator">:</span>
                                <div className="time-input-col">
                                  <button type="button" className="time-arrow-btn" onClick={() => adjustTimeValue('onMinute', 'up')}>
                                    <ChevronUp size={16} />
                                  </button>
                                  <input
                                    type="number"
                                    min="0"
                                    max="59"
                                    placeholder="분"
                                    value={editOnMinute}
                                    onChange={(e) => setEditOnMinute(e.target.value)}
                                    className="time-digit-input"
                                    inputMode="numeric"
                                    pattern="[0-9]*"
                                  />
                                  <button type="button" className="time-arrow-btn" onClick={() => adjustTimeValue('onMinute', 'down')}>
                                    <ChevronDown size={16} />
                                  </button>
                                </div>
                              </div>
                            </div>
                            <div className="form-group">
                              <label>화면 끄기 시간</label>
                              <div className="digital-time-picker">
                                <div className="time-input-col">
                                  <button type="button" className="time-arrow-btn" onClick={() => adjustTimeValue('offHour', 'up')}>
                                    <ChevronUp size={16} />
                                  </button>
                                  <input
                                    type="number"
                                    min="0"
                                    max="23"
                                    placeholder="시"
                                    value={editOffHour}
                                    onChange={(e) => setEditOffHour(e.target.value)}
                                    className="time-digit-input"
                                    inputMode="numeric"
                                    pattern="[0-9]*"
                                  />
                                  <button type="button" className="time-arrow-btn" onClick={() => adjustTimeValue('offHour', 'down')}>
                                    <ChevronDown size={16} />
                                  </button>
                                </div>
                                <span className="time-separator">:</span>
                                <div className="time-input-col">
                                  <button type="button" className="time-arrow-btn" onClick={() => adjustTimeValue('offMinute', 'up')}>
                                    <ChevronUp size={16} />
                                  </button>
                                  <input
                                    type="number"
                                    min="0"
                                    max="59"
                                    placeholder="분"
                                    value={editOffMinute}
                                    onChange={(e) => setEditOffMinute(e.target.value)}
                                    className="time-digit-input"
                                    inputMode="numeric"
                                    pattern="[0-9]*"
                                  />
                                  <button type="button" className="time-arrow-btn" onClick={() => adjustTimeValue('offMinute', 'down')}>
                                    <ChevronDown size={16} />
                                  </button>
                                </div>
                              </div>
                            </div>
                          </div>

                          <div className="form-group">
                            <label>요일 반복</label>
                            <div className="days-selector-buttons">
                              {['일', '월', '화', '수', '목', '금', '토'].map((dayName, idx) => {
                                const active = editDays.includes(idx);
                                return (
                                  <button
                                    key={idx}
                                    type="button"
                                    className={`day-btn ${active ? 'active' : ''}`}
                                    onClick={() => toggleEditDay(idx)}
                                  >
                                    {dayName}
                                  </button>
                                );
                              })}
                            </div>
                          </div>

                          <div className="edit-actions-row">
                            <button type="button" className="sched-cancel-btn" onClick={cancelEditing}>
                              취소
                            </button>
                            <button type="button" className="sched-save-btn" onClick={() => handleSaveEdit(sched.id)}>
                              저장
                            </button>
                          </div>
                        </div>
                      );
                    }

                    return (
                      <div key={sched.id} className="schedule-item-card">
                        <div className="schedule-info-part">
                          <div className="sched-target">{getScheduleTargetName(sched)}</div>
                          <div className="sched-times">
                            {sched.onTime && <span className="time-badge on">ON: {sched.onTime}</span>}
                            {sched.offTime && <span className="time-badge off">OFF: {sched.offTime}</span>}
                          </div>
                          <div className="sched-days">반복: {getDaysText(sched.days)}</div>
                        </div>

                        <div className="schedule-action-part">
                          {/* 활성화 토글 */}
                          <div className="toggle-switch-area">
                            <input
                              type="checkbox"
                              id={`toggle-${sched.id}`}
                              checked={sched.enabled}
                              onChange={() => handleToggleSchedule(sched)}
                              className="toggle-checkbox"
                            />
                            <label htmlFor={`toggle-${sched.id}`} className="toggle-label"></label>
                          </div>

                          {/* 수정 버튼 */}
                          <button className="sched-edit-btn" onClick={() => startEditing(sched)}>
                            <Edit size={16} />
                          </button>

                          {/* 삭제 버튼 */}
                          <button className="sched-delete-btn" onClick={() => handleDeleteSchedule(sched.id)}>
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        )}
      </main>

      {/* 하단 네비게이션 바 */}
      <nav className="mobile-nav">
        <button
          className={`nav-tab ${activeTab === 'dashboard' ? 'active' : ''}`}
          onClick={() => setActiveTab('dashboard')}
        >
          <LayoutGrid />
          <span>기기 관제</span>
        </button>
        
        <button
          className={`nav-tab ${activeTab === 'schedules' ? 'active' : ''}`}
          onClick={() => setActiveTab('schedules')}
        >
          <Calendar />
          <span>스케줄 설정</span>
        </button>
      </nav>
    </div>
  );
}

export default App;
