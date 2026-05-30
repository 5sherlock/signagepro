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

function DevicePreview({ groupId, deviceId, onUpdate, pcAudio = false, devVol = 8, liveSlide = null }) {
  const [playlist, setPlaylist] = useState([]);
  const [ntpOffset, setNtpOffset] = useState(0); // 서버시각 − 로컬시각 (ms)
  const [currentIndex, setCurrentIndex] = useState(0);
  const [prevIndex, setPrevIndex] = useState(-1);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const prevIdxRef = useRef(-1);
  const transTimerRef = useRef(null);
  const videoElRef = useRef(null);

  // ── PC 오디오 볼륨/음소거 실시간 반영 ────────────────────────────────────
  useEffect(() => {
    const el = videoElRef.current;
    if (!el) return;
    const muted = !pcAudio || devVol === 0;
    el.muted = muted;
    if (!muted) el.volume = Math.max(0, Math.min(1, devVol / 15));
  }, [pcAudio, devVol]);

  // ── 플레이리스트 + NTP 오프셋 로드 + 주기적 재동기 ──────────────────────────────────────
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
    // 10초마다 NTP 재동기 — 기기 heartbeat 주기(10초)와 동일 → 타임라인 항상 최신
    const resyncTimer = setInterval(load, 10_000);

    const socket = io(SOCKET_URL);
    socket.on('playlist_updated', ({ groupId: gid }) => {
      if (gid === groupId) load();
    });
    return () => {
      clearInterval(resyncTimer);
      socket.disconnect();
    };
  }, [groupId, deviceId]);

  // ── 200ms 마다 슬라이드 위치 재계산 ──────────────────────────────────────
  // NTP epoch 기반 순수 계산 — 기기의 PlaylistEngine과 동일한 알고리즘
  // liveSlide(하트비트) 오버라이드 제거: 최대 10초 지연 + stale 값에 의한 오싱크 원인
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
  // 애니메이션 CSS: 전환 중엔 나가는 슬라이드(prevItem)의 효과 사용
  const transType = (prevItem ?? activeItem)?.transition?.toLowerCase() || 'fade';
  const transTime = (prevItem ?? activeItem)?.transitionTime || 1000;
  const duration = activeItem?.duration || 10;
  // 라벨 표시: 항상 현재 슬라이드(activeItem)의 전환 효과 — "이 슬라이드가 나갈 때 쓸 효과"
  const labelTransType = activeItem?.transition?.toLowerCase() || 'fade';

  // 기기 실제 보고값으로 미리보기 중인지 여부
  const isLiveSync = liveSlide && liveSlide.index > 0 && liveSlide.index <= playlist.length;

  // ── 부모(App)에 현재 재생 상태 보고 ─────────────────────────────────────
  // onUpdate를 deps에 포함하면 App 재렌더 시 새 함수 레퍼런스 → 무한 루프
  // → 의도적으로 deps에서 제외 (eslint-disable)
  useEffect(() => {
    if (!onUpdate) return;
    onUpdate({
      filename: activeItem?.media?.filename || 'No Media',
      currentTime,
      duration,
      transType: labelTransType,
      transTime,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeItem, currentTime, labelTransType, transTime, duration]);

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
        <video
          key={media.path}
          ref={videoElRef}
          src={`${SOCKET_URL}${media.path}`}
          autoPlay
          muted={!pcAudio || devVol === 0}
          loop
          playsInline
          onLoadedMetadata={e => {
            e.target.muted = !pcAudio || devVol === 0;
            e.target.volume = Math.max(0, Math.min(1, devVol / 15));
          }}
          style={{ width: '100%', height: '100%', objectFit: 'cover', background: '#000' }}
        />
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
      {/* LIVE / NTP 표시 배지 */}
      <div style={{
        position: 'absolute', top: 4, right: 4, zIndex: 10,
        fontSize: '0.48rem', fontWeight: 700, letterSpacing: '0.04em',
        padding: '2px 5px', borderRadius: 3,
        background: isLiveSync ? 'rgba(16,185,129,0.85)' : 'rgba(99,102,241,0.75)',
        color: '#fff', pointerEvents: 'none',
      }}>
        {isLiveSync ? '● LIVE' : '◎ NTP'}
      </div>
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
// GradientLevelMeter — Option C 그라디언트 L/R 레벨미터
// ─────────────────────────────────────────────
/** GradientLevelMeter — 실제 Visualizer 측정값(0~100) 기반, 우측 컴팩트 */
function GradientLevelMeter({ vu }) {
  const pct = Math.max(0, Math.min(100, vu ?? 0));
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      {['L', 'R'].map(ch => (
        <div key={ch} style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
          <span style={{ fontSize: '0.45rem', color: '#334155', width: 6, flexShrink: 0, textAlign: 'right' }}>{ch}</span>
          <div style={{
            width: 72, height: 7, background: '#0c111e', borderRadius: 2,
            overflow: 'hidden', position: 'relative',
            boxShadow: 'inset 0 1px 2px rgba(0,0,0,0.6)'
          }}>
            <div style={{
              height: '100%', width: `${pct}%`, borderRadius: 2,
              background: 'linear-gradient(to right, #22c55e 0%, #84cc16 45%, #eab308 65%, #f97316 80%, #ef4444 100%)',
              transition: 'width 0.1s', position: 'relative'
            }}>
              {pct > 2 && (
                <div style={{ position: 'absolute', right: 0, top: 0, width: 2, height: '100%', background: '#fff', opacity: 0.8 }} />
              )}
            </div>
          </div>
        </div>
      ))}
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

/** 시:분 스텝퍼 — ▲▼ + 직접 입력, 항상 두 자리 표시 */
function TimeStepper({ value, onChange }) {
  const parts = (value || '00:00').split(':');
  const h = parseInt(parts[0]) || 0;
  const m = parseInt(parts[1]) || 0;

  const fmt = (n, max) => ((n % max + max) % max).toString().padStart(2, '0');
  const stepH = d => onChange(`${fmt(h + d, 24)}:${fmt(m, 60)}`);
  const stepM = d => onChange(`${fmt(h, 24)}:${fmt(m + d, 60)}`);

  const btnSt = { width: '40px', height: '22px', borderRadius: '5px', border: '1px solid var(--border)', background: 'rgba(255,255,255,0.06)', color: 'var(--text-secondary)', fontSize: '0.65rem', cursor: 'pointer', lineHeight: 1 };
  const inpSt = { width: '40px', height: '38px', textAlign: 'center', fontSize: '1.15rem', fontWeight: 700, fontFamily: 'monospace', background: 'rgba(255,255,255,0.06)', border: '1px solid var(--border)', borderRadius: '6px', color: 'var(--text-primary)', outline: 'none', cursor: 'text' };

  const col = (display, onUp, onDown, onBlurCommit) => (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '2px' }}>
      <button onClick={onUp} style={btnSt}>▲</button>
      <input
        type="text" inputMode="numeric" maxLength={2}
        defaultValue={display}
        key={display}                    /* display 바뀌면 재마운트 → 값 동기화 */
        style={inpSt}
        onFocus={e => { e.target.select(); e.target.style.borderColor = '#3b82f6'; e.target.style.background = 'rgba(59,130,246,0.12)'; }}
        onBlur={e => {
          e.target.style.borderColor = 'var(--border)';
          e.target.style.background = 'rgba(255,255,255,0.06)';
          onBlurCommit(e.target.value);
        }}
      />
      <button onClick={onDown} style={btnSt}>▼</button>
    </div>
  );

  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', background: 'rgba(255,255,255,0.04)', border: '1px solid var(--border)', borderRadius: '10px', padding: '6px 10px' }}>
      {col(fmt(h, 24), () => stepH(1), () => stepH(-1), v => { let n = parseInt(v,10); if(isNaN(n))n=0; onChange(`${fmt(n,24)}:${fmt(m,60)}`); })}
      <span style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-secondary)', userSelect: 'none' }}>:</span>
      {col(fmt(m, 60), () => stepM(1), () => stepM(-1), v => { let n = parseInt(v,10); if(isNaN(n))n=0; onChange(`${fmt(h,24)}:${fmt(n,60)}`); })}
    </div>
  );
}

