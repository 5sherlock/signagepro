import { useState, useEffect, useRef, useCallback } from 'react';
import { Activity, Monitor, Film, Settings, LayoutGrid, Plus } from 'lucide-react';
import { io } from 'socket.io-client';
import GroupManager from './components/GroupManager';
import { SOCKET_URL, apiFetch, getToken } from './config';
import MediaManager from './components/MediaManager';

// ─────────────────────────────────────────────
// 유틸
// ─────────────────────────────────────────────

function formatTime(seconds) {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

/**
 * PlaylistEngine(Android)과 동일 알고리즘. ms 단위 정밀도.
 * 서버 NTP epoch ms → 현재 슬라이드 인덱스 + 경과/남은 시간(초) 계산.
 * 모든 기기가 동일한 epoch를 받으면 항상 동일한 슬라이드를 반환.
 */
function computeNtpPosition(medias, epochMs) {
  const durationsMs = medias.map(m => (m.duration || 10) * 1000);
  const cycleMs = durationsMs.reduce((a, b) => a + b, 0);
  if (cycleMs <= 0) return { idx: 0, elapsed: 0, remaining: durationsMs[0] / 1000 || 10 };
  // 음수 방어
  const posInCycle = ((epochMs % cycleMs) + cycleMs) % cycleMs;
  let idx = 0, acc = 0;
  for (let i = 0; i < durationsMs.length; i++) {
    if (posInCycle < acc + durationsMs[i]) { idx = i; break; }
    acc += durationsMs[i];
  }
  const elapsedMs = posInCycle - acc;
  return {
    idx,
    elapsed: Math.floor(elapsedMs / 1000),
    remaining: Math.ceil((durationsMs[idx] - elapsedMs) / 1000),
  };
}

// ─────────────────────────────────────────────
// DevicePreview — 기기별 PiP 미리보기
// ─────────────────────────────────────────────
//
// ■ 핵심 설계: setTimeout 누적 오차 제거
//   이전 방식: 첫 슬롯만 NTP 기준, 이후 setTimeout(duration*1000) 체이닝
//              → JS setTimeout이 매번 수ms 밀려 기기 간 점점 벌어짐
//   현재 방식: 200ms setInterval로 매 tick마다 NTP epoch 재계산
//              → 오차가 쌓이지 않음. 101/102가 항상 동일 슬라이드 표시

function DevicePreview({ groupId, deviceId, onUpdate }) {
  const [playlist, setPlaylist] = useState([]);
  const [ntpOffset, setNtpOffset] = useState(0); // 서버시각 − 로컬시각 (ms)
  const [currentIndex, setCurrentIndex] = useState(0);
  const [prevIndex, setPrevIndex] = useState(-1);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const prevIdxRef = useRef(-1);
  const transTimerRef = useRef(null);

  // ── 플레이리스트 + NTP 오프셋 로드 ──────────────────────────────────────
  useEffect(() => {
    if (!groupId) return;

    const load = () => {
      const fetchStart = Date.now();
      Promise.all([
        fetch(`${SOCKET_URL}/api/groups/${groupId}/playlist`).then(r => r.json()),
        fetch(`${SOCKET_URL}/api/time`)
          .then(r => r.json())
          .catch(() => ({ epochMs: Date.now() })),
      ])
        .then(([data, { epochMs }]) => {
          const medias = (data?.medias || []).filter(
            m => !m.targetDeviceId || m.targetDeviceId === deviceId
          );
          setPlaylist(medias);
          // RTT 절반 보정 → 네트워크 왕복 지연 제거
          const rtt = Date.now() - fetchStart;
          setNtpOffset(epochMs - Date.now() + rtt / 2);
          prevIdxRef.current = -1;
        })
        .catch(err => console.error('미리보기 불러오기 실패:', err));
    };

    load();
    const socket = io(SOCKET_URL);
    socket.on('playlist_updated', ({ groupId: gid }) => {
      if (gid === groupId) load();
    });
    return () => socket.disconnect();
  }, [groupId, deviceId]);

  // ── 200ms 마다 NTP 기반 슬라이드 위치 재계산 ─────────────────────────────
  useEffect(() => {
    if (!playlist.length) return;

    const tick = () => {
      const nowMs = Date.now() + ntpOffset;
      const { idx, elapsed } = computeNtpPosition(playlist, nowMs);

      // 인덱스가 바뀌면 전환 애니메이션 트리거
      if (prevIdxRef.current !== -1 && prevIdxRef.current !== idx) {
        setPrevIndex(prevIdxRef.current);
        setIsTransitioning(true);
        if (transTimerRef.current) clearTimeout(transTimerRef.current);
        const tTime = playlist[prevIdxRef.current]?.transitionTime || 1000;
        transTimerRef.current = setTimeout(() => setIsTransitioning(false), tTime);
      }
      prevIdxRef.current = idx;
      setCurrentIndex(idx);
      setCurrentTime(elapsed);
    };

    tick(); // 즉시 1회 실행
    const interval = setInterval(tick, 200);
    return () => {
      clearInterval(interval);
      if (transTimerRef.current) clearTimeout(transTimerRef.current);
    };
  }, [playlist, ntpOffset]);

  const activeItem = playlist[currentIndex];
  const prevItem = prevIndex !== -1 ? playlist[prevIndex] : null;
  const transType = (prevItem ?? activeItem)?.transition?.toLowerCase() || 'fade';
  const transTime = (prevItem ?? activeItem)?.transitionTime || 1000;
  const duration = activeItem?.duration || 10;

  // ── 부모(App)에 현재 재생 상태 보고 ─────────────────────────────────────
  // onUpdate를 deps에 포함하면 App 재렌더 시 새 함수 레퍼런스 → 무한 루프
  // → 의도적으로 deps에서 제외 (eslint-disable)
  useEffect(() => {
    if (!onUpdate) return;
    onUpdate({
      filename: activeItem?.media?.filename || 'No Media',
      currentTime,
      duration,
      transType,
      transTime,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeItem, currentTime, transType, transTime, duration]);

  if (!playlist.length) {
    return (
      <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#444' }}>
        No Media
      </div>
    );
  }

  const slideDir = (prevItem ?? activeItem)?.slideDirection?.toLowerCase() || 'right';
  let inClass = '';
  let outClass = '';
  if (transType === 'slide') {
    inClass  = `preview-slide-in-${slideDir}`;
    outClass = `preview-slide-out-${slideDir}`;
  } else if (transType === 'fade') {
    inClass  = 'preview-fade';
    outClass = 'preview-fade-out';
  } else if (transType === 'dissolve') {
    inClass  = 'preview-dissolve';
    outClass = 'preview-dissolve-out';
  }

  const renderMedia = (item) => {
    if (!item) return null;
    const { media } = item;
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
      {/* 전환 중: 이전 슬라이드 — slide-out 애니메이션 적용 */}
      {isTransitioning && prevItem && (
        <div
          className={outClass}
          style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 1, '--trans-time': `${transTime}ms` }}
        >
          {renderMedia(prevItem)}
        </div>
      )}
      {/* 현재 슬라이드 — slide-in 애니메이션 적용 */}
      <div
        key={`${activeItem.id}-${currentIndex}`}
        className={inClass}
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

// ─────────────────────────────────────────────
// App
// ─────────────────────────────────────────────

// ─────────────────────────────────────────────
// SettingsTab — 서버 설정 + OTA 업데이트 배포
// ─────────────────────────────────────────────

const DAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'];
const ALL_DAYS = '0,1,2,3,4,5,6';

function ScreenScheduleSection({ onUnauth }) {
  const [schedules, setSchedules] = useState([]);
  const [saving, setSaving] = useState(false);
  const [pushing, setPushing] = useState(false);
  const [pushResult, setPushResult] = useState(null);
  const [draft, setDraft] = useState({ deviceId: '', onTime: '09:00', offTime: '22:00', days: '1,2,3,4,5', enabled: true });
  const [editId, setEditId] = useState(null);

  const check401 = (res) => { if (res.status === 401) { onUnauth?.(); throw new Error('401'); } return res; };

  const load = () =>
    apiFetch(`${SOCKET_URL}/api/schedules`)
      .then(check401).then(r => r.json()).then(setSchedules)
      .catch(e => { if (e.message !== '401') console.error(e); });

  useEffect(() => { load(); }, []);

  const toggleDay = (day) => {
    const cur = draft.days ? draft.days.split(',').map(Number) : [];
    const next = cur.includes(day) ? cur.filter(d => d !== day) : [...cur, day].sort((a, b) => a - b);
    setDraft(p => ({ ...p, days: next.join(',') }));
  };

  const save = async () => {
    setSaving(true);
    try {
      const body = { ...draft, id: editId || undefined };
      await apiFetch(`${SOCKET_URL}/api/schedules`, { method: 'POST', body: JSON.stringify(body) })
        .then(check401);
      setEditId(null);
      setDraft({ deviceId: '', onTime: '09:00', offTime: '22:00', days: '1,2,3,4,5', enabled: true });
      load();
    } catch (e) { if (e.message !== '401') alert('저장 실패'); }
    finally { setSaving(false); }
  };

  const remove = async (id) => {
    if (!window.confirm('스케줄을 삭제할까요?')) return;
    await apiFetch(`${SOCKET_URL}/api/schedules/${id}`, { method: 'DELETE' }).then(check401);
    load();
  };

  const startEdit = (s) => {
    setEditId(s.id);
    setDraft({ deviceId: s.deviceId || '', onTime: s.onTime || '', offTime: s.offTime || '', days: s.days, enabled: s.enabled });
  };

  const toggleEnabled = async (s) => {
    await apiFetch(`${SOCKET_URL}/api/schedules`, {
      method: 'POST',
      body: JSON.stringify({ id: s.id, deviceId: s.deviceId, onTime: s.onTime, offTime: s.offTime, days: s.days, enabled: !s.enabled }),
    }).then(check401);
    load();
  };

  const pushToDevices = async () => {
    setPushing(true);
    setPushResult(null);
    try {
      const data = await apiFetch(`${SOCKET_URL}/api/schedules/push`, { method: 'POST' })
        .then(check401).then(r => r.json());
      setPushResult(`✅ ${data.devices}개 기기에 전송 완료`);
      setTimeout(() => setPushResult(null), 3000);
    } catch (e) {
      if (e.message !== '401') setPushResult('❌ 전송 실패');
    } finally { setPushing(false); }
  };

  const activeDays = draft.days ? draft.days.split(',').map(Number) : [];
  const inputStyle = { padding: '6px 10px', borderRadius: '6px', border: '1px solid var(--border)', background: 'var(--bg-primary)', color: 'var(--text-primary)', fontSize: '0.9rem', colorScheme: 'dark' };

  return (
    <div className="glass-card" style={{ maxWidth: '600px', padding: '30px', marginTop: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <h2 style={{ margin: 0, fontSize: '1.2rem' }}>🕐 화면 스케줄</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          {pushResult && <span style={{ fontSize: '0.8rem', color: pushResult.startsWith('✅') ? '#10B981' : '#EF4444' }}>{pushResult}</span>}
          <button
            className="btn btn-primary"
            onClick={pushToDevices}
            disabled={pushing || schedules.length === 0}
            style={{ background: '#0EA5E9', fontSize: '0.85rem', padding: '6px 14px' }}>
            {pushing ? '전송 중…' : '📡 기기 전송'}
          </button>
        </div>
      </div>

      {/* 등록된 스케줄 목록 */}
      {schedules.length > 0 && (
        <div style={{ marginBottom: '20px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {schedules.map(s => (
            <div key={s.id} style={{ padding: '12px 14px', borderRadius: '8px', background: 'rgba(255,255,255,0.04)', border: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
              <span style={{ flex: 1, fontSize: '0.85rem', color: s.enabled ? 'var(--text-primary)' : '#666' }}>
                {s.deviceId || '전체 기기'} &nbsp;|&nbsp;
                {s.onTime && `☀️ ${s.onTime}`} {s.offTime && `🌙 ${s.offTime}`} &nbsp;|&nbsp;
                {s.days.split(',').map(Number).map(d => DAY_LABELS[d]).join(' ')}
              </span>
              <button onClick={() => toggleEnabled(s)} style={{ fontSize: '0.75rem', padding: '3px 8px', borderRadius: '4px', border: '1px solid var(--border)', background: 'transparent', color: s.enabled ? '#10B981' : '#666', cursor: 'pointer' }}>
                {s.enabled ? '활성' : '비활성'}
              </button>
              <button onClick={() => startEdit(s)} style={{ fontSize: '0.75rem', padding: '3px 8px', borderRadius: '4px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text-secondary)', cursor: 'pointer' }}>수정</button>
              <button onClick={() => remove(s.id)} style={{ fontSize: '0.75rem', padding: '3px 8px', borderRadius: '4px', border: '1px solid #EF4444', background: 'transparent', color: '#EF4444', cursor: 'pointer' }}>삭제</button>
            </div>
          ))}
        </div>
      )}

      {/* 스케줄 추가/수정 폼 */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>켜는 시간</label>
            <input type="time" value={draft.onTime} onChange={e => setDraft(p => ({ ...p, onTime: e.target.value }))} style={inputStyle} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>끄는 시간</label>
            <input type="time" value={draft.offTime} onChange={e => setDraft(p => ({ ...p, offTime: e.target.value }))} style={inputStyle} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>기기 ID (비우면 전체)</label>
            <input type="text" value={draft.deviceId} onChange={e => setDraft(p => ({ ...p, deviceId: e.target.value }))} placeholder="예: dev-101" style={{ ...inputStyle, width: '120px' }} />
          </div>
        </div>

        {/* 요일 선택 */}
        <div style={{ display: 'flex', gap: '6px' }}>
          {DAY_LABELS.map((label, day) => (
            <button key={day} onClick={() => toggleDay(day)}
              style={{ width: '36px', height: '36px', borderRadius: '50%', border: '1px solid var(--border)', background: activeDays.includes(day) ? '#3B82F6' : 'transparent', color: activeDays.includes(day) ? '#fff' : 'var(--text-secondary)', fontSize: '0.8rem', cursor: 'pointer', fontWeight: activeDays.includes(day) ? 700 : 400 }}>
              {label}
            </button>
          ))}
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button className="btn btn-primary" onClick={save} disabled={saving}>
            {saving ? '저장 중…' : editId ? '✏️ 수정 저장' : '+ 스케줄 추가'}
          </button>
          {editId && (
            <button className="btn" onClick={() => { setEditId(null); setDraft({ deviceId: '', onTime: '09:00', offTime: '22:00', days: '1,2,3,4,5', enabled: true }); }}
              style={{ background: 'transparent', border: '1px solid var(--border)', color: 'var(--text-secondary)' }}>
              취소
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function SettingsTab({ onUnauth }) {
  const [otaStatus, setOtaStatus] = useState(null);
  const [pushing, setPushing] = useState(false);
  const [adbRunning, setAdbRunning] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const adbControllerRef = useRef(null);
  const apkInputRef = useRef(null);

  const check401 = (res) => { if (res.status === 401) { onUnauth?.(); throw new Error('401'); } return res; };

  const refreshStatus = () =>
    apiFetch(`${SOCKET_URL}/api/update/status`)
      .then(check401)
      .then(r => r.json())
      .then(setOtaStatus)
      .catch(e => { if (e.message !== '401') setOtaStatus({ available: false }); });

  useEffect(() => { refreshStatus(); }, []);

  const handleApkUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.name.endsWith('.apk')) { alert('APK 파일만 업로드 가능합니다.'); return; }
    if (!window.confirm(`${file.name} (${(file.size / 1024 / 1024).toFixed(1)} MB) 을 업로드할까요?`)) return;

    setUploading(true);
    setUploadProgress(0);
    const form = new FormData();
    form.append('apk', file);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${SOCKET_URL}/api/update/apk`);
    const token = localStorage.getItem('SIGNAGE_TOKEN');
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    xhr.upload.onprogress = (ev) => {
      if (ev.lengthComputable) setUploadProgress(Math.round(ev.loaded / ev.total * 100));
    };
    xhr.onload = () => {
      setUploading(false);
      e.target.value = '';
      if (xhr.status === 200) {
        try {
          const data = JSON.parse(xhr.responseText);
          // POST 응답으로 직접 갱신 (브라우저 GET 캐시 문제 방지)
          setOtaStatus({ available: true, size: data.size, updatedAt: data.updatedAt });
        } catch { refreshStatus(); }
      } else { alert('업로드 실패: ' + xhr.responseText); }
    };
    xhr.onerror = () => { setUploading(false); alert('업로드 중 오류가 발생했습니다.'); };
    xhr.send(form);
  };

  const pushUpdate = (deviceId = '') => {
    if (!window.confirm(deviceId ? `${deviceId} 에 업데이트를 배포할까요?` : '전체 단말에 업데이트를 배포할까요?')) return;
    setPushing(true);
    apiFetch(`${SOCKET_URL}/api/update/push`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceId })
    })
      .then(check401)
      .then(r => r.json())
      .then(d => alert(d.success ? `배포 완료 → ${d.pushed}` : `실패: ${d.error}`))
      .catch(e => { if (e.message !== '401') alert('오류: ' + e.message); })
      .finally(() => setPushing(false));
  };

  const adbInstall = (deviceId = '') => {
    if (!window.confirm(deviceId ? `${deviceId}에 ADB 직접 설치할까요?` : '전체 단말에 ADB 직접 설치할까요?')) return;
    adbControllerRef.current = new AbortController();
    setPushing(true);
    setAdbRunning(true);
    apiFetch(`${SOCKET_URL}/api/update/adb-install`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceId }),
      signal: adbControllerRef.current.signal
    })
      .then(check401)
      .then(r => r.json())
      .then(d => {
        if (d.results && d.results.length > 0) {
          alert(d.results.map(r => `[${r.deviceId}] ${r.success ? '✅ 성공' : '❌ 실패'}\n${r.output || r.error || ''}`).join('\n\n'));
        } else {
          alert(d.error || '결과 없음');
        }
      })
      .catch(e => { if (e.name !== 'AbortError' && e.message !== '401') alert('오류: ' + e.message); })
      .finally(() => { setPushing(false); setAdbRunning(false); });
  };

  const cancelAdbInstall = () => {
    adbControllerRef.current?.abort();
    apiFetch(`${SOCKET_URL}/api/update/adb-cancel`, { method: 'POST' }).catch(() => {});
    setPushing(false);
    setAdbRunning(false);
  };

  return (
    <div className="content-area" style={{ overflowY: 'auto' }}>
      <header className="header">
        <h1 className="header-title">환경설정</h1>
      </header>

      {/* 서버 연결 */}
      <div className="glass-card" style={{ maxWidth: '600px', padding: '30px', marginTop: '20px' }}>
        <h2 style={{ marginBottom: '20px', fontSize: '1.2rem' }}>서버 연결 설정</h2>
        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)' }}>중앙 서버 주소 (API/Socket)</label>
          <input
            type="text"
            className="glass-input"
            placeholder="예: http://192.168.0.25:3000"
            defaultValue={localStorage.getItem('SIGNAGE_SERVER_URL') || SOCKET_URL}
            id="server-url-input"
            style={{ width: '100%', padding: '12px' }}
          />
          <p style={{ marginTop: '8px', fontSize: '0.8rem', color: '#94a3b8' }}>
            변경 후 앱이 다시 시작됩니다.
          </p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => {
            const val = document.getElementById('server-url-input').value;
            localStorage.setItem('SIGNAGE_SERVER_URL', val);
            alert('저장되었습니다. 앱을 재시작합니다.');
            window.location.reload();
          }}
        >
          설정 저장 및 적용
        </button>
      </div>

      {/* OTA 업데이트 */}
      <div className="glass-card" style={{ maxWidth: '600px', padding: '30px', marginTop: '20px' }}>
        <h2 style={{ marginBottom: '8px', fontSize: '1.2rem' }}>📦 단말 APK 원격 배포 (OTA)</h2>
        <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '16px' }}>
          APK를 업로드한 후 배포 버튼을 누르세요. 단말이 자동으로 다운로드 후 설치합니다.
        </p>

        {/* APK 업로드 */}
        <div style={{ marginBottom: '16px' }}>
          <input ref={apkInputRef} type="file" accept=".apk" style={{ display: 'none' }} onChange={handleApkUpload} />
          <button
            className="btn btn-primary"
            style={{ background: '#8B5CF6' }}
            disabled={uploading}
            onClick={() => apkInputRef.current?.click()}
          >
            {uploading ? `업로드 중… ${uploadProgress}%` : '📤 APK 파일 업로드'}
          </button>
          {uploading && (
            <div style={{ marginTop: '8px', height: '6px', borderRadius: '3px', background: 'rgba(255,255,255,0.1)', overflow: 'hidden' }}>
              <div style={{ height: '100%', width: `${uploadProgress}%`, background: '#8B5CF6', transition: 'width 0.2s' }} />
            </div>
          )}
        </div>

        {/* APK 상태 */}
        <div style={{ marginBottom: '20px', padding: '12px', borderRadius: '8px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
          {otaStatus === null ? (
            <span style={{ color: '#94a3b8', fontSize: '0.85rem' }}>확인 중…</span>
          ) : otaStatus.available ? (
            <>
              <span style={{ color: '#10B981', fontSize: '0.85rem' }}>
                ✅ APK 준비됨 — {(otaStatus.size / 1024 / 1024).toFixed(1)} MB
                &nbsp;({new Date(otaStatus.updatedAt).toLocaleString('ko-KR')})
              </span>
              <button
                style={{ flexShrink: 0, padding: '4px 10px', fontSize: '0.78rem', background: '#EF4444', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer' }}
                onClick={() => {
                  if (!window.confirm('배포용 APK를 삭제할까요?')) return;
                  apiFetch(`${SOCKET_URL}/api/update/apk`, { method: 'DELETE' })
                    .then(r => r.json())
                    .then(d => { if (d.ok) setOtaStatus({ available: false }); else alert(d.error); })
                    .catch(e => alert('오류: ' + e.message));
                }}
              >
                × 배포 취소
              </button>
            </>
          ) : (
            <span style={{ color: '#EF4444', fontSize: '0.85rem' }}>
              ❌ APK 없음 — server/update/app.apk 를 배치하세요
            </span>
          )}
        </div>

        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          <button
            className="btn btn-primary"
            disabled={pushing || !otaStatus?.available}
            onClick={() => pushUpdate('')}
          >
            {pushing && !adbRunning ? '배포 중…' : '🚀 OTA 푸시 (확인 필요할 수 있음)'}
          </button>
          <button
            className="btn btn-primary"
            style={{ background: '#10B981' }}
            disabled={pushing || !otaStatus?.available}
            onClick={() => adbInstall('')}
          >
            {adbRunning ? '설치 중…' : '⚡ ADB 직접 설치 (확인 없음)'}
          </button>
          {adbRunning && (
            <button
              className="btn btn-primary"
              style={{ background: '#EF4444' }}
              onClick={cancelAdbInstall}
            >
              ✕ 설치 취소
            </button>
          )}
        </div>
        <p style={{ marginTop: '10px', fontSize: '0.78rem', color: '#94a3b8' }}>
          ⚡ ADB 직접 설치: 단말에서 ADB TCP 모드(5555)가 활성화된 경우 확인창 없이 강제 설치.<br/>
          USB에 signagepro.json <code>{"{ \"type\": \"enable_adb_tcp\" }"}</code> 액션으로 1회 활성화 가능.
        </p>
      </div>

      <ScreenScheduleSection onUnauth={onUnauth} />
    </div>
  );
}

// ─────────────────────────────────────────────
// RemoteControlModal — 기기 원격 제어
// ─────────────────────────────────────────────

function RemoteControlModal({ device, onClose }) {
  const [screenshot, setScreenshot] = useState(null);
  const [screenshotLoading, setScreenshotLoading] = useState(false);
  const [volume, setVolume] = useState(8);
  const [busy, setBusy] = useState(false);
  const [log, setLog] = useState('');
  const prevUrlRef = useRef(null);

  const addLog = (msg) => setLog(msg);

  const takeScreenshot = async () => {
    setScreenshotLoading(true);
    addLog('스크린샷 캡처 중…');
    try {
      const r = await apiFetch(`${SOCKET_URL}/api/devices/${device.id}/screenshot`, { method: 'POST' });
      if (!r.ok) { addLog('스크린샷 실패'); return; }
      const blob = await r.blob();
      if (prevUrlRef.current) URL.revokeObjectURL(prevUrlRef.current);
      const url = URL.createObjectURL(blob);
      prevUrlRef.current = url;
      setScreenshot(url);
      addLog('스크린샷 완료');
    } catch { addLog('ADB 연결 실패'); }
    finally { setScreenshotLoading(false); }
  };

  const sendCommand = async (endpoint, body = {}, label = '') => {
    setBusy(true);
    addLog(`${label} 전송 중…`);
    try {
      const r = await apiFetch(`${SOCKET_URL}/api/devices/${device.id}/${endpoint}`, {
        method: 'POST',
        body: JSON.stringify(body),
      });
      const d = await r.json();
      addLog(d.ok ? `${label} 완료` : `오류: ${d.error}`);
    } catch { addLog('명령 전송 실패'); }
    finally { setBusy(false); }
  };

  useEffect(() => {
    takeScreenshot();
    return () => { if (prevUrlRef.current) URL.revokeObjectURL(prevUrlRef.current); };
  }, []);

  const btnStyle = (color = 'rgba(255,255,255,0.05)') => ({
    padding: '10px 6px', borderRadius: '8px',
    background: color, border: '1px solid var(--border)',
    color: 'var(--text-primary)', cursor: busy ? 'not-allowed' : 'pointer',
    fontSize: '0.82rem', opacity: busy ? 0.6 : 1,
  });

  return (
    <div
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
      style={{ position:'fixed', inset:0, background:'rgba(0,0,0,0.75)', zIndex:1000, display:'flex', alignItems:'center', justifyContent:'center' }}
    >
      <div style={{ background:'var(--bg-secondary)', border:'1px solid var(--border)', borderRadius:'16px', width:'580px', maxWidth:'92vw', padding:'24px', display:'flex', flexDirection:'column', gap:'14px' }}>

        {/* 헤더 */}
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
          <h2 style={{ margin:0, fontSize:'1.05rem', fontWeight:700 }}>🎮 원격 제어 — {device.name}</h2>
          <button onClick={onClose} style={{ background:'transparent', border:'none', color:'var(--text-secondary)', cursor:'pointer', fontSize:'1.3rem', lineHeight:1 }}>✕</button>
        </div>

        {/* 스크린샷 */}
        <div style={{ background:'#000', borderRadius:'10px', overflow:'hidden', aspectRatio:'16/9', position:'relative', display:'flex', alignItems:'center', justifyContent:'center' }}>
          {screenshot
            ? <img src={screenshot} alt="screenshot" style={{ width:'100%', height:'100%', objectFit:'contain' }} />
            : <span style={{ color:'#555', fontSize:'0.85rem' }}>{screenshotLoading ? '캡처 중…' : '스크린샷 없음'}</span>
          }
          {screenshotLoading && (
            <div style={{ position:'absolute', inset:0, background:'rgba(0,0,0,0.55)', display:'flex', alignItems:'center', justifyContent:'center', color:'#fff', fontSize:'0.85rem' }}>
              📸 캡처 중…
            </div>
          )}
        </div>

        {/* 스크린샷 버튼 */}
        <button onClick={takeScreenshot} disabled={screenshotLoading}
          style={{ padding:'8px', borderRadius:'8px', background:'#3B82F6', color:'#fff', border:'none', cursor:'pointer', fontSize:'0.85rem', opacity: screenshotLoading ? 0.6 : 1 }}>
          📸 스크린샷 새로고침
        </button>

        {/* 제어 버튼 */}
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:'8px' }}>
          <button disabled={busy} style={btnStyle()} onClick={() => sendCommand('restart-app', {}, '앱 재시작')}>🔄 앱 재시작</button>
          <button disabled={busy} style={btnStyle()} onClick={() => sendCommand('screen', { on: true }, '화면 켜기')}>☀️ 화면 켜기</button>
          <button disabled={busy} style={btnStyle()} onClick={() => sendCommand('screen', { on: false }, '화면 끄기')}>🌙 화면 끄기</button>
        </div>

        {/* 볼륨 */}
        <div style={{ display:'flex', alignItems:'center', gap:'10px', background:'rgba(255,255,255,0.04)', padding:'10px 14px', borderRadius:'8px', border:'1px solid var(--border)' }}>
          <span style={{ fontSize:'0.85rem', color:'var(--text-secondary)', whiteSpace:'nowrap' }}>🔊 볼륨</span>
          <input type="range" min={0} max={15} value={volume}
            onChange={e => setVolume(+e.target.value)}
            style={{ flex:1, accentColor:'#3B82F6' }} />
          <span style={{ fontSize:'0.85rem', fontFamily:'monospace', width:'20px', textAlign:'center', color:'var(--text-primary)' }}>{volume}</span>
          <button disabled={busy} onClick={() => sendCommand('volume', { level: volume }, `볼륨 ${volume} 적용`)}
            style={{ padding:'5px 12px', borderRadius:'6px', background:'#3B82F6', color:'#fff', border:'none', cursor:'pointer', fontSize:'0.8rem', opacity: busy ? 0.6 : 1 }}>
            적용
          </button>
        </div>

        {/* 상태 로그 */}
        {log && <p style={{ margin:0, fontSize:'0.78rem', color:'#94a3b8', fontFamily:'monospace' }}>▶ {log}</p>}
      </div>
    </div>
  );
}

function LoginScreen({ onLogin }) {
  const [mode, setMode] = useState('login'); // 'login' | 'change'
  const [pw, setPw] = useState('');
  const [currentPw, setCurrentPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [confirmPw, setConfirmPw] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const submitLogin = async (e) => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      const r = await fetch(`${SOCKET_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: pw }),
      });
      const d = await r.json();
      if (!r.ok) { setError(d.error || '로그인 실패'); return; }
      localStorage.setItem('SIGNAGE_TOKEN', d.token);
      onLogin();
    } catch { setError('서버에 연결할 수 없습니다.'); }
    finally { setLoading(false); }
  };

  const submitChange = async (e) => {
    e.preventDefault();
    setError(''); setSuccess('');
    if (newPw !== confirmPw) { setError('새 비밀번호가 일치하지 않습니다.'); return; }
    if (newPw.length < 4) { setError('새 비밀번호는 4자 이상이어야 합니다.'); return; }
    setLoading(true);
    try {
      const r = await fetch(`${SOCKET_URL}/api/auth/change-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ current: currentPw, newPassword: newPw }),
      });
      const d = await r.json();
      if (!r.ok) { setError(d.error || '변경 실패'); return; }
      setSuccess('비밀번호가 변경되었습니다. 새 비밀번호로 로그인하세요.');
      setCurrentPw(''); setNewPw(''); setConfirmPw('');
      setTimeout(() => { setMode('login'); setSuccess(''); }, 2000);
    } catch { setError('서버에 연결할 수 없습니다.'); }
    finally { setLoading(false); }
  };

  const inputStyle = { padding:'10px 12px', borderRadius:'8px', border:'1px solid var(--border)', background:'var(--bg-primary)', color:'var(--text-primary)', fontSize:'1rem', outline:'none' };

  return (
    <div style={{ display:'flex', alignItems:'center', justifyContent:'center', height:'100vh', background:'var(--bg-primary)' }}>
      <form onSubmit={mode === 'login' ? submitLogin : submitChange}
        style={{ background:'var(--bg-secondary)', border:'1px solid var(--border)', borderRadius:'12px', padding:'40px', width:'320px', display:'flex', flexDirection:'column', gap:'16px' }}>
        <div style={{ display:'flex', alignItems:'center', gap:'10px', marginBottom:'4px' }}>
          <Activity size={28} color="#3B82F6" />
          <span style={{ fontSize:'1.3rem', fontWeight:700, color:'var(--text-primary)' }}>SignagePro</span>
        </div>

        {mode === 'login' ? (
          <>
            <p style={{ color:'var(--text-secondary)', fontSize:'0.85rem', margin:0 }}>관리자 비밀번호를 입력하세요.</p>
            <input type="password" value={pw} onChange={e => setPw(e.target.value)}
              placeholder="비밀번호" autoFocus style={inputStyle} />
            {error && <p style={{ color:'#EF4444', fontSize:'0.8rem', margin:0 }}>{error}</p>}
            <button type="submit" disabled={loading}
              style={{ padding:'10px', borderRadius:'8px', background:'#3B82F6', color:'#fff', border:'none', fontWeight:600, fontSize:'1rem', cursor:'pointer' }}>
              {loading ? '확인 중…' : '로그인'}
            </button>
            <button type="button" onClick={() => { setMode('change'); setError(''); }}
              style={{ padding:'6px', background:'transparent', border:'none', color:'var(--text-secondary)', fontSize:'0.8rem', cursor:'pointer', textDecoration:'underline' }}>
              비밀번호 변경
            </button>
          </>
        ) : (
          <>
            <p style={{ color:'var(--text-secondary)', fontSize:'0.85rem', margin:0 }}>비밀번호를 변경합니다.</p>
            <input type="password" value={currentPw} onChange={e => setCurrentPw(e.target.value)}
              placeholder="현재 비밀번호" autoFocus style={inputStyle} />
            <input type="password" value={newPw} onChange={e => setNewPw(e.target.value)}
              placeholder="새 비밀번호" style={inputStyle} />
            <input type="password" value={confirmPw} onChange={e => setConfirmPw(e.target.value)}
              placeholder="새 비밀번호 확인" style={inputStyle} />
            {error && <p style={{ color:'#EF4444', fontSize:'0.8rem', margin:0 }}>{error}</p>}
            {success && <p style={{ color:'#10B981', fontSize:'0.8rem', margin:0 }}>{success}</p>}
            <button type="submit" disabled={loading}
              style={{ padding:'10px', borderRadius:'8px', background:'#10B981', color:'#fff', border:'none', fontWeight:600, fontSize:'1rem', cursor:'pointer' }}>
              {loading ? '변경 중…' : '비밀번호 변경'}
            </button>
            <button type="button" onClick={() => { setMode('login'); setError(''); }}
              style={{ padding:'6px', background:'transparent', border:'none', color:'var(--text-secondary)', fontSize:'0.8rem', cursor:'pointer', textDecoration:'underline' }}>
              로그인으로 돌아가기
            </button>
          </>
        )}
      </form>
    </div>
  );
}

function App() {
  const [authed, setAuthed] = useState(!!getToken());
  const [activeTab, setActiveTab] = useState('dashboard');
  const [devices, setDevices] = useState([]);
  const [groups, setGroups] = useState([]);
  const [stores, setStores] = useState([]);
  const [selectedStoreId, setSelectedStoreId] = useState('');
  const [gridLayout, setGridLayout] = useState('auto');
  const [deviceMeta, setDeviceMeta] = useState({});
  const [customGrid, setCustomGrid] = useState({ rows: 2, cols: 3 });
  const [apkAvailable, setApkAvailable] = useState(false);
  const [adbRunning, setAdbRunning] = useState(false);
  const [serverOnline, setServerOnline] = useState(null); // null=확인중, true=연결, false=끊김

  const onUnauth = useCallback(() => {
    localStorage.removeItem('SIGNAGE_TOKEN');
    setAuthed(false);
  }, []);

  const fetchDevices = useCallback(() => {
    apiFetch(`${SOCKET_URL}/api/devices`, { cache: 'no-store' })
      .then(res => { if (res.status === 401) { onUnauth(); return null; } return res.json(); })
      .then(data => {
        if (!data) return;
        const mappedDevices = data.map(d => ({
          ...d,
          groupName: d.group?.name || '미배정 기기',
          storeName: d.store?.name || '미배정 사업장'
        }));
        setDevices(mappedDevices);
      })
      .catch(err => console.error('기기 목록 불러오기 실패:', err));
  }, [onUnauth]);

  const fetchGroups = useCallback(() => {
    apiFetch(`${SOCKET_URL}/api/groups`)
      .then(res => { if (res.status === 401) { onUnauth(); return null; } return res.json(); })
      .then(data => { if (data) setGroups(data); })
      .catch(err => console.error('그룹 목록 불러오기 실패:', err));
  }, [onUnauth]);

  const fetchStores = useCallback(() => {
    apiFetch(`${SOCKET_URL}/api/stores`)
      .then(res => { if (res.status === 401) { onUnauth(); return null; } return res.json(); })
      .then(data => {
        if (!data) return;
        setStores(data);
        setSelectedStoreId(prev => prev || (data.length > 0 ? data[0].id : prev));
      })
      .catch(err => console.error('사업장 목록 불러오기 실패:', err));
  }, [onUnauth]);

  useEffect(() => {
    const checkServer = () =>
      fetch(`${SOCKET_URL}/api/time`, { cache: 'no-store' })
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(() => setServerOnline(true))
        .catch(() => setServerOnline(false));

    const checkApk = () =>
      apiFetch(`${SOCKET_URL}/api/update/status`)
        .then(r => r.json())
        .then(d => setApkAvailable(!!d.available))
        .catch(() => setApkAvailable(false));

    checkServer();
    checkApk();
    const timer = setInterval(() => { checkServer(); checkApk(); }, 10000);
    return () => clearInterval(timer);
  }, []);

  const installApk = (deviceId) => {
    const label = deviceId || '전체 단말';
    if (!window.confirm(`${label}에 APK를 설치할까요?`)) return;
    setAdbRunning(true);
    apiFetch(`${SOCKET_URL}/api/update/adb-install`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceId }),
    })
      .then(r => r.json())
      .then(d => {
        const results = d.results || [];
        const ok = results.filter(r => r.success).map(r => r.deviceId).join(', ');
        const fail = results.filter(r => !r.success).map(r => r.deviceId).join(', ');
        alert(`설치 완료: ${ok || '없음'}\n실패: ${fail || '없음'}`);
      })
      .catch(() => alert('요청 실패'))
      .finally(() => setAdbRunning(false));
  };

  useEffect(() => {
    fetchDevices();
    fetchGroups();
    fetchStores();

    const socket = io(SOCKET_URL);

    socket.on('connect', () => setServerOnline(true));
    socket.on('disconnect', () => setServerOnline(false));
    socket.on('connect_error', () => setServerOnline(false));

    socket.on('device_status_update', (update) => {
      setDevices(prev => {
        const exists = prev.find(d => d.id === update.deviceId);
        if (exists) {
          return prev.map(d =>
            d.id === update.deviceId
              ? { ...d, status: update.status, cpuUsage: update.cpu || d.cpuUsage, memUsage: update.mem || d.memUsage, ip: update.ip || d.ip, appVersion: update.appVersion || d.appVersion }
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
    if (!d.groupId) return false;
    if (selectedStoreId === 'all') return true;
    if (selectedStoreId === 'unassigned') return !d.storeId;
    return d.storeId === selectedStoreId;
  });

  if (!authed) return <LoginScreen onLogin={() => setAuthed(true)} />;

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="logo-container">
          <Activity size={32} color="#3B82F6" />
          <span className="logo-text">SignagePro</span>
        </div>
        <div style={{ padding: '8px 16px', marginBottom: '4px' }}>
          <span style={{
            display: 'inline-flex', alignItems: 'center', gap: '6px',
            fontSize: '0.72rem', fontWeight: 600,
            color: serverOnline === null ? '#94a3b8' : serverOnline ? '#10B981' : '#EF4444',
          }}>
            <span style={{
              width: '7px', height: '7px', borderRadius: '50%',
              background: serverOnline === null ? '#94a3b8' : serverOnline ? '#10B981' : '#EF4444',
              boxShadow: serverOnline === true ? '0 0 6px #10B981' : serverOnline === false ? '0 0 6px #EF4444' : 'none',
            }} />
            {serverOnline === null ? '확인 중…' : serverOnline ? '서버 연결됨' : '서버 연결 끊김'}
          </span>
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
        <div style={{ padding: '16px', marginTop: 'auto' }}>
          <button
            onClick={() => {
              apiFetch(`${SOCKET_URL}/api/auth/logout`, { method: 'POST' }).catch(() => {});
              onUnauth();
            }}
            style={{ width: '100%', padding: '8px', borderRadius: '8px', background: 'transparent', border: '1px solid rgba(255,255,255,0.15)', color: 'var(--text-secondary)', fontSize: '0.8rem', cursor: 'pointer' }}
          >
            로그아웃
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        {activeTab === 'dashboard' && (() => {
          // 가장 많은 기기가 쓰는 버전을 기준 버전으로 삼음
          const versionCounts = {};
          devices.forEach(d => { if (d.appVersion) versionCounts[d.appVersion] = (versionCounts[d.appVersion] || 0) + 1; });
          const standardVersion = Object.keys(versionCounts).sort((a, b) => versionCounts[b] - versionCounts[a])[0] || null;
          return (
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
                            onUpdate={(meta) => setDeviceMeta(prev => ({ ...prev, [device.id]: meta }))}
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
                        {device.ip && (
                          <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', fontFamily: 'monospace', marginTop: '2px' }}>
                            {device.ip}
                          </div>
                        )}
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
                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '4px' }}>
                        <div className={`status-badge ${device.status}`}>
                          <span className="status-dot"></span>
                          {device.status === 'online' ? '온라인' : '오프라인'}
                        </div>
                        {device.appVersion ? (
                          <span style={{
                            fontSize: '0.65rem', fontFamily: 'monospace',
                            color: (standardVersion && device.appVersion !== standardVersion) ? '#f59e0b' : 'var(--text-secondary)',
                            fontWeight: (standardVersion && device.appVersion !== standardVersion) ? 700 : 400,
                          }}
                            title={(standardVersion && device.appVersion !== standardVersion) ? `기준 버전: v${standardVersion}` : ''}
                          >
                            {(standardVersion && device.appVersion !== standardVersion) ? '⚠ ' : ''}v{device.appVersion}
                          </span>
                        ) : (
                          <span style={{ fontSize: '0.65rem', color: '#f59e0b', fontWeight: 700 }} title="앱 버전 미확인">
                            ⚠ 버전 미확인
                          </span>
                        )}
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
                    <div style={{ marginTop: '8px', display: 'flex', justifyContent: 'flex-end', gap: '6px', flexWrap: 'wrap' }}>
                      {apkAvailable && (device.appVersion !== standardVersion) && (
                        <button
                          disabled={adbRunning}
                          style={{
                            fontSize: '0.7rem', padding: '3px 10px',
                            background: 'transparent', border: '1px solid #10B981',
                            borderRadius: '4px', color: '#10B981',
                            cursor: adbRunning ? 'not-allowed' : 'pointer', opacity: adbRunning ? 0.5 : 1
                          }}
                          onClick={() => installApk(device.id)}
                        >
                          {adbRunning ? '설치 중…' : '⚡ APK 업데이트'}
                        </button>
                      )}
                      <button
                        style={{
                          fontSize: '0.7rem', padding: '3px 10px',
                          background: 'transparent', border: '1px solid var(--border)',
                          borderRadius: '4px', color: 'var(--text-secondary)',
                          cursor: 'pointer'
                        }}
                        onClick={() => {
                          if (!window.confirm(`${device.name || device.id} 기기를 재부팅할까요?`)) return;
                          apiFetch(`${SOCKET_URL}/api/devices/${device.id}/reboot`, { method: 'POST' })
                            .then(r => r.json())
                            .then(r => alert(r.ok ? '재부팅 명령을 전송했습니다.' : `오류: ${r.error}`))
                            .catch(() => alert('요청 실패'));
                        }}
                      >
                        🔄 재부팅
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </>
          );
        })()}

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
          <SettingsTab onUnauth={onUnauth} />
        )}
      </main>
    </div>
  );
}

export default App;
