import { useState, useEffect, useRef } from 'react';
import { io } from 'socket.io-client';
import { SOCKET_URL } from '../config';

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

export default function DevicePreview({ groupId, deviceId, onUpdate, pcAudio = false, devVol = 8, liveSlide = null }) {
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
  useEffect(() => {
    if (!onUpdate) return;
    onUpdate({
      filename: activeItem?.media?.filename || 'No Media',
      currentTime,
      duration,
      transType: labelTransType,
      transTime,
    });
  }, [activeItem, currentTime, labelTransType, transTime, duration, onUpdate]);

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