function ScreenScheduleSection({ onUnauth, deviceOrder = {} }) {
  const [schedules, setSchedules] = useState([]);
  const [devices, setDevices] = useState([]);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null); // { msg, ok }
  const [draft, setDraft] = useState({ deviceId: '', onTime: '09:00', offTime: '22:00', days: '1,2,3,4,5', enabled: true });
  const [editId, setEditId] = useState(null);
  const [showForm, setShowForm] = useState(false); // 추가 폼 열림/닫힘
  // 기기 현재 시각 — 온라인 기기의 deviceTime 기준으로 1초 tick
  const [deviceNow, setDeviceNow] = useState(null);

  const showToast = (msg, ok = true) => {
    setToast({ msg, ok });
    setTimeout(() => setToast(null), 3000);
  };

  const check401 = (res) => { if (res.status === 401) { onUnauth?.(); throw new Error('401'); } return res; };

  const load = () =>
    apiFetch(`${SOCKET_URL}/api/schedules`)
      .then(check401).then(r => r.json()).then(setSchedules)
      .catch(e => { if (e.message !== '401') console.error(e); });

  useEffect(() => { load(); }, []);

  useEffect(() => {
    let baseDeviceTime = null;
    let baseLocal = null;

    const fetchDevices = () =>
      apiFetch(`${SOCKET_URL}/api/devices`)
        .then(r => r.json())
        .then(devList => {
          setDevices(devList);
          const onlineDev = devList.find(d => d.status === 'online' && d.deviceTime);
          if (onlineDev) { baseDeviceTime = onlineDev.deviceTime; baseLocal = Date.now(); }
        })
        .catch(() => {});

    fetchDevices();
    const pollId = setInterval(fetchDevices, 10000); // 10초마다 갱신
    const tickId = setInterval(() => {
      if (baseDeviceTime !== null) setDeviceNow(baseDeviceTime + (Date.now() - baseLocal));
    }, 1000);
    return () => { clearInterval(pollId); clearInterval(tickId); };
  }, []);

  const toggleDay = (day) => {
    const cur = draft.days ? draft.days.split(',').map(Number) : [];
    const next = cur.includes(day) ? cur.filter(d => d !== day) : [...cur, day].sort((a, b) => a - b);
    setDraft(p => ({ ...p, days: next.join(',') }));
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


  const activeDays = draft.days ? draft.days.split(',').map(Number) : [];
  const isFormOpen = showForm || !!editId;
  const inp = { padding: '7px 11px', borderRadius: '8px', border: '1px solid var(--border)', background: 'rgba(255,255,255,0.06)', color: 'var(--text-primary)', fontSize: '0.95rem', colorScheme: 'dark', outline: 'none', width: '100%' };

  const closeForm = () => { setShowForm(false); setEditId(null); setDraft({ deviceId: '', onTime: '09:00', offTime: '22:00', days: '1,2,3,4,5', enabled: true }); };
  const handleSave = async () => {
    // 중복 체크 — 같은 기기·시간·요일 조합이 이미 존재하는지 확인
    const dup = schedules.find(s =>
      s.id !== editId &&
      (s.deviceId || '') === (draft.deviceId || '') &&
      s.onTime  === draft.onTime &&
      s.offTime === draft.offTime &&
      s.days    === draft.days
    );
    if (dup) { showToast('❌ 동일한 스케줄이 이미 있습니다', false); return; }

    setSaving(true);
    try {
      await apiFetch(`${SOCKET_URL}/api/schedules`, { method: 'POST', body: JSON.stringify({ ...draft, id: editId || undefined }) }).then(check401);
      closeForm();
      load();
      showToast('✅ 저장 완료 — 기기에 자동 전송됨');
    } catch (e) { if (e.message !== '401') showToast('❌ 저장 실패', false); }
    finally { setSaving(false); }
  };

  return (
    <div className="glass-card" style={{ maxWidth: '600px', padding: '24px 28px', marginTop: '20px', height: 'auto', alignSelf: 'stretch' }}>

      {/* ── 헤더 ── */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px' }}>
        <h2 style={{ margin: 0, fontSize: '1.15rem', flex: 1 }}>🕐 화면 스케줄</h2>
        {deviceNow && (
          <span style={{ fontSize: '0.72rem', color: '#64748b', fontFamily: 'monospace', background: 'rgba(255,255,255,0.04)', padding: '3px 8px', borderRadius: '6px', border: '1px solid var(--border)', flexShrink: 0 }}
            title="기기 현재 시각 (KST) — 스케줄은 이 시각 기준으로 실행됩니다">
            🕐 기기 {new Date(deviceNow).toLocaleTimeString('ko-KR', { timeZone: 'Asia/Seoul', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })}
          </span>
        )}
        {!isFormOpen && (
          <button onClick={() => setShowForm(true)}
            style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '6px 14px', borderRadius: '8px', border: 'none', background: '#3b82f6', color: '#fff', fontSize: '0.82rem', fontWeight: 700, cursor: 'pointer', flexShrink: 0 }}>
            + 새 스케줄
          </button>
        )}
      </div>

      {/* ── 토스트 ── */}
      {toast && (
        <div style={{ marginBottom: '16px', padding: '9px 14px', borderRadius: '8px', background: toast.ok ? 'rgba(16,185,129,0.12)' : 'rgba(239,68,68,0.12)', border: `1px solid ${toast.ok ? '#10B981' : '#EF4444'}`, fontSize: '0.85rem', fontWeight: 600, color: toast.ok ? '#10B981' : '#EF4444' }}>
          {toast.msg}
        </div>
      )}

      {/* ── 스케줄 카드 목록 ── */}
      {schedules.length === 0 && !isFormOpen && (
        <div style={{ textAlign: 'center', padding: '32px 0', color: '#475569', fontSize: '0.88rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '8px' }}>📅</div>
          등록된 스케줄이 없습니다.<br />
          <span style={{ color: '#3b82f6' }}>+ 새 스케줄</span> 버튼으로 추가하세요.
        </div>
      )}

      {schedules.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: isFormOpen ? '20px' : '0' }}>
          {schedules.map(s => {
            const isEditing = editId === s.id;
            // 수정 중인 카드는 draft 값을 실시간 반영
            const liveOnTime  = isEditing ? draft.onTime  : s.onTime;
            const liveOffTime = isEditing ? draft.offTime : s.offTime;
            const liveDeviceId = isEditing ? draft.deviceId : s.deviceId;
            const liveDays = (isEditing ? draft.days : s.days)
              .split(',').filter(Boolean).map(Number);
            return (
              <div key={s.id} style={{ borderRadius: '10px', border: `1px solid ${isEditing ? '#f59e0b' : 'var(--border)'}`, background: isEditing ? 'rgba(245,158,11,0.06)' : s.enabled ? 'rgba(255,255,255,0.04)' : 'rgba(255,255,255,0.02)', overflow: 'hidden' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 12px', minWidth: 0 }}>

                  {/* 활성 토글 */}
                  <div onClick={() => toggleEnabled(s)} title={s.enabled ? '클릭하여 비활성화' : '클릭하여 활성화'}
                    style={{ width: '36px', height: '20px', borderRadius: '10px', background: s.enabled ? '#10b981' : '#334155', cursor: 'pointer', position: 'relative', flexShrink: 0, transition: 'background 0.2s' }}>
                    <div style={{ position: 'absolute', top: '3px', left: s.enabled ? '19px' : '3px', width: '14px', height: '14px', borderRadius: '50%', background: '#fff', transition: 'left 0.2s' }} />
                  </div>

                  {/* 시간 표시 */}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '5px', marginBottom: '5px', flexWrap: 'wrap', minWidth: 0 }}>
                      {liveOnTime  && <span style={{ fontSize: '0.95rem', fontWeight: 700, color: s.enabled ? '#fbbf24' : '#475569', flexShrink: 0 }}>☀️ {liveOnTime}</span>}
                      {liveOnTime && liveOffTime && <span style={{ color: '#475569', fontSize: '0.75rem', flexShrink: 0 }}>→</span>}
                      {liveOffTime && <span style={{ fontSize: '0.95rem', fontWeight: 700, color: s.enabled ? '#818cf8' : '#475569', flexShrink: 0 }}>🌙 {liveOffTime}</span>}
                      {liveDeviceId
                        ? <span style={{ fontSize: '0.68rem', color: '#60a5fa', background: 'rgba(96,165,250,0.1)', border: '1px solid rgba(96,165,250,0.2)', padding: '1px 6px', borderRadius: '4px', maxWidth: '120px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {devices.find(d => d.id === liveDeviceId)?.name || liveDeviceId}
                          </span>
                        : <span style={{ fontSize: '0.68rem', color: '#64748b' }}>전체 기기</span>
                      }
                    </div>
                    {/* 요일 칩 */}
                    <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                      {DAY_LABELS.map((label, d) => (
                        <span key={d} style={{ fontSize: '0.68rem', padding: '1px 6px', borderRadius: '4px', fontWeight: liveDays.includes(d) ? 700 : 400, background: liveDays.includes(d) ? (s.enabled ? 'rgba(59,130,246,0.2)' : 'rgba(100,116,139,0.15)') : 'transparent', color: liveDays.includes(d) ? (s.enabled ? '#93c5fd' : '#64748b') : '#334155', border: liveDays.includes(d) ? `1px solid ${s.enabled ? 'rgba(59,130,246,0.3)' : 'rgba(100,116,139,0.3)'}` : '1px solid transparent' }}>
                          {label}
                        </span>
                      ))}
                    </div>
                  </div>

                  {/* 수정·삭제 */}
                  <div style={{ display: 'flex', gap: '6px', flexShrink: 0 }}>
                    <button
                      onClick={() => { startEdit(s); setShowForm(true); }}
                      disabled={isFormOpen}
                      style={{ padding: '5px 10px', borderRadius: '6px', border: '1px solid var(--border)', background: 'transparent', color: isFormOpen ? '#334155' : 'var(--text-secondary)', fontSize: '0.75rem', cursor: isFormOpen ? 'not-allowed' : 'pointer', opacity: isFormOpen ? 0.4 : 1, transition: 'opacity 0.15s' }}>
                      ✏️ 수정
                    </button>
                    <button
                      onClick={() => remove(s.id)}
                      disabled={isEditing}
                      style={{ padding: '5px 10px', borderRadius: '6px', border: '1px solid rgba(239,68,68,0.4)', background: 'transparent', color: '#ef4444', fontSize: '0.75rem', cursor: isEditing ? 'not-allowed' : 'pointer', opacity: isEditing ? 0.3 : 1, transition: 'opacity 0.15s' }}>
                      🗑
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── 추가/수정 폼 패널 ── */}
      {isFormOpen && (
        <div style={{ borderRadius: '12px', border: `1px solid ${editId ? '#f59e0b66' : '#3b82f666'}`, background: editId ? 'rgba(245,158,11,0.05)' : 'rgba(59,130,246,0.05)', padding: '20px' }}>
          <div style={{ fontSize: '0.85rem', fontWeight: 700, color: editId ? '#f59e0b' : '#60a5fa', marginBottom: '16px' }}>
            {editId ? '✏️ 스케줄 수정' : '➕ 새 스케줄 추가'}
          </div>

          {/* 시간 */}
          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', marginBottom: '16px', alignItems: 'flex-end' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.72rem', color: '#94a3b8', marginBottom: '6px' }}>☀️ 켜는 시간 <span style={{ color: '#64748b' }}>(24시)</span></label>
              <TimeStepper value={draft.onTime} onChange={v => setDraft(p => ({ ...p, onTime: v }))} />
            </div>
            <div style={{ color: '#475569', fontSize: '1.4rem', paddingBottom: '6px' }}>→</div>
            <div>
              <label style={{ display: 'block', fontSize: '0.72rem', color: '#94a3b8', marginBottom: '6px' }}>🌙 끄는 시간 <span style={{ color: '#64748b' }}>(24시)</span></label>
              <TimeStepper value={draft.offTime} onChange={v => setDraft(p => ({ ...p, offTime: v }))} />
            </div>
          </div>

          {/* 기기 선택 */}
          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '0.72rem', color: '#94a3b8', marginBottom: '8px' }}>📱 적용 기기</label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
              {/* 전체 기기 */}
              {(() => {
                const sel = draft.deviceId === '';
                return (
                  <button onClick={() => setDraft(p => ({ ...p, deviceId: '' }))}
                    style={{ padding: '5px 12px', borderRadius: '6px', fontSize: '0.72rem', fontWeight: sel ? 700 : 400, cursor: 'pointer', border: sel ? '2px solid #3b82f6' : '1px solid var(--border)', background: sel ? '#3b82f6' : 'rgba(255,255,255,0.03)', color: sel ? '#fff' : '#64748b', transition: 'all 0.15s' }}>
                    전체 기기
                  </button>
                );
              })()}
              {/* 개별 기기 칩 — deviceOrder 순서 적용 */}
              {(() => {
                const orderedIds = Object.values(deviceOrder).flat();
                const sorted = orderedIds.length === 0 ? devices : [
                  ...orderedIds.map(id => devices.find(d => d.id === id)).filter(Boolean),
                  ...devices.filter(d => !orderedIds.includes(d.id)),
                ];
                return sorted.map(dev => {
                  const sel = draft.deviceId === dev.id;
                  const online = dev.status === 'online';
                  return (
                    <button key={dev.id} onClick={() => setDraft(p => ({ ...p, deviceId: sel ? '' : dev.id }))}
                      style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '5px 10px', borderRadius: '6px', fontSize: '0.72rem', fontWeight: sel ? 700 : 400, cursor: 'pointer', border: sel ? '2px solid #3b82f6' : '1px solid var(--border)', background: sel ? 'rgba(59,130,246,0.18)' : 'rgba(255,255,255,0.03)', color: sel ? '#93c5fd' : '#64748b', transition: 'all 0.15s' }}>
                      <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: !online ? '#ef4444' : sel ? '#10b981' : '#334155', flexShrink: 0 }} />
                      {dev.name}
                    </button>
                  );
                });
              })()}
            </div>
          </div>

          {/* 요일 선택 */}
          <div style={{ marginBottom: '20px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <label style={{ fontSize: '0.72rem', color: '#94a3b8' }}>반복 요일</label>
              <label style={{ display: 'flex', alignItems: 'center', gap: '4px', cursor: 'pointer', fontSize: '0.68rem', color: activeDays.length === 7 ? '#3b82f6' : '#64748b', userSelect: 'none' }}>
                <input
                  type="checkbox"
                  checked={activeDays.length === 7}
                  onChange={() => setDraft(p => ({ ...p, days: activeDays.length === 7 ? '' : '0,1,2,3,4,5,6' }))}
                  style={{ width: '12px', height: '12px', accentColor: '#3b82f6', cursor: 'pointer' }}
                />
                전체
              </label>
            </div>
            <div style={{ display: 'flex', gap: '6px' }}>
              {DAY_LABELS.map((label, day) => (
                <button key={day} onClick={() => toggleDay(day)}
                  style={{ flex: 1, height: '28px', borderRadius: '6px', border: activeDays.includes(day) ? '2px solid #3b82f6' : '1px solid var(--border)', background: activeDays.includes(day) ? '#3b82f6' : 'rgba(255,255,255,0.03)', color: activeDays.includes(day) ? '#fff' : '#64748b', fontSize: '0.72rem', cursor: 'pointer', fontWeight: activeDays.includes(day) ? 700 : 400, transition: 'all 0.15s' }}>
                  {label}
                </button>
              ))}
            </div>
          </div>

          {/* 저장·취소 */}
          <div style={{ display: 'flex', gap: '8px' }}>
            <button onClick={handleSave} disabled={saving}
              style={{ flex: 1, padding: '7px', borderRadius: '7px', border: 'none', background: saving ? '#374151' : (editId ? '#f59e0b' : '#3b82f6'), color: '#fff', fontSize: '0.78rem', fontWeight: 700, cursor: saving ? 'default' : 'pointer' }}>
              {saving ? '저장 중…' : editId ? '수정 저장' : '저장 및 전송'}
            </button>
            <button onClick={closeForm}
              style={{ padding: '7px 14px', borderRadius: '7px', border: '1px solid var(--border)', background: 'transparent', color: 'var(--text-secondary)', fontSize: '0.78rem', cursor: 'pointer' }}>
              취소
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function SettingsTab({ onUnauth, deviceOrder = {} }) {
  const [otaStatus, setOtaStatus] = useState(null);
  const [pushing, setPushing] = useState(false);
  const [adbRunning, setAdbRunning] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [pushResults, setPushResults] = useState(null); // { deviceId, success, label }[]
  // 기기 목록 + 선택
  const [allDevices, setAllDevices] = useState([]);
  const [selectedIds, setSelectedIds] = useState(new Set());
  // 기기별 다운로드/설치 진행률 { [deviceId]: { cur, total, pct, label } | null }
  const [dlProgress, setDlProgress] = useState({});
  const adbControllerRef = useRef(null);
  const apkInputRef = useRef(null);
  const [localApkVersion, setLocalApkVersion] = useState(() => localStorage.getItem('signagepro_apk_version'));
  const [serverApkVersion, setServerApkVersion] = useState(null);

  const check401 = (res) => { if (res.status === 401) { onUnauth?.(); throw new Error('401'); } return res; };

  const refreshStatus = () =>
    apiFetch(`${SOCKET_URL}/api/update/status`)
      .then(check401)
      .then(r => r.json())
      .then(data => { setOtaStatus(data); if (data?.apkVersion) setServerApkVersion(data.apkVersion); })
      .catch(e => { if (e.message !== '401') setOtaStatus({ available: false }); });

  const refreshDevices = () =>
    apiFetch(`${SOCKET_URL}/api/devices`)
      .then(r => r.json())
      .then(devs => {
        setAllDevices(devs);
        // 처음 로드 시 온라인 기기만 자동 선택 (설치 중일 땐 변경 안 함)
        setSelectedIds(prev => {
          if (prev.size === 0 && !pushing) return new Set(devs.filter(d => d.status === 'online').map(d => d.id));
          return prev;
        });
      })
      .catch(() => {});

  // 페이지 복귀 시 서버 설치 상태 복원
  const checkAdbStatus = () =>
    apiFetch(`${SOCKET_URL}/api/update/adb-status`)
      .then(r => r.json())
      .then(s => {
        if (s.running) {
          setPushing(true);
          setAdbRunning(true);
          if (s.deviceIds?.length) setSelectedIds(new Set(s.deviceIds));
        } else if (s.results) {
          setPushResults(s.results.map(r => ({
            id: r.deviceId, success: r.success,
            label: r.success ? '✅ 완료' : '❌ 실패'
          })));
          if (s.deviceIds?.length) setSelectedIds(new Set(s.deviceIds));
        }
      })
      .catch(() => {});

  useEffect(() => {
    refreshStatus();
    refreshDevices();
    checkAdbStatus();
    const t = setInterval(refreshDevices, 10000);

    // 소켓으로 기기 진행률 수신
    const socket = io(SOCKET_URL);

    // OTA: 기기가 서버에서 APK 다운로드하는 진행률
    socket.on('device_status_update', (update) => {
      setAllDevices(prev => prev.map(d =>
        d.id === update.deviceId ? { ...d, status: update.status, dl: update.dl ?? null } : d
      ));
      setDlProgress(prev => {
        const next = { ...prev };
        if (update.dl) {
          // OTA 다운로드 진행률 (isAdb 없음)
          const isApk = update.dl.cur === 0 && update.dl.total === 0;
          next[update.deviceId] = {
            pct: update.dl.pct ?? 0,
            label: isApk ? '자료전송중' : '다운로드중',
            color: isApk ? '#8b5cf6' : '#3b82f6',
          };
        } else if (next[update.deviceId]?.isAdb) {
          // ADB 설치 완료 후 기기가 다시 접속 → "PC 응답 대기"로 전환 (삭제 안 함)
          if (update.status === 'online') {
            next[update.deviceId] = { pct: 100, label: 'PC 응답 대기', color: '#475569', isAdb: true };
          }
          // 오프라인이면 현재 상태(기기 앱 시작 중) 유지
        } else {
          // OTA 항목이면 dl 없을 때 삭제
          delete next[update.deviceId];
        }
        return next;
      });
    });

    // ADB: 서버 PC에서 직접 adb install 하는 진행률
    socket.on('adb_install_progress', ({ deviceId, stage, pct }) => {
      setDlProgress(prev => {
        const next = { ...prev };
        if (stage === 'success') {
          // 2초간 "설치 완료" 표시 후 → 기기가 돌아올 때까지 "기기 앱 시작 중" 유지
          next[deviceId] = { pct: 100, label: '설치 완료', color: '#10b981', isAdb: true };
          setTimeout(() => setDlProgress(p => p[deviceId]
            ? { ...p, [deviceId]: { pct: 100, label: '기기 앱 시작 중', color: '#64748b', isAdb: true } }
            : p
          ), 2000);
        } else if (stage === 'failed' || stage === 'cancelled') {
          next[deviceId] = { pct: pct ?? 0, label: '설치 실패', color: '#ef4444', isAdb: true };
          setTimeout(() => setDlProgress(p => { const n = { ...p }; delete n[deviceId]; return n; }), 5000);
        } else if (stage === 'connecting') {
          next[deviceId] = { pct: pct ?? 10, label: '기기 연결 중', color: '#64748b', isAdb: true };
        } else if (stage === 'finalizing') {
          next[deviceId] = { pct: pct ?? 85, label: '앱 실행 중', color: '#8b5cf6', isAdb: true };
        } else {
          // installing: ADB로 APK를 기기에 직접 설치하는 단계
          next[deviceId] = { pct: pct ?? 30, label: 'APK 설치 중', color: '#f59e0b', isAdb: true };
        }
        return next;
      });
    });

    return () => { clearInterval(t); socket.disconnect(); };
  }, []);

  const handleApkUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.name.endsWith('.apk')) { alert('APK 파일만 업로드 가능합니다.'); return; }
    if (!window.confirm(`${file.name} (${(file.size / 1024 / 1024).toFixed(1)} MB) 을 업로드할까요?`)) return;

    const verMatch = file.name.match(/(\d+\.\d+\.\d+(?:\.\d+)?)/);
    if (verMatch) { localStorage.setItem('signagepro_apk_version', verMatch[1]); setLocalApkVersion(verMatch[1]); }

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
          setOtaStatus({ available: true, size: data.size, updatedAt: data.updatedAt, lastDeployedAt: null });
        } catch { refreshStatus(); }
      } else { alert('업로드 실패: ' + xhr.responseText); }
    };
    xhr.onerror = () => { setUploading(false); alert('업로드 중 오류가 발생했습니다.'); };
    xhr.send(form);
  };

  // 선택된 기기 이름 목록
  const selectedDevices = allDevices.filter(d => selectedIds.has(d.id));
  const selectedLabel = selectedDevices.length === 0
    ? '선택된 기기 없음'
    : selectedDevices.length === allDevices.length
      ? `전체 ${allDevices.length}대`
      : `${selectedDevices.map(d => d.name).join(', ')} (${selectedDevices.length}대)`;

  const toggleDevice = (id) =>
    setSelectedIds(prev => { const s = new Set(prev); s.has(id) ? s.delete(id) : s.add(id); return s; });

  const toggleAll = () =>
    setSelectedIds(prev =>
      prev.size === allDevices.length ? new Set() : new Set(allDevices.map(d => d.id))
    );

  const pushUpdate = async () => {
    if (selectedIds.size === 0) { alert('배포할 기기를 선택하세요.'); return; }
    if (!window.confirm(`${selectedLabel}에 OTA 업데이트를 배포할까요?`)) return;
    const ids = [...selectedIds];
    setPushing(true);
    setPushResults(null);
    // 선택된 모든 기기 → "알림 전송 중" 표시
    setDlProgress(prev => {
      const next = { ...prev };
      ids.forEach(id => { next[id] = { pct: 0, label: '알림 전송 중', color: '#f97316' }; });
      return next;
    });
    try {
      const results = await Promise.all(
        ids.map(async (id) => {
          const dev = allDevices.find(d => d.id === id);
          try {
            const r = await apiFetch(`${SOCKET_URL}/api/update/push`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ deviceId: id })
            }).then(check401).then(r => r.json());
            // 푸시 전달 완료 → dlProgress 해제 (배지 표시, 기기가 다운로드 시작하면 다시 채워짐)
            setDlProgress(prev => { const n = { ...prev }; delete n[id]; return n; });
            return { id, name: dev?.name || id, success: r.success };
          } catch (e) {
            if (e.message === '401') throw e;
            setDlProgress(prev => { const n = { ...prev }; delete n[id]; return n; });
            return { id, name: dev?.name || id, success: false, error: e.message };
          }
        })
      );
      setPushResults(results);
      // 배포 완료 → 배포 시각 갱신
      if (results.some(r => r.success)) {
        setOtaStatus(prev => prev ? { ...prev, lastDeployedAt: new Date().toISOString() } : prev);
      }
    } catch (e) {
      if (e.message !== '401') alert('오류: ' + e.message);
      setDlProgress(prev => {
        const n = { ...prev };
        ids.forEach(id => delete n[id]);
        return n;
      });
    } finally {
      setPushing(false);
    }
  };

  const adbInstall = async () => {
    if (selectedIds.size === 0) { alert('설치할 기기를 선택하세요.'); return; }
    if (!window.confirm(`${selectedLabel}에 ADB 직접 설치할까요?`)) return;
    adbControllerRef.current = new AbortController();
    setPushing(true);
    setAdbRunning(true);
    setPushResults(null);
    // 선택된 모든 기기를 즉시 "대기중"으로 표시 — 순차 설치 대기 상태 시각화
    const ids = [...selectedIds];
    setDlProgress(prev => {
      const next = { ...prev };
      ids.forEach(id => { next[id] = { pct: 0, label: '대기중', color: '#475569' }; });
      return next;
    });
    try {
      const d = await apiFetch(`${SOCKET_URL}/api/update/adb-install`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deviceIds: ids }),
        signal: adbControllerRef.current.signal
      }).then(check401).then(r => r.json());

      const mapped = (d.results || []).map(r => {
        const dev = allDevices.find(dev => dev.id === r.deviceId);
        return { id: r.deviceId, name: dev?.name || r.deviceId, success: r.success, output: r.output };
      });
      // dlProgress 클리어와 배지 표시를 동시에 — 공백 없이 바↔배지 교체
      setDlProgress(prev => {
        const next = { ...prev };
        ids.forEach(id => delete next[id]);
        return next;
      });
      setPushResults(mapped);
      // ADB 설치 완료 → 배포 시각 갱신
      if (mapped.some(r => r.success)) {
        setOtaStatus(prev => prev ? { ...prev, lastDeployedAt: new Date().toISOString() } : prev);
      }
    } catch (e) {
      if (e.name === 'AbortError') { /* 취소됨 */ }
      else if (e.message !== '401') alert('오류: ' + e.message);
    } finally {
      setPushing(false);
      setAdbRunning(false);
    }
  };

  const cancelAdbInstall = () => {
    adbControllerRef.current?.abort();
    apiFetch(`${SOCKET_URL}/api/update/adb-cancel`, { method: 'POST' }).catch(() => {});
    setPushing(false);
    setAdbRunning(false);
    setPushResults(null);
    setDlProgress({});
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
                ✅ APK 준비됨 — {(otaStatus.size / 1024 / 1024).toFixed(1)} MB{(serverApkVersion || localApkVersion) && <>&nbsp;<span style={{ background: 'rgba(16,185,129,0.15)', color: '#10b981', fontWeight: 700, padding: '1px 6px', borderRadius: '4px', fontSize: '0.8rem' }}>v{serverApkVersion || localApkVersion}</span></>}
                {otaStatus.lastDeployedAt
                  ? <>&nbsp;<span style={{ color: '#64748b' }}>업로드 {new Date(otaStatus.updatedAt).toLocaleString('ko-KR')}</span>
                      &nbsp;·&nbsp;<span style={{ color: '#10b981', fontWeight: 600 }}>마지막 배포 {new Date(otaStatus.lastDeployedAt).toLocaleString('ko-KR')}</span></>
                  : <>&nbsp;({new Date(otaStatus.updatedAt).toLocaleString('ko-KR')})</>
                }
              </span>
              <button
                style={{ flexShrink: 0, padding: '4px 10px', fontSize: '0.78rem', background: '#EF4444', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer' }}
                onClick={() => {
                  if (!window.confirm('배포용 APK를 삭제할까요?')) return;
                  apiFetch(`${SOCKET_URL}/api/update/apk`, { method: 'DELETE' })
                    .then(r => r.json())
                    .then(d => { if (d.ok) { setOtaStatus({ available: false }); localStorage.removeItem('signagepro_apk_version'); setLocalApkVersion(null); } else alert(d.error); })
                    .catch(e => alert('오류: ' + e.message));
                }}
              >
                🗑 삭제
              </button>
            </>
          ) : (
            <span style={{ color: '#EF4444', fontSize: '0.85rem' }}>
              ❌ APK 없음 — server/update/app.apk 를 배치하세요
            </span>
          )}
        </div>

        {/* 기기 선택 */}
        <div style={{ marginBottom: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
            <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>📱 배포 대상 기기</span>
            <button
              style={{ fontSize: '0.78rem', padding: '3px 10px', background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.12)', borderRadius: '6px', color: '#e2e8f0', cursor: 'pointer' }}
              onClick={toggleAll}
            >
              {selectedIds.size === allDevices.length ? '전체 해제' : '전체 선택'}
            </button>
          </div>
          {allDevices.length === 0 ? (
            <p style={{ fontSize: '0.82rem', color: '#64748b', padding: '10px 0' }}>기기 없음</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {(() => {
                const orderedIds = Object.values(deviceOrder).flat();
                const sorted = orderedIds.length === 0 ? allDevices : [
                  ...orderedIds.map(id => allDevices.find(d => d.id === id)).filter(Boolean),
                  ...allDevices.filter(d => !orderedIds.includes(d.id)),
                ];
                return sorted;
              })().map(dev => {
                const isOnline = dev.status === 'online';
                const checked = selectedIds.has(dev.id);
                const result = pushResults?.find(r => r.id === dev.id);
                return (
                  <div key={dev.id}>
                    <label
                      style={{
                        display: 'flex', alignItems: 'center', gap: '10px',
                        padding: '9px 12px', borderRadius: '8px', cursor: 'pointer',
                        background: checked ? 'rgba(249,115,22,0.08)' : 'rgba(255,255,255,0.03)',
                        border: `1px solid ${checked ? 'rgba(249,115,22,0.35)' : 'rgba(255,255,255,0.07)'}`,
                        transition: 'all 0.15s'
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggleDevice(dev.id)}
                        style={{ width: '15px', height: '15px', accentColor: '#F97316', cursor: 'pointer' }}
                      />
                      <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: isOnline ? '#10B981' : '#475569', flexShrink: 0 }} />
                      <span style={{ flex: 1, fontSize: '0.88rem', color: isOnline ? '#e2e8f0' : '#94a3b8' }}>
                        {dev.name}
                      </span>
                      {dev.appVersion && (
                        <span style={{ fontSize: '0.75rem', color: '#64748b', fontFamily: 'monospace' }}>
                          v{dev.appVersion}
                        </span>
                      )}
                      <span style={{ fontSize: '0.75rem', color: isOnline ? '#10B981' : '#475569' }}>
                        {isOnline ? '온라인' : '오프라인'}
                      </span>
                      {result && !dlProgress[dev.id] && (
                        <span style={{ fontSize: '0.75rem', color: result.success ? '#10B981' : '#EF4444', fontWeight: 600 }}>
                          {result.success ? '✅ 전송됨' : '❌ 실패'}
                        </span>
                      )}
                    </label>
                    {/* 다운로드/설치 진행률 바 (OTA + ADB 공용) */}
                    {(() => {
                      // dlProgress가 있으면 그걸 우선 사용
                      const dp = dlProgress[dev.id];
                      // 설치/배포 진행 중 + 이 기기가 선택돼 있고 + 아직 결과 없을 때 → 폴백 상태
                      const otaPushing = pushing && !adbRunning;
                      const isQueued = (adbRunning || otaPushing) && selectedIds.has(dev.id) && !result;
                      if (!dp && !isQueued) return null;
                      const pct   = dp?.pct   ?? 0;
                      const color = dp?.color ?? '#475569';
                      const label = dp?.label ?? (
                        adbRunning
                          ? (!isOnline ? '기기 앱 시작 중' : 'PC 전송 대기')
                          : (!isOnline ? '기기 재시작 중'  : '알림 전송 중')
                      );
                      return (
                        <div style={{ padding: '4px 12px 8px', marginTop: -4 }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color, marginBottom: 3 }}>
                            <span>{label}</span>
                            {pct > 0 && <span style={{ fontWeight: 700 }}>{pct}%</span>}
                          </div>
                          <div style={{ height: 5, borderRadius: 3, background: 'rgba(255,255,255,0.08)' }}>
                            <div style={{ height: '100%', borderRadius: 3, background: color, width: `${pct}%`, transition: 'width 0.3s' }} />
                          </div>
                        </div>
                      );
                    })()}
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* 배포 버튼 */}
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
          <button
            className="btn btn-primary"
            disabled={pushing || !otaStatus?.available || selectedIds.size === 0}
            onClick={pushUpdate}
          >
            {pushing && !adbRunning ? '배포 중…' : `🚀 OTA 푸시 (${selectedIds.size}대)`}
          </button>
          <button
            className="btn btn-primary"
            style={{ background: '#10B981' }}
            disabled={pushing || !otaStatus?.available || selectedIds.size === 0}
            onClick={adbInstall}
          >
            {adbRunning ? '설치 중…' : `⚡ ADB 설치 (${selectedIds.size}대)`}
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

      <ScreenScheduleSection onUnauth={onUnauth} deviceOrder={deviceOrder} />
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
  const [currentDateTime, setCurrentDateTime] = useState(new Date());

  useEffect(() => {
    const clockTimer = setInterval(() => {
      setCurrentDateTime(new Date());
    }, 1000);
    return () => clearInterval(clockTimer);
  }, []);
  const [devices, setDevicesState] = useState(() => {
    try {
      const cached = JSON.parse(localStorage.getItem('SIGNAGE_DEVICES') || '[]');
      // 초기 로드 시에는 혼선을 피하기 위해 시각적인 상태를 안전하게 오프라인 상태로 우선 복원합니다.
      return cached.map(d => ({ ...d, status: 'offline' }));
    } catch { return []; }
  });

  const [groups, setGroupsState] = useState(() => {
    try { return JSON.parse(localStorage.getItem('SIGNAGE_GROUPS') || '[]'); } catch { return []; }
  });

  const [stores, setStoresState] = useState(() => {
    try { return JSON.parse(localStorage.getItem('SIGNAGE_STORES') || '[]'); } catch { return []; }
  });

  const setDevices = useCallback((updater) => {
    setDevicesState(prev => {
      const next = typeof updater === 'function' ? updater(prev) : updater;
      localStorage.setItem('SIGNAGE_DEVICES', JSON.stringify(next));
      return next;
    });
  }, []);

  const setGroups = useCallback((updater) => {
    setGroupsState(prev => {
      const next = typeof updater === 'function' ? updater(prev) : updater;
      localStorage.setItem('SIGNAGE_GROUPS', JSON.stringify(next));
      return next;
    });
  }, []);

  const setStores = useCallback((updater) => {
    setStoresState(prev => {
      const next = typeof updater === 'function' ? updater(prev) : updater;
      localStorage.setItem('SIGNAGE_STORES', JSON.stringify(next));
      return next;
    });
  }, []);

  // PC 스피커로 듣기 — deviceId Set (체크된 기기만 음소거 해제)
  const [pcAudioSet, setPcAudioSet] = useState(new Set());
  const [selectedStoreId, setSelectedStoreId] = useState('');
  const [gridLayout, setGridLayout] = useState('auto');
  const [deviceMeta, setDeviceMeta] = useState({});
  // 음소거 전 볼륨 기억 — 음소거 해제 시 원래 볼륨으로 복원
  const preMuteVol = useRef({});
  const [customGrid, setCustomGrid] = useState({ rows: 2, cols: 3 });
  const [apkAvailable, setApkAvailable] = useState(false);
  const [adbRunning, setAdbRunning] = useState(false);
  const [serverOnline, setServerOnline] = useState(null); // null=확인중, true=연결, false=끊김
  const [allSchedules, setAllSchedules] = useState([]);
  // 기기 표시 순서 (그룹별 device id 배열, localStorage 영속)
  const [deviceOrder, setDeviceOrderState] = useState(() => {
    try { return JSON.parse(localStorage.getItem('SIGNAGE_DEVICE_ORDER') || '{}'); } catch { return {}; }
  });
  const setDeviceOrder = (updater) => {
    setDeviceOrderState(prev => {
      const next = typeof updater === 'function' ? updater(prev) : updater;
      localStorage.setItem('SIGNAGE_DEVICE_ORDER', JSON.stringify(next));
      return next;
    });
  };

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
        setDevices(prev => {
          return mappedDevices.map(newDev => {
            const oldDev = prev.find(d => d.id === newDev.id);
            if (oldDev) {
              return {
                ...newDev,
                ip: newDev.ip || oldDev.ip,               // IP가 null로 덮어써지면 🕐 블록 전체가 사라짐
                deviceTime: newDev.deviceTime ?? oldDev.deviceTime,
                vu: newDev.vu ?? oldDev.vu,
                vol: newDev.vol ?? oldDev.vol,
                dl: newDev.dl ?? oldDev.dl,
                slide: newDev.slide ?? oldDev.slide,
                screenOff: newDev.screenOff ?? oldDev.screenOff ?? false
              };
            }
            return newDev;
          });
        });
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

  const fetchSchedules = useCallback(() => {
    apiFetch(`${SOCKET_URL}/api/schedules`)
      .then(r => r.json())
      .then(data => { console.log('[schedules]', data); setAllSchedules(Array.isArray(data) ? data : []); })
      .catch(err => console.error('[schedules] fetch 실패:', err));
  }, []);

  useEffect(() => {
    fetchDevices();
    fetchGroups();
    fetchStores();
    fetchSchedules();

    const socket = io(SOCKET_URL);

    socket.on('connect', () => {
      setServerOnline(true);
      // 재연결 시 DB 실제 상태로 동기화 (끊긴 동안 놓친 offline 이벤트 복구)
      fetchDevices();
    });
    socket.on('disconnect', () => {
      setServerOnline(false);
      setDevices(prev => prev.map(d => ({ ...d, status: 'offline' })));
    });
    socket.on('connect_error', () => {
      setServerOnline(false);
      setDevices(prev => prev.map(d => ({ ...d, status: 'offline' })));
    });

    socket.on('device_status_update', (update) => {
      setDevices(prev => {
        const exists = prev.find(d => d.id === update.deviceId);
        if (exists) {
          return prev.map(d =>
            d.id === update.deviceId
              ? { ...d, status: update.status, cpuUsage: update.cpu ?? d.cpuUsage, memUsage: update.mem ?? d.memUsage, ip: update.ip || d.ip, appVersion: update.appVersion || d.appVersion, dl: update.dl ?? null, vol: update.vol ?? d.vol, deviceTime: update.deviceTime ?? d.deviceTime, slide: update.slide ?? d.slide, screenOff: update.screenOff ?? d.screenOff ?? false }
              : d
          );
        } else {
          fetchDevices();
          return prev;
        }
      });
    });

    // VU 실시간 레벨 (300ms)
    socket.on('device_vu_update', ({ deviceId, vu }) => {
      setDevices(prev => prev.map(d => d.id === deviceId ? { ...d, vu } : d));
    });

    socket.on('group_assignment_changed', () => {
      fetchDevices();
      fetchGroups();
    });

    socket.on('screen_schedule', () => fetchSchedules());

    // 폴백 폴링: Socket.io 이벤트 유실 대비 — 15초마다 DB 실제 상태로 강제 동기화
    // 서버 GET /api/devices가 lastSeen 기반으로 실시간 재계산하므로 폴링만으로도 정확히 감지됨
    const pollTimer = setInterval(() => fetchDevices(), 15_000);

    return () => { socket.disconnect(); clearInterval(pollTimer); };
  }, []);

  const filteredDevices = (() => {
    const base = devices.filter(d => {
      if (!d.groupId) return false;
      if (selectedStoreId === 'all') return true;
      if (selectedStoreId === 'unassigned') return !d.storeId;
      return d.storeId === selectedStoreId;
    });
    // 그룹별 저장된 순서 적용
    const orderMap = deviceOrder; // { [groupId]: [id, id, ...] }
    const orderedIds = Object.values(orderMap).flat();
    if (orderedIds.length === 0) return base;
    return [
      ...orderedIds.map(id => base.find(d => d.id === id)).filter(Boolean),
      ...base.filter(d => !orderedIds.includes(d.id)),
    ];
  })();

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
        <div style={{ padding: '16px', marginTop: 'auto', display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {/* Dashboard Version & Last Updated Date/Time display */}
          <div style={{ padding: '10px', borderRadius: '8px', background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)', display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem', color: '#64748b', alignItems: 'center' }}>
              <span>대시보드 버전</span>
              <span style={{ fontWeight: 600, color: '#3b82f6', fontFamily: 'monospace' }}>v{__APP_VERSION__}</span>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', fontSize: '0.68rem', color: '#64748b', borderTop: '1px solid rgba(255,255,255,0.05)', paddingTop: '6px', marginTop: '2px' }}>
              <span style={{ color: '#475569', fontSize: '0.62rem', fontWeight: 600, textTransform: 'uppercase' }}>빌드 날짜</span>
              <span style={{ color: 'var(--text-secondary)', fontFamily: 'monospace', fontWeight: 600 }}>{__BUILD_DATE__}</span>
            </div>
          </div>

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
                  <option value="4x1">4 x 1</option>
                  <option value="4x3">4 x 3</option>
                  <option value="5x1">5 x 1</option>
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
                    style={{ animationDelay: `${index * 0.1}s`, position: 'relative' }}
                  >
                    {/* 서버 오프라인 오버레이 레이어 추가 */}
                    {serverOnline !== true && (
                      <div style={{
                        position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
                        background: 'rgba(15, 23, 42, 0.78)', // 어두운 반투명 색상
                        backdropFilter: 'blur(3px)', // 흐림 효과
                        borderRadius: '16px', // 기기 카드 둥근 모서리 맞춤
                        display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                        zIndex: 10, gap: '10px',
                        border: '1px solid rgba(239, 68, 68, 0.2)',
                        padding: '16px', textAlign: 'center'
                      }}>
                        <div style={{
                          width: '38px', height: '38px', borderRadius: '50%', background: 'rgba(239, 68, 68, 0.15)',
                          display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#EF4444', fontWeight: 'bold', fontSize: '1.1rem'
                        }}>
                          ⚠️
                        </div>
                        <div style={{ fontWeight: 700, fontSize: '0.88rem', color: '#EF4444' }}>서버 통신 차단</div>
                        <div style={{ fontSize: '0.7rem', color: '#94a3b8', lineHeight: '1.3' }}>
                          대시보드 서버가 가동 상태가 아니므로<br />기기 실시간 모니터링을 진행할 수 없습니다.
                        </div>
                      </div>
                    )}

                    {/* 화면 꺼짐 오버레이 — 스케줄 OFF, 기기는 온라인 유지 */}
                    {device.screenOff && device.status === 'online' && serverOnline === true && (
                      <div style={{
                        position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
                        background: 'rgba(8, 12, 28, 0.86)',
                        backdropFilter: 'blur(2px)',
                        borderRadius: '16px',
                        display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                        zIndex: 5, gap: '8px',
                        border: '1px solid rgba(99,102,241,0.25)',
                        padding: '16px', textAlign: 'center', pointerEvents: 'none'
                      }}>
                        <div style={{ fontSize: '1.6rem', lineHeight: 1 }}>🌙</div>
                        <div style={{ fontWeight: 700, fontSize: '0.82rem', color: '#818cf8' }}>화면 꺼짐</div>
                        <div style={{ fontSize: '0.66rem', color: '#475569', lineHeight: '1.4' }}>
                          스케줄에 의해 꺼진 상태<br/>기기는 정상 연결 중
                        </div>
                      </div>
                    )}

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
                            pcAudio={pcAudioSet.has(device.id)}
                            devVol={device.vol ?? 8}
                            liveSlide={device.slide ?? null}
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
                        {(device.ip || device.deviceTime) && (
                          <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', fontFamily: 'monospace', marginTop: '2px', display: 'flex', gap: '6px', alignItems: 'center', flexWrap: 'nowrap', overflow: 'hidden' }}>
                            {device.ip && <span style={{ flexShrink: 0 }}>{device.ip}</span>}
                            {device.deviceTime && (() => {
                              // lastSeen 기준으로 보정: 마지막 하트비트 후 경과 시간만큼 더해 현재 추정 시각 표시
                              const lastSeenMs = device.lastSeen ? new Date(device.lastSeen).getTime() : null;
                              const adjTime = lastSeenMs ? device.deviceTime + (Date.now() - lastSeenMs) : device.deviceTime;
                              return (
                                <span style={{ color: '#475569', fontFamily: 'monospace', flexShrink: 0, whiteSpace: 'nowrap' }} title="기기 현재 시각 (KST 기준으로 스케줄 적용됨)">
                                  🕐 {new Date(adjTime).toLocaleTimeString('ko-KR', { timeZone: 'Asia/Seoul', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })}
                                </span>
                              );
                            })()}
                          </div>
                        )}
                        {(() => {
                          const sched = allSchedules.filter(s => s.enabled && (!s.deviceId || s.deviceId === device.id));
                          if (!sched.length) return null;
                          const s = sched[0];
                          return (
                            <div style={{ fontSize: '0.72rem', color: '#38bdf8', marginTop: '3px', display: 'flex', gap: '8px', flexWrap: 'nowrap', overflow: 'hidden' }}>
                              {s.onTime  ? <span style={{ flexShrink: 0, whiteSpace: 'nowrap' }}>☀️ {s.onTime}</span>  : null}
                              {s.offTime ? <span style={{ flexShrink: 0, whiteSpace: 'nowrap' }}>🌙 {s.offTime}</span> : null}
                            </div>
                          );
                        })()}
                        {deviceMeta[device.id] && (
                          <div style={{ marginTop: '4px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', marginBottom: '2px', overflow: 'hidden' }}>
                              <Film size={10} style={{ color: 'var(--text-secondary)', flexShrink: 0 }} />
                              <span style={{ fontSize: '0.65rem', color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                {deviceMeta[device.id].filename}
                              </span>
                            </div>
                            {/* 시간 / 전환타입 — 항상 한 줄 (whiteSpace:nowrap으로 강제) */}
                            <div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', marginTop: '1px' }}>
                              <span style={{ fontFamily: 'monospace', fontSize: '0.65rem', color: 'var(--text-secondary)' }}>
                                {formatTime(deviceMeta[device.id].currentTime)} / {formatTime(deviceMeta[device.id].duration)}
                              </span>
                              <span style={{ marginLeft: '6px', fontSize: '0.58rem', color: '#60a5fa', fontWeight: 700, textTransform: 'uppercase', background: 'rgba(96,165,250,0.1)', padding: '1px 4px', borderRadius: 3 }}>
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
                        {device.appVersion ? (() => {
                          const isOutdated = standardVersion && device.appVersion !== standardVersion;
                          // "0.4.0 (2026-05-27 12:50)" → verNum="0.4.0", verDate="2026-05-27 12:50"
                          const parenIdx = device.appVersion.indexOf(' (');
                          const verNum  = parenIdx >= 0 ? device.appVersion.slice(0, parenIdx) : device.appVersion;
                          const verDate = parenIdx >= 0 ? device.appVersion.slice(parenIdx + 2).replace(/\)$/, '') : null;
                          return (
                            <div style={{
                              display: 'flex',
                              flexDirection: 'column',
                              alignItems: 'flex-end',
                              textAlign: 'right',
                              lineHeight: 1.3
                            }}>
                              <span
                                style={{ fontSize: '0.65rem', fontFamily: 'monospace', whiteSpace: 'nowrap',
                                  color: isOutdated ? '#f59e0b' : 'var(--text-secondary)',
                                  fontWeight: isOutdated ? 700 : 400 }}
                                title={`v${device.appVersion}${isOutdated ? `  (기준: v${standardVersion})` : ''}`}
                              >
                                {isOutdated ? '⚠ ' : ''}v{verNum}
                              </span>
                              {verDate && (
                                <span style={{ fontSize: '0.6rem', fontFamily: 'monospace', whiteSpace: 'nowrap', color: '#64748b' }}>
                                  {verDate}
                                </span>
                              )}
                            </div>
                          );
                        })() : (
                          <span style={{ fontSize: '0.65rem', color: '#f59e0b', fontWeight: 700, whiteSpace: 'nowrap' }} title="앱 버전 미확인">
                            ⚠ 버전 미확인
                          </span>
                        )}
                      </div>
                    </div>

                    {/* 볼륨 + 그라디언트 레벨미터 (Option C) */}
                    {(() => {
                      const vol = device.vol;
                      const volPct = vol != null ? Math.round(vol / 15 * 100) : null;
                      const isMuted = vol === 0;
                      // 비디오 재생 중일 때만 활성 — 이미지 전용이거나 오프라인이면 비활성
                      const isVideo = /\.(mp4|mov|avi|mkv|webm|m4v)$/i.test(deviceMeta[device.id]?.filename || '');
                      const canAudio = device.status === 'online' && isVideo;
                      const sendVol = (level) => {
                        if (!canAudio) return;
                        setDevices(prev => prev.map(d => d.id === device.id ? { ...d, vol: level } : d));
                        apiFetch(`${SOCKET_URL}/api/devices/${device.id}/volume`, {
                          method: 'POST',
                          headers: { 'Content-Type': 'application/json' },
                          body: JSON.stringify({ level })
                        });
                      };
                      return (
                        <div style={{ padding: '4px 8px 6px', opacity: canAudio ? 1 : 0.28, pointerEvents: canAudio ? 'auto' : 'none' }}>
                          {/* 상단: 볼륨 라벨+퍼센트  |  우측: L/R 레벨미터 */}
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: 5 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                              <span style={{ fontSize: '0.6rem', color: '#475569' }}>볼륨</span>
                              {vol == null
                                ? <span style={{ fontSize: '0.6rem', color: '#334155' }}>미확인</span>
                                : isMuted
                                  ? <span style={{ fontSize: '0.63rem', color: '#ef4444', fontWeight: 700 }}>🔇 음소거</span>
                                  : <span style={{ fontSize: '0.7rem', color: '#3b82f6', fontWeight: 700, fontFamily: 'monospace' }}>{volPct}%</span>
                              }
                            </div>
                            {/* 우측 컴팩트 L/R 레벨미터 */}
                            <GradientLevelMeter vu={device.vu} />
                          </div>
                          {/* 슬라이더 + 음소거 */}
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <label title="음소거" style={{ display: 'flex', alignItems: 'center', gap: 2, cursor: 'pointer', flexShrink: 0 }}>
                              <input
                                type="checkbox"
                                checked={isMuted}
                                onChange={e => {
                                  if (e.target.checked) {
                                    // 음소거: 현재 볼륨 기억 후 0으로
                                    preMuteVol.current[device.id] = vol ?? 8;
                                    sendVol(0);
                                  } else {
                                    // 음소거 해제: 기억된 볼륨 복원 (없으면 8)
                                    sendVol(preMuteVol.current[device.id] ?? 8);
                                  }
                                }}
                                style={{ accentColor: '#ef4444', cursor: 'pointer', width: 13, height: 13 }}
                              />
                              <span style={{ fontSize: '0.65rem', userSelect: 'none' }}>🔇</span>
                            </label>
                            <div style={{ flex: 1 }}>
                              <input
                                type="range" min={0} max={15} step={1}
                                value={vol ?? 8}
                                style={{ width: '100%', accentColor: '#3b82f6', cursor: 'pointer', display: 'block' }}
                                onChange={e => {
                                  const level = Number(e.target.value);
                                  setDevices(prev => prev.map(d => d.id === device.id ? { ...d, vol: level } : d));
                                }}
                                onMouseUp={e => sendVol(Number(e.target.value))}
                              />
                            </div>
                          </div>
                        </div>
                      );
                    })()}

                    {/* PC 스피커로 듣기 — 볼륨 연동 */}
                    {(() => {
                      const isVideo = /\.(mp4|mov|avi|mkv|webm|m4v)$/i.test(deviceMeta[device.id]?.filename || '');
                      const canAudio = device.status === 'online' && isVideo;
                      return (
                    <label style={{ display: 'flex', alignItems: 'center', gap: 5,
                      cursor: canAudio ? 'pointer' : 'default',
                      padding: '3px 8px', marginBottom: 6,
                      background: 'transparent', border: 'none',
                      opacity: canAudio ? 1 : 0.28,
                      pointerEvents: canAudio ? 'auto' : 'none',
                      transition: 'all 0.2s'
                    }}>
                      <input
                        type="checkbox"
                        checked={pcAudioSet.has(device.id)}
                        onChange={e => {
                          setPcAudioSet(prev => {
                            const next = new Set(prev);
                            if (e.target.checked) next.add(device.id);
                            else next.delete(device.id);
                            return next;
                          });
                        }}
                        style={{ accentColor: '#3b82f6', cursor: 'pointer', width: 12, height: 12 }}
                      />
                      <span style={{ fontSize: '0.6rem', color: pcAudioSet.has(device.id) ? '#3b82f6' : '#475569',
                        userSelect: 'none', fontWeight: pcAudioSet.has(device.id) ? 600 : 400 }}>
                        🖥️ PC 스피커로 듣기
                      </span>
                    </label>
                      ); })()}

                    {/* CPU / 메모리 */}
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

                    {/* 다운로드 / APK 업데이트 진행바 */}
                    {device.dl && (() => {
                      const isApk = device.dl.cur === 0 && device.dl.total === 0;
                      const label = isApk
                        ? '📦 APK 업데이트'
                        : `📥 미디어 (${device.dl.cur}/${device.dl.total})`;
                      const barColor = isApk ? '#8b5cf6' : '#3b82f6';
                      const bgColor  = isApk ? 'rgba(139,92,246,0.08)' : 'rgba(59,130,246,0.07)';
                      const bdColor  = isApk ? 'rgba(139,92,246,0.25)' : 'rgba(59,130,246,0.2)';
                      return (
                        <div style={{ margin: '6px 0 2px', padding: '5px 8px', background: bgColor, borderRadius: 6, border: `1px solid ${bdColor}` }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.65rem', color: '#64748b', marginBottom: 4 }}>
                            <span>{label}</span>
                            <span style={{ color: barColor, fontWeight: 700 }}>{device.dl.pct}%</span>
                          </div>
                          <div style={{ height: 4, background: '#1e293b', borderRadius: 2, overflow: 'hidden' }}>
                            <div style={{ height: '100%', width: `${device.dl.pct}%`, background: barColor, borderRadius: 2, transition: 'width 0.4s ease' }} />
                          </div>
                        </div>
                      );
                    })()}

                    {/* APK 업데이트 / 재부팅 버튼 */}
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
                        disabled={device.status !== 'online'}
                        title={device.status !== 'online' ? '오프라인 기기는 재부팅할 수 없습니다' : '기기 재부팅'}
                        style={{
                          fontSize: '0.7rem', padding: '3px 10px',
                          background: 'transparent',
                          border: `1px solid ${device.status !== 'online' ? '#1e293b' : 'var(--border)'}`,
                          borderRadius: '4px',
                          color: device.status !== 'online' ? '#334155' : 'var(--text-secondary)',
                          cursor: device.status !== 'online' ? 'not-allowed' : 'pointer',
                          opacity: device.status !== 'online' ? 0.4 : 1,
                        }}
                        onClick={() => {
                          if (device.status !== 'online') return;
                          if (!window.confirm(`${device.name || device.id} 기기를 재부팅할까요?`)) return;
                          apiFetch(`${SOCKET_URL}/api/devices/${device.id}/reboot`, { method: 'POST' })
                            .then(r => r.json())
                            .then(r => {
                              if (r.ok) alert('재부팅 명령을 전송했습니다.');
                              else alert(`재부팅 실패\n\n${r.error}`);
                            })
                            .catch(() => alert('서버 요청 실패'));
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
              deviceOrder={deviceOrder}
              onDeviceOrderChange={setDeviceOrder}
            />
          </div>
        )}
        {activeTab === 'settings' && (
          <SettingsTab onUnauth={onUnauth} deviceOrder={deviceOrder} />
        )}
      </main>
    </div>
  );
}

export default App;
