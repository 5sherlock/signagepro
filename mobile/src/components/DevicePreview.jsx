import { useState, useEffect, useRef } from 'react';
import { io } from 'socket.io-client';
import { SOCKET_URL, getToken } from '../config';

function computeNtpPosition(medias, epochMs) {
  const durationsMs = medias.map(m => (m.duration || 10) * 1000);
  const cycleMs = durationsMs.reduce((a, b) => a + b, 0);
  if (cycleMs <= 0) return { idx: 0, elapsed: 0, remaining: durationsMs[0] / 1000 || 10 };
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

export default function DevicePreview({ groupId, deviceId, onUpdate, pcAudio = false, devVol = 8, liveSlide = null }) {
  const [playlist, setPlaylist] = useState([]);
  const [ntpOffset, setNtpOffset] = useState(0);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [prevIndex, setPrevIndex] = useState(-1);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const prevIdxRef = useRef(-1);
  const transTimerRef = useRef(null);
  const videoElRef = useRef(null);

  useEffect(() => {
    const el = videoElRef.current;
    if (!el) return;
    const muted = !pcAudio || devVol === 0;
    el.muted = muted;
    if (!muted) el.volume = Math.max(0, Math.min(1, devVol / 15));
  }, [pcAudio, devVol]);

  useEffect(() => {
    if (!groupId) return;
    const load = () => {
      const fetchStart = Date.now();
      Promise.all([
        fetch(`${SOCKET_URL}/api/groups/${groupId}/playlist`).then(r => r.json()),
        fetch(`${SOCKET_URL}/api/time`).then(r => r.json()).catch(() => ({ epochMs: Date.now() })),
      ])
        .then(([data, { epochMs }]) => {
          const medias = (data?.medias || []).filter(
            m => !m.targetDeviceId || m.targetDeviceId === deviceId
          );
          setPlaylist(medias);
          const rtt = Date.now() - fetchStart;
          setNtpOffset(epochMs - Date.now() + rtt / 2);
          prevIdxRef.current = -1;
        })
        .catch(err => console.error('미리보기 불러오기 실패:', err));
    };
    load();
    const resyncTimer = setInterval(load, 10_000);
    const socket = io(SOCKET_URL, { auth: { token: getToken() } });
    socket.on('playlist_updated', ({ groupId: gid }) => {
      if (gid === groupId) load();
    });
    return () => {
      clearInterval(resyncTimer);
      socket.disconnect();
    };
  }, [groupId, deviceId]);

  useEffect(() => {
    if (!playlist.length) return;
    const tick = () => {
      const nowMs = Date.now() + ntpOffset;
      const { idx, elapsed } = computeNtpPosition(playlist, nowMs);
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
    tick();
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
  const labelTransType = activeItem?.transition?.toLowerCase() || 'fade';
  const isLiveSync = liveSlide && liveSlide.index > 0 && liveSlide.index <= playlist.length;

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
    inClass = `preview-slide-in-${slideDir}`;
    outClass = `preview-slide-out-${slideDir}`;
  } else if (transType === 'fade') {
    inClass = 'preview-fade';
    outClass = 'preview-fade-out';
  } else if (transType === 'dissolve') {
    inClass = 'preview-dissolve';
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
      <div style={{
        position: 'absolute', top: 4, right: 4, zIndex: 10,
        fontSize: '0.48rem', fontWeight: 700, letterSpacing: '0.04em',
        padding: '2px 5px', borderRadius: 3,
        background: isLiveSync ? 'rgba(16,185,129,0.85)' : 'rgba(99,102,241,0.75)',
        color: '#fff', pointerEvents: 'none',
      }}>
        {isLiveSync ? '● LIVE' : '◎ NTP'}
      </div>
      {isTransitioning && prevItem && (
        <div
          className={outClass}
          style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 1, '--trans-time': `${transTime}ms` }}
        >
          {renderMedia(prevItem)}
        </div>
      )}
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
