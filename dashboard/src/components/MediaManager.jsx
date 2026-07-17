import React, { useState, useEffect, useRef, useCallback } from 'react';
import { SOCKET_URL, apiFetch } from '../config';
import {
  DndContext,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
  DragOverlay,
} from '@dnd-kit/core';

/**
 * 커스텀 센서: 포인터 다운이 .device-row-v4 내부에서 시작된 경우에만 dnd-kit 드래그 활성화.
 * 라이브러리 아이템(HTML5 draggable)에서 시작된 드래그는 무시 → 충돌 방지.
 */
class DeviceRowPointerSensor extends PointerSensor {
  static activators = [
    {
      eventName: 'onPointerDown',
      handler({ nativeEvent: event }) {
        // 기본 HTML5 draggable 요소에서 시작 → dnd-kit 비활성화
        if (event.target.closest('[draggable="true"]')) return false;
        // .device-row-v4 안에서만 dnd-kit 활성화 (행 재정렬 전용)
        return !!event.target.closest('.device-row-v4');
      },
    },
  ];
}
import {
  SortableContext,
  verticalListSortingStrategy,
  useSortable,
  arrayMove,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import {
  Plus,
  Trash2,
  Upload,
  ChevronRight,
  ChevronLeft,
  Play,
  Pause,
  X,
  ChevronDown,
  Clock,
  ExternalLink,
  Save,
  Monitor,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  ArrowDown,
  GripVertical,
  RotateCcw,
  CalendarClock,
  Bookmark
} from 'lucide-react';
import './PreviewModal.css';
import './MediaManager.css';

const API = SOCKET_URL;

// ── 미디어 렌더러 ─────────────────────────────────────────
// 썸네일은 서버가 생성한 480px 축소본(/thumb)을 쓴다. 원본 풀해상도를 다수
// 동시 디코드하면 브라우저 이미지 메모리가 고갈돼 일부가 검게 렌더되기 때문.
// 서버 썸네일 생성 실패 시 원본으로 폴백(onError). + lazy/async 로딩.
export const thumbUrlFor = (path) => {
  if (!path) return '';
  if (path.startsWith('http')) return path;        // 외부(R2 등)는 그대로
  return `${API}/thumb/${path.split('/').pop()}`;  // /uploads/<file> → /thumb/<file>
};
const MediaThumb = ({ path, style = {} }) => {
  if (!path) return <div style={{ background: '#111', ...style }} />;
  const url = path.startsWith('http') ? path : `${API}${path}`;
  const isVideo = /\.(mp4|webm|mov)$/i.test(path);
  if (isVideo) return <video src={url} muted loop playsInline preload="metadata" style={{ objectFit: 'contain', ...style }} />;
  return <img src={thumbUrlFor(path)} alt="" loading="lazy" decoding="async"
    onError={(e) => { if (e.currentTarget.src !== url) e.currentTarget.src = url; }}
    style={{ objectFit: 'contain', ...style }} />;
};

// ── 트랙 효과 오버레이 계산 ─────────────────────────────────────
// 실제 적용되는 전환 효과를 트랙에 시각적으로 표현하는 그라디언트 반환
const getTrackOverlay = (transType, transTime, preWait, track) => {
  const halfTrans = transTime / 2;
  if (track === 1) {
    const barTime = preWait + transTime;
    const preWaitPct = (preWait / barTime) * 100;
    const midPct = ((preWait + halfTrans) / barTime) * 100;
    if (transType === 'fade') {
      // PRE_WAIT 동안 투명 → 전환 절반까지 검정으로 페이드 → 끝까지 검정
      return `linear-gradient(90deg, transparent 0%, transparent ${preWaitPct}%, #000 ${midPct}%, #000 100%)`;
    }
    if (transType === 'dissolve') {
      // PRE_WAIT 동안 투명 → 전환 끝까지 점진적 검정(반투명)
      return `linear-gradient(90deg, transparent 0%, transparent ${preWaitPct}%, rgba(0,0,0,0.7) 100%)`;
    }
    if (transType === 'slide') {
      // 슬라이드: 별도의 가이드 색상 없이 투명하게 처리
      return 'transparent';
    }
    return 'transparent';
  } else {
    const barTime = transTime + 1000;
    const halfTrans = transTime / 2;
    const halfPct = (halfTrans / barTime) * 100;
    const endPct = (transTime / barTime) * 100;
    if (transType === 'fade') {
      // 시작에 검정 → 전환 절반에서 페이드 인 → 끝까지 투명
      return `linear-gradient(90deg, #000 0%, #000 ${halfPct}%, transparent ${endPct}%, transparent 100%)`;
    }
    if (transType === 'dissolve') {
      // 시작 반투명 검정 → 전환 끝까지 페이드 아웃
      return `linear-gradient(90deg, rgba(0,0,0,0.7) 0%, transparent ${endPct}%, transparent 100%)`;
    }
    if (transType === 'slide') {
      return 'transparent';
    }
    return 'transparent';
  }
};

// ── 프리미어 스타일 필름스트립 (트랙 폭에 비례해 썸네일 반복) ────────────
const TrackFilmstrip = ({ item }) => {
  if (!item?.media?.path) return null;
  const url = item.media.path.startsWith('http') ? item.media.path : `${API}${item.media.path}`;
  const isVideo = /\.(mp4|webm|mov)$/i.test(item.media.path);
  const frames = Array.from({ length: 40 });
  
  return (
    <div style={{ display: 'flex', height: '100%', width: '100%', overflow: 'hidden' }}>
      {frames.map((_, i) => (
        <div key={i} className="track-frame-container">
          {isVideo ? (
            <video className="track-frame-thumb" src={url} muted preload="metadata" />
          ) : (
            <img className="track-frame-thumb" src={thumbUrlFor(item.media.path)} alt="" loading="lazy" decoding="async"
              onError={(e) => { if (e.currentTarget.src !== url) e.currentTarget.src = url; }} />
          )}
        </div>
      ))}
    </div>
  );
};

// ── 슬라이드쇼 미리보기 모달 ─────────────────────────────
// ── 슬라이드쇼 미리보기 모달 (전체 재생 목록 고도화) ─────────────────────────────
const PlaylistPreviewModal = ({ items, deviceName, onClose }) => {
  const [isPlaying, setIsPlaying] = useState(true);
  const [currentTime, setCurrentTime] = useState(0); // ms
  const lastUpdateRef = useRef(performance.now());
  
  const totalDuration = items.reduce((acc, item) => acc + (Number(item.duration || 10) * 1000), 0);

  useEffect(() => {
    let raf;
    const animate = (now) => {
      if (isPlaying) {
        const delta = now - lastUpdateRef.current;
        setCurrentTime(prev => (prev + delta) % totalDuration);
      }
      lastUpdateRef.current = now;
      raf = requestAnimationFrame(animate);
    };
    raf = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(raf);
  }, [isPlaying, totalDuration]);

  const getCurrentInfo = () => {
    let accumulated = 0;
    for (let i = 0; i < items.length; i++) {
      const itemDur = Number(items[i].duration || 10) * 1000;
      if (currentTime >= accumulated && currentTime < accumulated + itemDur) {
        return { index: i, item: items[i], accumulated };
      }
      accumulated += itemDur;
    }
    return { index: 0, item: items[0], accumulated: 0 };
  };

  const { index, item, accumulated } = getCurrentInfo();
  const nextItem = items[(index + 1) % items.length];
  const transTime = Number(item.transitionTime || 1000);
  const itemDurMs = Number(item.duration || 10) * 1000;

  // 전환 진행률 계산 (0 ~ 1)
  const remainingTime = itemDurMs - (currentTime - accumulated);
  const isTransitioning = remainingTime < transTime && items.length > 1;
  const transProgress = isTransitioning ? Math.min(1, Math.max(0, (transTime - remainingTime) / transTime)) : 0;

  // 슬라이드 방향 계산
  const slideDir = item.slideDirection?.toLowerCase() || 'right';
  const slideAxis = (slideDir === 'up' || slideDir === 'down') ? 'Y' : 'X';
  const slideSign = (slideDir === 'right' || slideDir === 'up') ? -1 : 1;
  const isSlide = isTransitioning && item.transition?.toLowerCase() === 'slide';
  const outgoingTransform = isSlide ? `translate${slideAxis}(${slideSign * transProgress * 100}%)` : 'none';
  const incomingTransform = isSlide ? `translate${slideAxis}(${-slideSign * (1 - transProgress) * 100}%)` : 'none';

  return (
    <div className="preview-modal-overlay">
      <div className="preview-modal-content playlist-preview-v4">
        <div className="preview-header">
          <div className="header-info">
            <Monitor size={16} />
            <span className="device-label">{deviceName}</span>
            <span className="playlist-label">PLAYLIST TIMELINE</span>
          </div>
          <button className="preview-close-btn" onClick={onClose}><X size={20} /></button>
        </div>

        <div className="preview-main-display">
          <div className="preview-aspect-ratio-box">
            <div className="preview-container" style={{ background: '#000' }}>
              {/* 기본 레이어: 현재 아이템 (FADE일 경우 50%까지 투명해짐) */}
              <div
                className="preview-layer"
                style={{
                  opacity: (isTransitioning && item.transition?.toLowerCase() === 'fade') 
                    ? Math.max(0, 1 - (transProgress * 2)) 
                    : 1,
                  transform: outgoingTransform
                }}
              >
                <MediaThumb path={item.media?.path || item.path} style={{ width: '100%', height: '100%', objectFit: 'fill' }} />
              </div>
              
              {/* 전환 레이어: 다음 아이템 */}
              <div
                className="preview-layer transition-layer"
                style={{
                  opacity: (isTransitioning)
                    ? (item.transition?.toLowerCase() === 'fade'
                        ? Math.max(0, (transProgress - 0.5) * 2) // 50% 이후부터 나타남
                        : item.transition?.toLowerCase() === 'slide'
                          ? 1 // SLIDE: 항상 불투명, 위치만 이동
                          : transProgress // DISSOLVE: 교차 페이드
                      )
                    : 0,
                  transform: incomingTransform,
                  zIndex: isTransitioning ? 10 : -1,
                  visibility: isTransitioning ? 'visible' : 'hidden',
                  pointerEvents: 'none',
                  transition: isTransitioning ? 'none' : 'opacity 0.2s linear'
                }}
              >
                <MediaThumb path={nextItem.media?.path || nextItem.path} style={{ width: '100%', height: '100%', objectFit: 'fill' }} />
              </div>
            </div>
          </div>
        </div>

        <div className="playlist-timeline-bar">
          {/* 타임라인 조각들 (썸네일 + 파일명 + 전환효과) */}
          <div className="timeline-track-container">
            <div className="timeline-track">
              {items.map((it, i) => {
                const isCurrent = index === i;
                const itTrans = it.transition || 'fade';
                const itTransTime = it.transitionTime || 1000;
                
                return (
                  <div 
                    key={i} 
                    className={`timeline-chunk ${isCurrent ? 'active' : ''}`}
                    style={{ width: `${((Number(it.duration || 10) * 1000) / totalDuration) * 100}%` }}
                  >
                    <div className="chunk-content">
                      <div className="chunk-thumb">
                        <MediaThumb path={it.media?.path || it.path} style={{ width: '100%', height: '100%' }} />
                      </div>
                      <div className="chunk-label">{it.media?.filename || 'Media'}</div>
                    </div>
                    <div className="chunk-transition-label">
                      {itTrans === 'none' ? 'CUT' : `${itTrans.toUpperCase()} ${(itTransTime/1000).toFixed(1)}s`}
                    </div>
                  </div>
                );
              })}
              <div className="timeline-indicator-line" style={{ left: `${(currentTime / totalDuration) * 100}%` }} />
            </div>
          </div>

          {/* 2. 하단: 독립형 슬라이드 바 (조작 전용) */}
          <div className="dedicated-seekbar-container">
            <input 
              type="range" 
              className="main-seekbar"
              min={0}
              max={totalDuration}
              step={10}
              value={currentTime}
              onMouseDown={() => setIsPlaying(false)} // 조작 시 정지
              onChange={(e) => {
                const newTime = Number(e.target.value);
                setCurrentTime(newTime);
                lastUpdateRef.current = performance.now();
              }}
            />
            <div className="seekbar-progress-fill" style={{ width: `${(currentTime / totalDuration) * 100}%` }} />
          </div>

          <div className="timeline-controls">
            <div className="controls-left"></div>

            <div className="controls-center">
              <button className="play-pause-btn" onClick={() => setIsPlaying(!isPlaying)}>
                {isPlaying ? <Pause size={24} fill="currentColor" /> : <Play size={24} fill="currentColor" />}
              </button>
              <div className="time-info">
                <span className="current">{(currentTime / 1000).toFixed(1)}s</span>
                <span className="total">/ {(totalDuration / 1000).toFixed(1)}s</span>
              </div>
            </div>

            <div className="controls-right"></div>
          </div>
        </div>
      </div>
    </div>
  );
};

// ── 전환 효과 전용 미리보기 모달 (고도화 버전) ───────────────────────────
const TransitionPreviewModal = ({ currentItem, nextItem, onChange, onClose }) => {
  const [isPlaying, setIsPlaying] = useState(true);
  const [currentTime, setCurrentTime] = useState(0); // ms
  const [transType, setTransType] = useState(currentItem.transition || 'dissolve');
  const [slideDir, setSlideDir] = useState(currentItem.slideDirection || 'right');
  const [timeStr, setTimeStr] = useState(String(currentItem.transitionTime !== undefined ? currentItem.transitionTime : 1000));
  const [localTransTime, setLocalTransTime] = useState(currentItem.transitionTime !== undefined ? currentItem.transitionTime : 1000);
  const transTime = localTransTime;

  const PRE_WAIT = 1000; // 전환 전 1초 대기
  const TOTAL_TIME = PRE_WAIT + transTime + 1000; // 총 3초 내외
  const lastUpdateRef = useRef(performance.now());

  const handleTransChange = (t) => {
    setTransType(t);
    if (onChange) onChange({ transition: t });
  };

  const handleSlideDir = (dir) => {
    setSlideDir(dir);
    if (onChange) onChange({ slideDirection: dir });
  };

  const handleTimeChange = (val) => {
    setTimeStr(val);
    const parsed = parseInt(val, 10);
    if (!isNaN(parsed) && parsed >= 0) {
      setLocalTransTime(parsed);
      if (onChange) onChange({ transitionTime: parsed });
    }
  };

  useEffect(() => {
    let raf;
    const animate = (now) => {
      if (isPlaying) {
        const delta = now - lastUpdateRef.current;
        setCurrentTime(prev => (prev + delta) % TOTAL_TIME);
      }
      lastUpdateRef.current = now;
      raf = requestAnimationFrame(animate);
    };
    
    raf = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(raf);
  }, [isPlaying, TOTAL_TIME]);

  const renderMedia = (item) => {
    if (!item?.media?.path) return <div className="preview-media-empty">미디어 없음</div>;
    const isVideo = /\.(mp4|webm|mov)$/i.test(item.media.path);
    const url = item.media.path.startsWith('http') ? item.media.path : `${API}${item.media.path}`;
    // 항상 원본 비율 유지(contain)하여 이미지가 잘리지 않게 함
    const fit = 'contain';
    return isVideo ? <video src={url} autoPlay muted style={{ width: '100%', height: '100%', objectFit: fit }} /> : <img src={url} alt="" style={{ width: '100%', height: '100%', objectFit: fit }} />;
  };

  // 현재 시간에 따른 애니메이션 계산
  const getStyles = () => {
    const progress = Math.max(0, Math.min(1, (currentTime - PRE_WAIT) / transTime));

    if (transType === 'dissolve') {
      return {
        layer1: { opacity: 1 - progress, zIndex: 2 },
        layer2: { opacity: progress, zIndex: 1 }
      };
    }

    if (transType === 'fade') {
      // 페이드: 검정으로 사라졌다 나타남
      const l1Opacity = progress < 0.5 ? 1 - (progress * 2) : 0;
      const l2Opacity = progress >= 0.5 ? (progress - 0.5) * 2 : 0;
      return {
        layer1: { opacity: l1Opacity, zIndex: 2 },
        layer2: { opacity: l2Opacity, zIndex: 1 }
      };
    }
    if (transType === 'slide') {
      // 슬라이드 방향: right=다음이 오른쪽에서, left=왼쪽에서, up=아래에서, down=위에서
      const isVertical = (slideDir === 'up' || slideDir === 'down');
      const axis = isVertical ? 'Y' : 'X';
      // right/up: layer1이 음의 방향으로, layer2가 양의 위치에서 진입
      // left/down: 반대
      const dir = (slideDir === 'right' || slideDir === 'up') ? -1 : 1;
      return {
        layer1: { transform: `translate${axis}(${dir * progress * 100}%)`, zIndex: 2 },
        layer2: { transform: `translate${axis}(${-dir * (1 - progress) * 100}%)`, zIndex: 1 }
      };
    }
    return { layer1: { opacity: 1 }, layer2: { opacity: 0 } };
  };

  const styles = getStyles();

  return (
    <div className="preview-modal-overlay" onClick={onClose}>
      <div className="preview-modal transition-editor-box" onClick={e => e.stopPropagation()}>
        <div className="editor-main-view">
          <div className="transition-preview-container">
            <div className="transition-layer" style={{ ...styles.layer1, transition: 'none' }}>
              {renderMedia(currentItem)}
            </div>
            <div className="transition-layer" style={{ ...styles.layer2, transition: 'none' }}>
              {renderMedia(nextItem)}
            </div>
          </div>
          
          <button className="preview-close-fixed" onClick={onClose}><X size={24} /></button>
        </div>

        {/* 하단 컨트롤 및 타임라인 */}
        <div className="editor-controls">
          <div className="control-bar">
            <button className="ctrl-btn" onClick={() => setIsPlaying(!isPlaying)}>
              {isPlaying ? <Pause size={20} fill="currentColor" /> : <Play size={20} fill="currentColor" />}
            </button>
            
            <div className="trans-selector">
              <span className="label">EFFECT</span>
              <div className="trans-buttons">
                {['fade', 'slide', 'dissolve'].map(t => (
                  <button
                    key={t}
                    className={`trans-opt ${transType === t ? 'active' : ''}`}
                    onClick={() => handleTransChange(t)}
                  >
                    {t.toUpperCase()}
                  </button>
                ))}
              </div>
            </div>

            {transType === 'slide' && (
              <div className="slide-dir-selector">
                <span className="label">DIRECTION</span>
                <div className="slide-dir-buttons">
                  {[
                    { dir: 'right', Icon: ArrowLeft, title: '오른쪽 → 왼쪽 (다음이 오른쪽에서)' },
                    { dir: 'left',  Icon: ArrowRight, title: '왼쪽 → 오른쪽 (다음이 왼쪽에서)' },
                    { dir: 'up',    Icon: ArrowUp, title: '아래 → 위 (다음이 아래에서)' },
                    { dir: 'down',  Icon: ArrowDown, title: '위 → 아래 (다음이 위에서)' },
                  ].map(({ dir, Icon, title }) => (
                    <button
                      key={dir}
                      className={`slide-dir-btn ${slideDir === dir ? 'active' : ''}`}
                      onClick={() => handleSlideDir(dir)}
                      title={title}
                    >
                      <Icon size={16} />
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div className="trans-duration-edit">
              <span className="label">DURATION</span>
              <div className="duration-input-wrapper">
                <input 
                  type="number" 
                  value={timeStr} 
                  onChange={e => handleTimeChange(e.target.value)}
                  step={100}
                  min={0}
                />
                <span className="unit">ms</span>
              </div>
            </div>
            
            <div className="time-display">
              {(currentTime / 1000).toFixed(2)}s / {(TOTAL_TIME / 1000).toFixed(2)}s
            </div>
          </div>

          <div className="dedicated-seekbar-container" style={{ marginTop: 20, marginBottom: 12 }}>
            <input 
              type="range" 
              className="main-seekbar"
              min={0}
              max={TOTAL_TIME}
              step={10}
              value={currentTime}
              onMouseDown={() => setIsPlaying(false)}
              onChange={(e) => {
                const newTime = Number(e.target.value);
                setCurrentTime(newTime);
                lastUpdateRef.current = performance.now();
              }}
            />
            <div className="seekbar-progress-fill" style={{ width: `${(currentTime / TOTAL_TIME) * 100}%` }} />
          </div>

          <div className="timeline-container">
            <div className="track-label">TRACK 1</div>
            <div className="track-lane">
              <div
                className="track-bar track-1"
                style={{ width: `${(PRE_WAIT + transTime) / TOTAL_TIME * 100}%` }}
              >
                <TrackFilmstrip item={currentItem} />
                <div
                  className="track-tint"
                  style={{ background: getTrackOverlay(transType, transTime, PRE_WAIT, 1) }}
                />
                {currentItem?.media?.filename && (
                  <span className="track-thumb-label">{currentItem.media.filename}</span>
                )}
              </div>
            </div>

            <div className="track-label">TRACK 2</div>
            <div className="track-lane">
              <div
                className="track-bar track-2"
                style={{
                  left: `${PRE_WAIT / TOTAL_TIME * 100}%`,
                  width: `${(transTime + 1000) / TOTAL_TIME * 100}%`
                }}
              >
                <TrackFilmstrip item={nextItem} />
                <div
                  className="track-tint"
                  style={{ background: getTrackOverlay(transType, transTime, PRE_WAIT, 2) }}
                />
                {nextItem?.media?.filename && (
                  <span className="track-thumb-label">{nextItem.media.filename}</span>
                )}
              </div>
            </div>

            {/* 재생 헤드 */}
            <div className="playhead" style={{ left: `${(currentTime / TOTAL_TIME) * 100}%` }}></div>
          </div>
        </div>
      </div>
    </div>
  );
};

// ── [V4] 고정형 미디어 아이템 ─────────────────────────────
const MediaItemV4 = ({ item, onRemove, onChange }) => {
  const { media, duration = 10 } = item;
  return (
    <div className="media-card-v4">
      <div className="media-card-grip" title="드래그하여 순서 변경">
        <GripVertical size={14} color="#475569" />
      </div>
      <div className="media-card-thumb">
        <MediaThumb path={media?.path} style={{ width: '100%', height: '100%' }} />
        <button className="media-card-del" onClick={onRemove} title="삭제">
          <Trash2 size={12} />
        </button>
      </div>
      <div className="media-card-name" title={media?.filename}>{media?.filename}</div>
      <div className="media-card-duration">
        <Clock size={12} color="#3b82f6" />
        <input
          className="duration-input"
          type="number"
          value={duration}
          min={1}
          onChange={e => onChange({ duration: Number(e.target.value) })}
        />
        <span>sec</span>
      </div>
    </div>
  );
};

// ── [V4] 전환 효과 브릿지 ───────────────────────────────────
const TransitionBridgeV4 = ({ item, isLoop, onChange, onPreview }) => {
  const { transitionTime = 1000 } = item;
  const isCut = (item.transition || 'fade') === 'none';
  return (
    <div className="transition-bridge-v4">
      <div className="bridge-line" />
      <div className="bridge-box">
        <select
          className="mini-select"
          value={item.transition || 'fade'}
          onChange={e => {
            const val = e.target.value;
            onChange({ transition: val, ...(val === 'none' ? { transitionTime: 0 } : {}) });
          }}
        >
          <option value="fade">FADE</option>
          <option value="slide">SLIDE</option>
          <option value="dissolve">DISSOLVE</option>
          <option value="none">CUT</option>
        </select>
        {!isCut && (
          <div className="bridge-time-row">
            <input type="number" value={transitionTime} min={0} step={100} onChange={e => onChange({ transitionTime: Number(e.target.value) })} />
            <span>ms</span>
          </div>
        )}
        <button className="bridge-preview-btn" onClick={onPreview}>미리보기</button>
      </div>
      <div className="bridge-line" />
      {isLoop && <span className="loop-badge">LOOP ↩</span>}
    </div>
  );
};

// ── [V4] 기기 고정형 행 (dnd-kit sortable) ───────────────────
const DeviceRowV4 = ({ device, items, changedSlots = [], isDirty, onDrop, onRemoveItem, onChangeItem, onDeleteDevice, onPreview, onTransitionPreview, onReorder, libDragOver = false, dropTarget = null }) => {
  const [reorderDragIdx, setReorderDragIdx] = useState(null);
  const [reorderOverIdx, setReorderOverIdx] = useState(null);
  const [hoverChange, setHoverChange] = useState(null); // 교체예정 슬롯 호버 → 새 이미지 비교 { idx, x, y }
  const timelineRef = useRef(null);

  useEffect(() => {
    const el = timelineRef.current;
    if (!el) return;
    const onWheel = e => {
      // 가로 오버플로(스크롤 필요)가 있을 때만 휠을 가로채 가로 스크롤로 전환.
      // 작게보기(wrap)처럼 가로 오버플로가 없으면 기본 세로 스크롤 허용.
      if (el.scrollWidth <= el.clientWidth) return;
      e.preventDefault();
      el.scrollLeft += e.deltaY;
    };
    el.addEventListener('wheel', onWheel, { passive: false });
    return () => el.removeEventListener('wheel', onWheel);
  }, []);

  const isReorderDrag = (e) => e.dataTransfer.types.includes('timeline-index');

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: device.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : 1,
  };

  return (
    <div ref={setNodeRef} style={style} className="device-row-v4">
      <div className={`device-card-v4 ${items.length > 0 ? 'has-media' : ''}`}>
        <Trash2
          size={14}
          className="device-card-del-icon"
          onClick={() => onDeleteDevice(device.id, device.name)}
        />
        {/* 드래그 핸들 — 좌상단 고정 */}
        <div className="row-drag-handle" {...attributes} {...listeners} title="드래그하여 순서 변경">
          <GripVertical size={15} />
        </div>
        <div className="device-card-header">
          <span className={`device-dot ${device.status === 'online' ? 'online' : 'offline'}`} />
          <span className="device-name">{device.name}</span>
        </div>
        {isDirty && <div className="device-card-pending">(배포 대기)</div>}

        <div style={{ height: '10px' }}></div>

        {items.length > 0 && (
          <button className="device-full-preview-btn" onClick={onPreview}>
            <ExternalLink size={12} style={{ marginRight: 4 }} />
            전체 미리보기
          </button>
        )}
      </div>

      <div
        ref={timelineRef}
        className={`device-timeline-v4 ${libDragOver ? 'drag-over' : ''}`}
        data-device-id={device.id}
      >
        {items.length === 0 && <div className="timeline-empty">미디어를 드래그하여 추가하세요</div>}
        {items.map((item, idx) => {
          const isLast = idx === items.length - 1;
          const nextItem = items.length > 1 ? items[(idx + 1) % items.length] : null;
          const isDraggedOver = reorderOverIdx === idx && reorderDragIdx !== idx;
          const isDragging = reorderDragIdx === idx;
          const change = changedSlots.find(c => c.index === idx); // 예약으로 교체 예정 ({index, path})
          const willChange = !!change;

          return (
            <React.Fragment key={item._key || idx}>
              {dropTarget && dropTarget.mode === 'insert' && dropTarget.index === idx && <span className="mm-insert-line" />}
              <div
                className={`reorder-item-wrapper${isDragging ? ' reorder-dragging' : ''}${isDraggedOver ? ' reorder-over' : ''}${dropTarget && dropTarget.mode === 'replace' && dropTarget.index === idx ? ' lib-replace-target' : ''}`}
                style={willChange ? { position: 'relative', outline: '2px solid #a78bfa', outlineOffset: '2px', borderRadius: 8 } : { position: 'relative' }}
                onMouseEnter={(e) => { if (willChange) { const r = e.currentTarget.getBoundingClientRect(); setHoverChange({ idx, x: r.left + r.width / 2, y: r.top }); } }}
                onMouseLeave={() => setHoverChange(h => (h && h.idx === idx ? null : h))}
                onDragOver={e => {
                  if (e.dataTransfer.types.includes('device-row-id')) return;
                  e.preventDefault();
                  if (isReorderDrag(e)) {
                    // 타임라인 내 순서 변경 드래그만 여기서 처리
                    e.stopPropagation();
                    if (reorderDragIdx !== idx) {
                      setReorderOverIdx(idx);
                    }
                  }
                  // 라이브러리 드래그: stopPropagation 안 함 → 부모 device-timeline-v4가 dragOver 상태 표시
                }}
                onDragLeave={e => {
                  if (isReorderDrag(e)) e.stopPropagation();
                  if (reorderOverIdx === idx) setReorderOverIdx(null);
                }}
                onDrop={e => {
                  if (isReorderDrag(e)) {
                    // 타임라인 내 순서 변경: 여기서 처리하고 전파 중단
                    e.preventDefault();
                    e.stopPropagation();
                    const fromIdx = Number(e.dataTransfer.getData('timeline-index'));
                    if (!isNaN(fromIdx) && fromIdx !== idx) {
                      onReorder(fromIdx, idx);
                    }
                  }
                  // 라이브러리 드롭: stopPropagation 안 함 → 부모 device-timeline-v4의 onDrop이 처리
                  setReorderDragIdx(null);
                  setReorderOverIdx(null);
                }}
              >
                {willChange && hoverChange && hoverChange.idx === idx && change.path && (
                  <div style={{ position: 'fixed', left: hoverChange.x, top: hoverChange.y - 6, transform: 'translate(-50%, -100%)', zIndex: 9999, background: '#0f172a', border: '1px solid rgba(167,139,250,0.65)', borderRadius: 8, padding: 8, boxShadow: '0 10px 28px rgba(0,0,0,0.6)', pointerEvents: 'none', display: 'flex', gap: 10, alignItems: 'center' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
                      <div style={{ fontSize: '0.62rem', color: '#94a3b8', fontWeight: 700 }}>교체 전 (현재)</div>
                      {item.media?.path
                        ? <img src={`${API}/thumb/${item.media.path.split('/').pop()}`} alt="" style={{ width: 140, height: 88, objectFit: 'cover', borderRadius: 4, display: 'block', background: '#000', border: '1px solid rgba(148,163,184,0.35)' }} />
                        : <div style={{ width: 140, height: 88, borderRadius: 4, background: '#1e293b', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.66rem', color: '#94a3b8' }}>없음</div>}
                    </div>
                    <div style={{ color: '#a78bfa', fontWeight: 700, fontSize: '1.2rem' }}>→</div>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
                      <div style={{ fontSize: '0.62rem', color: '#c4b5fd', fontWeight: 700 }}>교체 후 (예약)</div>
                      <img src={`${API}/thumb/${change.path.split('/').pop()}`} alt="" style={{ width: 140, height: 88, objectFit: 'cover', borderRadius: 4, display: 'block', background: '#000', border: '1px solid rgba(167,139,250,0.55)' }} />
                    </div>
                  </div>
                )}
                {/* 드래그 핸들 — 이 영역만 잡아서 이동 */}
                <div
                  className="timeline-item-drag-handle"
                  draggable
                  title="잡고 드래그하여 순서 변경"
                  onDragStart={e => {
                    e.dataTransfer.setData('timeline-index', String(idx));
                    e.dataTransfer.effectAllowed = 'move';
                    setReorderDragIdx(idx);
                  }}
                  onDragEnd={() => {
                    setReorderDragIdx(null);
                    setReorderOverIdx(null);
                  }}
                >
                  ⠿
                </div>
                <MediaItemV4 item={item} onRemove={() => onRemoveItem(idx)} onChange={upd => onChangeItem(idx, upd)} />
              </div>
              {(nextItem || (isLast && items.length > 0)) && (
                <TransitionBridgeV4
                  item={item}
                  isLoop={isLast}
                  onChange={upd => onChangeItem(idx, upd)}
                  onPreview={() => onTransitionPreview(item, nextItem, idx)}
                />
              )}
            </React.Fragment>
          );
        })}
        {dropTarget && dropTarget.mode === 'insert' && dropTarget.index === items.length && <span className="mm-insert-line" />}
      </div>
    </div>
  );
};

// 장면 선택 커스텀 드롭다운 — 최신순. 위로 열리면 최신이 트리거에 가깝게(맨 아래), 아래로 열리면 맨 위.
const SceneSelect = ({ scenes, value, onChange, placeholder = '장면 선택…' }) => {
  const [open, setOpen] = useState(false);
  const [up, setUp] = useState(false);
  const ref = useRef(null);
  const sorted = [...scenes].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  const selected = scenes.find(s => s.id === value);
  const list = up ? [...sorted].reverse() : sorted; // 위로 열면 뒤집어 최신이 트리거 옆(아래)에
  const toggle = () => {
    if (!open && ref.current) {
      const r = ref.current.getBoundingClientRect();
      setUp((window.innerHeight - r.bottom) < 250); // 아래 공간 부족 → 위로
    }
    setOpen(o => !o);
  };
  useEffect(() => {
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    if (open) document.addEventListener('mousedown', h);
    return () => document.removeEventListener('mousedown', h);
  }, [open]);
  const fld = { background: '#0f172a', border: '1px solid rgba(148,163,184,0.3)', borderRadius: 6, color: '#e2e8f0', padding: '6px 10px', fontSize: '0.85rem' };
  return (
    <div ref={ref} style={{ position: 'relative', minWidth: 170 }}>
      <button type="button" onClick={toggle} style={{ ...fld, display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', gap: 8, cursor: 'pointer' }}>
        <span style={{ color: selected ? '#e2e8f0' : '#64748b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{selected ? selected.name : placeholder}</span>
        <ChevronDown size={14} style={{ transform: open ? 'rotate(180deg)' : 'none', flexShrink: 0, transition: 'transform 0.15s' }} />
      </button>
      {open && (
        <div style={{ position: 'absolute', left: 0, right: 0, [up ? 'bottom' : 'top']: 'calc(100% + 4px)', maxHeight: 224, overflowY: 'auto', background: '#0f172a', border: '1px solid rgba(167,139,250,0.5)', borderRadius: 6, zIndex: 200, boxShadow: '0 8px 24px rgba(0,0,0,0.55)' }}>
          {list.length === 0 && <div style={{ padding: '8px 10px', color: '#64748b', fontSize: '0.82rem' }}>저장된 장면 없음</div>}
          {list.map(s => (
            <div key={s.id} onClick={() => { onChange(s.id); setOpen(false); }}
              style={{ padding: '7px 10px', cursor: 'pointer', fontSize: '0.85rem', color: s.id === value ? '#c4b5fd' : '#e2e8f0', background: s.id === value ? 'rgba(167,139,250,0.14)' : 'transparent', whiteSpace: 'nowrap' }}
              onMouseEnter={e => e.currentTarget.style.background = 'rgba(167,139,250,0.1)'}
              onMouseLeave={e => e.currentTarget.style.background = s.id === value ? 'rgba(167,139,250,0.14)' : 'transparent'}>
              {s.name}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// ── 콘텐츠 예약 모달 (장면 저장 + 시각 교체 예약) ─────────────────
// 장면 = 현재 배치 스냅샷. 예약 = "이 시각에 이 장면으로 교체" → 서버 스케줄러가 자동 적용(무인).
const ContentScheduleModal = ({ groupId, getCurrentItems, onClose, onApplied }) => {
  const [scenes, setScenes] = useState([]);
  const [scheds, setScheds] = useState([]);
  const [sceneName, setSceneName] = useState('');
  const [schedSceneId, setSchedSceneId] = useState('');
  const [schedAt, setSchedAt] = useState('');
  const [editingId, setEditingId] = useState(null); // 수정 중인 예약 id
  const [showAllScenes, setShowAllScenes] = useState(false); // 장면 5개 초과 펼치기
  const [showAllPast, setShowAllPast] = useState(false);     // 지난 예약 5개 초과 펼치기
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  const load = useCallback(async () => {
    try {
      const [r1, r2] = await Promise.all([
        apiFetch(`${SOCKET_URL}/api/groups/${groupId}/scenes`),
        apiFetch(`${SOCKET_URL}/api/groups/${groupId}/content-schedules`),
      ]);
      setScenes(await r1.json());
      setScheds(await r2.json());
    } catch { setMsg('목록 로드 실패'); }
  }, [groupId]);
  useEffect(() => { if (groupId) load(); }, [groupId, load]);

  const saveScene = async () => {
    const name = sceneName.trim();
    if (!name) { setMsg('장면 이름을 입력하세요'); return; }
    setBusy(true); setMsg('');
    try {
      const items = getCurrentItems ? getCurrentItems() : undefined;
      const r = await apiFetch(`${SOCKET_URL}/api/groups/${groupId}/scenes`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, items }),
      });
      const d = await r.json();
      if (d.success) { setSceneName(''); setMsg(`장면 "${name}" 저장됨 (${d.scene.itemCount}개)`); await load(); }
      else setMsg(d.error || '저장 실패');
    } catch { setMsg('저장 실패'); }
    finally { setBusy(false); }
  };

  const delScene = async (id) => {
    if (!window.confirm('이 장면을 삭제할까요? (연결된 예약도 함께 삭제됩니다)')) return;
    setBusy(true);
    try { await apiFetch(`${SOCKET_URL}/api/scenes/${id}`, { method: 'DELETE' }); await load(); }
    finally { setBusy(false); }
  };

  const applyNow = async (id, name) => {
    if (!window.confirm(`지금 즉시 "${name}" 장면으로 교체할까요? (전 기기 반영)`)) return;
    setBusy(true);
    try {
      await apiFetch(`${SOCKET_URL}/api/scenes/${id}/apply`, { method: 'POST' });
      setMsg(`"${name}" 즉시 적용됨`); onApplied && onApplied();
    } finally { setBusy(false); }
  };

  const toLocalInput = (iso) => { try { const d = new Date(iso); return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16); } catch { return ''; } };
  const startEdit = (s) => { setEditingId(s.id); setSchedSceneId(s.sceneId); setSchedAt(toLocalInput(s.switchAt)); setMsg('예약 수정 중 — 시각/장면 바꾼 뒤 저장'); };
  const cancelEdit = () => { setEditingId(null); setSchedSceneId(''); setSchedAt(''); setMsg(''); };

  const addSched = async () => {
    if (!schedSceneId) { setMsg('교체할 장면을 선택하세요'); return; }
    if (!schedAt) { setMsg('교체 시각을 선택하세요'); return; }
    setBusy(true); setMsg('');
    try {
      const switchAt = new Date(schedAt).toISOString();
      const url = editingId ? `${SOCKET_URL}/api/content-schedules/${editingId}` : `${SOCKET_URL}/api/groups/${groupId}/content-schedules`;
      const r = await apiFetch(url, {
        method: editingId ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sceneId: schedSceneId, switchAt }),
      });
      const d = await r.json();
      if (d.success) { setEditingId(null); setSchedAt(''); setSchedSceneId(''); setMsg(editingId ? '예약 수정됨' : '예약 추가됨'); await load(); }
      else setMsg(d.error || '예약 실패');
    } catch { setMsg('예약 실패'); }
    finally { setBusy(false); }
  };

  const delSched = async (id) => {
    setBusy(true);
    try { await apiFetch(`${SOCKET_URL}/api/content-schedules/${id}`, { method: 'DELETE' }); await load(); }
    finally { setBusy(false); }
  };

  const fmt = (iso) => { try { return new Date(iso).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }); } catch { return iso; } };
  const now = Date.now();
  const box = { background: '#1e293b', border: '1px solid rgba(148,163,184,0.2)', borderRadius: 8, padding: 12, marginBottom: 12 };
  const inp = { background: '#0f172a', border: '1px solid rgba(148,163,184,0.3)', borderRadius: 6, color: '#e2e8f0', padding: '6px 10px', fontSize: '0.85rem', colorScheme: 'dark' };
  const btn = (c) => ({ display: 'inline-flex', alignItems: 'center', gap: 5, padding: '6px 12px', borderRadius: 6, border: `1px solid ${c}55`, background: `${c}22`, color: c, cursor: 'pointer', fontSize: '0.8rem', whiteSpace: 'nowrap' });

  return (
    <div onClick={onClose} style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div onClick={e => e.stopPropagation()} style={{ background: '#0f172a', border: '1px solid rgba(148,163,184,0.25)', borderRadius: 12, width: 620, maxWidth: '92vw', maxHeight: '88vh', overflowY: 'auto', padding: 20, color: '#e2e8f0' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <h3 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 8, fontSize: '1.05rem' }}><CalendarClock size={20} color="#a78bfa" /> 콘텐츠 예약 교체</h3>
          <X size={20} style={{ cursor: 'pointer' }} onClick={onClose} />
        </div>
        <p style={{ fontSize: '0.78rem', color: '#94a3b8', marginTop: 0 }}>지정한 시각이 되면 <b>서버가 자동으로</b> 해당 장면으로 교체합니다(대시보드 꺼놔도 됨). 다음 예약 전까지 계속 유지됩니다.</p>

        {/* 1) 장면 저장 */}
        <div style={box}>
          <div style={{ fontWeight: 600, marginBottom: 4, fontSize: '0.9rem' }}>① 타임라인 배치를 장면으로 저장</div>
          <div style={{ fontSize: '0.74rem', color: '#94a3b8', marginBottom: 8, lineHeight: 1.5 }}>
            먼저 <b>타임라인을 원하는 배치(예: 교체할 새 이미지)로 편집</b>한 뒤 저장하세요.
            장면 저장은 <b style={{ color: '#34d399' }}>라이브 STB를 바꾸지 않습니다</b> — 예약 시각에만 교체됩니다.
            (지금 즉시 라이브 반영은 닫고 "변경사항 저장 및 배포")
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <input style={{ ...inp, flex: 1 }} placeholder="장면 이름 (예: 야간메뉴, 여름프로모션)" value={sceneName} onChange={e => setSceneName(e.target.value)} onKeyDown={e => e.key === 'Enter' && saveScene()} />
            <button style={btn('#10b981')} onClick={saveScene} disabled={busy}><Bookmark size={15} /> 현재 배치 저장</button>
          </div>
        </div>

        {/* 2) 저장된 장면 (최신순, 최근 5개 + 더보기) */}
        <div style={box}>
          {(() => {
            const sorted = [...scenes].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
            const vis = showAllScenes ? sorted : sorted.slice(0, 5);
            return (<>
              <div style={{ fontWeight: 600, marginBottom: 8, fontSize: '0.9rem' }}>② 저장된 장면 ({scenes.length})</div>
              {scenes.length === 0 ? <div style={{ color: '#64748b', fontSize: '0.82rem' }}>아직 없음 — 위에서 현재 배치를 저장하세요.</div> :
                <div style={{ maxHeight: 230, overflowY: 'auto' }}>
                  {vis.map(s => (
                    <div key={s.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid rgba(148,163,184,0.1)' }}>
                      <span style={{ fontSize: '0.85rem' }}>{s.name} <span style={{ color: '#64748b' }}>· {s.itemCount}개</span></span>
                      <span style={{ display: 'flex', gap: 6 }}>
                        <button style={btn('#3b82f6')} onClick={() => applyNow(s.id, s.name)} disabled={busy}><Play size={13} /> 즉시 적용</button>
                        <button style={btn('#f87171')} onClick={() => delScene(s.id)} disabled={busy}><Trash2 size={13} /></button>
                      </span>
                    </div>
                  ))}
                  {sorted.length > 5 && (
                    <button onClick={() => setShowAllScenes(v => !v)} style={{ marginTop: 6, background: 'transparent', border: 'none', color: '#94a3b8', fontSize: '0.78rem', cursor: 'pointer' }}>
                      {showAllScenes ? '접기' : `더 보기 (전체 ${sorted.length})`}
                    </button>
                  )}
                </div>}
            </>);
          })()}
        </div>

        {/* 3) 예약 추가/수정 */}
        <div style={box}>
          <div style={{ fontWeight: 600, marginBottom: 8, fontSize: '0.9rem' }}>③ {editingId ? '예약 수정' : '교체 예약 추가'}</div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
            <SceneSelect scenes={scenes} value={schedSceneId} onChange={setSchedSceneId} />
            <input style={inp} type="datetime-local" value={schedAt} onChange={e => setSchedAt(e.target.value)} />
            <button style={btn('#a78bfa')} onClick={addSched} disabled={busy}><Plus size={15} /> {editingId ? '수정 저장' : '예약'}</button>
            {editingId && <button style={btn('#94a3b8')} onClick={cancelEdit} disabled={busy}>취소</button>}
          </div>
        </div>

        {/* 4) 예약 목록 — 미래(대기) 전부 + 지난(적용됨) 최근 5개 */}
        <div style={box}>
          {(() => {
            const isPast = (s) => s.applied || !s.switchAt || new Date(s.switchAt).getTime() <= now;
            const sortDesc = (a, b) => new Date(b.createdAt) - new Date(a.createdAt);
            const future = scheds.filter(s => !isPast(s)).sort(sortDesc);
            const past = scheds.filter(isPast).sort(sortDesc);
            const visPast = showAllPast ? past : past.slice(0, 5);
            const row = (s) => {
              const p = isPast(s);
              return (
                <div key={s.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid rgba(148,163,184,0.1)', opacity: p ? 0.55 : 1, background: editingId === s.id ? 'rgba(167,139,250,0.12)' : 'transparent', borderRadius: editingId === s.id ? 4 : 0 }}>
                  <span style={{ fontSize: '0.85rem' }}>
                    <span style={{ color: p ? '#64748b' : '#a78bfa' }}>{fmt(s.switchAt)}</span>
                    {' → '}<b>{s.scene?.name || '(삭제된 장면)'}</b>
                    {s.applied && <span style={{ color: '#10b981', marginLeft: 6, fontSize: '0.72rem' }}>✓적용됨</span>}
                  </span>
                  <span style={{ display: 'flex', gap: 6 }}>
                    {!p && <button style={btn('#a78bfa')} onClick={() => startEdit(s)} disabled={busy}>수정</button>}
                    <button style={btn('#f87171')} onClick={() => delSched(s.id)} disabled={busy}><Trash2 size={13} /></button>
                  </span>
                </div>
              );
            };
            return (<>
              <div style={{ fontWeight: 600, marginBottom: 8, fontSize: '0.9rem' }}>④ 예약된 교체 (대기 {future.length}{past.length ? ` · 지난 ${past.length}` : ''})</div>
              {scheds.length === 0 ? <div style={{ color: '#64748b', fontSize: '0.82rem' }}>예약 없음</div> :
                <div style={{ maxHeight: 250, overflowY: 'auto' }}>
                  {future.map(row)}
                  {future.length === 0 && <div style={{ color: '#64748b', fontSize: '0.8rem', padding: '4px 0' }}>대기 중인 예약 없음</div>}
                  {past.length > 0 && <div style={{ fontSize: '0.72rem', color: '#64748b', margin: '8px 0 2px' }}>— 지난 교체 —</div>}
                  {visPast.map(row)}
                  {past.length > 5 && (
                    <button onClick={() => setShowAllPast(v => !v)} style={{ marginTop: 4, background: 'transparent', border: 'none', color: '#94a3b8', fontSize: '0.78rem', cursor: 'pointer' }}>
                      {showAllPast ? '지난 교체 접기' : `지난 교체 더 보기 (${past.length})`}
                    </button>
                  )}
                </div>}
            </>);
          })()}
        </div>

        {msg && <div style={{ color: '#fbbf24', fontSize: '0.82rem', marginTop: 4 }}>{msg}</div>}
      </div>
    </div>
  );
};

// ── 메인 MediaManager ─────────────────────────────────────
const MediaManager = ({ stores = [], groups = [], devices = [], selectedStoreId, setSelectedStoreId, selectedZoneId, setSelectedZoneId, fetchDevices, deviceOrder = {}, onDeviceOrderChange }) => {
  const [mediaList, setMediaList] = useState([]);
  const [bulkTransition, setBulkTransition] = useState('dissolve');
  const [bulkDuration, setBulkDuration] = useState(2000);
  const [bulkSlideDuration, setBulkSlideDuration] = useState(10); // 이미지 표시 시간(초) 일괄값
  const [timelineCompact, setTimelineCompact] = useState(true); // 타임라인 작게보기(한눈에) — 기본값
  const [showArchived, setShowArchived] = useState(false); // 보관함(미사용) 표시 여부
  const [librarySmall, setLibrarySmall] = useState(false);
  const [usedMediaIds, setUsedMediaIds] = useState(new Set()); // 저장 후 사용 중인 ID
  const [archiveSort, setArchiveSort] = useState('date-desc'); // 'date-desc' | 'date-asc' | 'ext' | 'size-desc'
  const [collapsedExts, setCollapsedExts] = useState(new Set()); // 접힌 확장자 그룹 키
  const [selectedGroupId, setSelectedGroupId] = useState('');
  const [lanes, setLanes] = useState({});
  const [savedState, setSavedState] = useState({});
  const [saving, setSaving] = useState(false);
  const [showSchedModal, setShowSchedModal] = useState(false);
  const [libPreview, setLibPreview] = useState(null); // 라이브러리 더블클릭 큰 미리보기
  const [groupScheds, setGroupScheds] = useState([]);
  const [changedByDevice, setChangedByDevice] = useState({}); // 다음 예약에서 기기별 바뀌는 슬롯 → 타임라인 표시
  const fetchGroupScheds = useCallback(async () => {
    if (!selectedGroupId) { setGroupScheds([]); setChangedByDevice({}); return; }
    try {
      const [r, rp] = await Promise.all([
        apiFetch(`${API}/api/groups/${selectedGroupId}/content-schedules`),
        apiFetch(`${API}/api/content-schedules/pending`),
      ]);
      setGroupScheds(await r.json());
      const pj = await rp.json();
      setChangedByDevice((pj.nextByGroup && pj.nextByGroup[selectedGroupId] && pj.nextByGroup[selectedGroupId].changedByDevice) || {});
    } catch { setGroupScheds([]); setChangedByDevice({}); }
  }, [selectedGroupId]);
  useEffect(() => { fetchGroupScheds(); }, [fetchGroupScheds]);
  useEffect(() => { if (!showSchedModal) fetchGroupScheds(); }, [showSchedModal, fetchGroupScheds]);
  const [previewData, setPreviewData] = useState(null);
  const [transPreview, setTransPreview] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(null); // null | { name, pct }
  const [deployPanel, setDeployPanel] = useState(null); // null | 'saving' | 'deploying'
  const [undoInfo, setUndoInfo] = useState({ available: false, mode: null, deployedAt: null }); // 되돌리기/다시원복
  // 배포한 그룹만 되돌리기 버튼 노출 + 안 누르면 유지(탭 세션 동안). 새 탭/새 세션에선 숨김.
  const [activeGroups, setActiveGroups] = useState(() => {
    try { return new Set(JSON.parse(sessionStorage.getItem('SIGNAGE_UNDO_ACTIVE_GROUPS') || '[]')); } catch { return new Set(); }
  });
  useEffect(() => {
    try { sessionStorage.setItem('SIGNAGE_UNDO_ACTIVE_GROUPS', JSON.stringify([...activeGroups])); } catch {}
  }, [activeGroups]);
  const markGroupActive = (gid) => setActiveGroups((prev) => { const n = new Set(prev); n.add(gid); return n; });
  const [reverting, setReverting] = useState(false);
  const deployHasSeenDl = useRef(false); // 기기 다운로드가 한 번이라도 감지됐는지
  const fileRef = useRef();
  const groupDevicesRef = useRef([]); // fetchPlaylist 스테일 클로저 방지용 ref
  const libraryListRef = useRef(null);

  // 라이브러리 → 타임라인 포인터 드래그 (crxMouse 제스처 충돌 방지)
  const libDragMediaRef = useRef(null);   // 드래그 중인 미디어 객체
  const handleDropRef = useRef(null);     // handleDrop 최신 참조
  const [libDragPos, setLibDragPos] = useState(null);   // { x, y } 드래그 중 좌표
  const [libDragOverId, setLibDragOverId] = useState(null); // 현재 hover 중인 deviceId
  const [libDropTarget, setLibDropTarget] = useState(null); // { deviceId, mode:'replace'|'insert', index } — 드롭 인디케이터

  const storeGroups = groups.filter(g => g.storeId === selectedStoreId);
  const rawGroupDevices = devices.filter(d => d.groupId === selectedGroupId);

  // 그룹별 저장된 순서 적용
  const groupOrder = deviceOrder[selectedGroupId] || [];
  const groupDevices = groupOrder.length > 0
    ? [
        ...groupOrder.map(id => rawGroupDevices.find(d => d.id === id)).filter(Boolean),
        ...rawGroupDevices.filter(d => !groupOrder.includes(d.id)),
      ]
    : rawGroupDevices;

  // 렌더마다 최신 groupDevices를 ref에 동기화 — fetchPlaylist 스테일 클로저 방지
  groupDevicesRef.current = groupDevices;

  // 사업장 변경 시 편성 초기화 (그룹 선택은 아래 동기화 이펙트가 처리)
  useEffect(() => {
    setLanes({});
    setSavedState({});
  }, [selectedStoreId]);

  // 관제(왼쪽 메뉴 상단)와 구역 선택 공유: 전역 selectedZoneId → 로컬 selectedGroupId 반영.
  // 관제가 '전체(all)'/무효 구역이면 편성엔 특정 구역이 필요하므로 첫 구역으로 대체(전역은 안 건드림).
  useEffect(() => {
    const shared = (selectedZoneId && selectedZoneId !== 'all' && storeGroups.some(g => g.id === selectedZoneId))
      ? selectedZoneId : null;
    if (shared) {
      if (selectedGroupId !== shared) setSelectedGroupId(shared);
    } else if (storeGroups.length > 0 && (!selectedGroupId || !storeGroups.some(g => g.id === selectedGroupId))) {
      setSelectedGroupId(storeGroups[0].id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedZoneId, storeGroups.map(g => g.id).join(','), selectedStoreId]);

  const fetchMedia = useCallback(async () => {
    if (!selectedStoreId) return;
    try {
      const res = await apiFetch(`${API}/api/media?storeId=${selectedStoreId}`);
      setMediaList(await res.json());
    } catch (e) { console.error(e); }
  }, [selectedStoreId]);

  const fetchPlaylist = useCallback(async () => {
    const devs = groupDevicesRef.current;
    if (!selectedGroupId || devs.length === 0) return;
    try {
      const res = await apiFetch(`${API}/api/groups/${selectedGroupId}/playlist`);
      const data = await res.json();
      const medias = data.medias || [];
      const newLanes = {};
      devs.forEach(d => { newLanes[d.id] = []; });
      medias.forEach((pm, idx) => {
        const item = { ...pm, _key: `${pm.mediaId}-${idx}` };
        if (!pm.targetDeviceId) {
          // targetDeviceId 없는 아이템 = 그룹 전체 공통 아이템
          devs.forEach(d => {
            newLanes[d.id].push({ ...item, _key: `${pm.mediaId}-${idx}-${d.id}` });
          });
        } else if (newLanes[pm.targetDeviceId] !== undefined) {
          newLanes[pm.targetDeviceId].push(item);
        }
        // else: targetDeviceId가 현재 그룹에 없는 기기 → 무시 (오염 방지)
      });
      setLanes(newLanes);
      setSavedState(JSON.parse(JSON.stringify(newLanes)));
      // 플레이리스트에 배정된 미디어 ID → 라이브러리에서 숨김 처리
      setUsedMediaIds(new Set(medias.map(pm => pm.mediaId)));
    } catch (e) { console.error(e); }
  }, [selectedGroupId]);

  // 직전 배포 되돌리기 상태 조회
  const fetchUndo = useCallback(async () => {
    if (!selectedGroupId) { setUndoInfo({ available: false, mode: null, deployedAt: null }); return; }
    try {
      const res = await apiFetch(`${API}/api/groups/${selectedGroupId}/playlist/undo`);
      const d = await res.json();
      setUndoInfo({ available: !!d.available, mode: d.mode || null, deployedAt: d.deployedAt || null });
    } catch { setUndoInfo({ available: false, mode: null, deployedAt: null }); }
  }, [selectedGroupId]);

  useEffect(() => { fetchUndo(); }, [fetchUndo]);

  // 직전 배포 되돌리기 실행
  const handleRevert = async () => {
    if (!selectedGroupId || !undoInfo.available || reverting) return;
    const msg = undoInfo.mode === 'redo'
      ? '되돌리기를 취소하고 방금 배포한 상태로 다시 원복합니다.\n현재 그룹의 기기들이 해당 콘텐츠를 다시 받습니다. 진행할까요?'
      : '직전 배포를 되돌려 이전 재생목록으로 복구합니다.\n현재 그룹의 기기들이 이전 콘텐츠를 다시 받습니다. 진행할까요?';
    if (!window.confirm(msg)) return;
    setReverting(true);
    try {
      const res = await apiFetch(`${API}/api/groups/${selectedGroupId}/playlist/revert`, { method: 'POST' });
      if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.error || '되돌리기 실패'); }
      await fetchPlaylist();   // 복구된 상태로 편집기 동기화
      await fetchUndo();
      markGroupActive(selectedGroupId); // 되돌리기/다시원복 토글 유지
      setDeployPanel('deploying'); // 기기 재다운로드 단계 표시
    } catch (e) {
      alert(e.message || '되돌리기 실패');
      fetchUndo(); // 만료/없음(404)이면 상태 갱신 → 버튼 자동 숨김
    } finally { setReverting(false); }
  };

  useEffect(() => { fetchMedia(); }, [fetchMedia]);

  useEffect(() => {
    const el = libraryListRef.current;
    if (!el) return;
    const onWheel = e => { e.preventDefault(); el.scrollTop += e.deltaY; };
    el.addEventListener('wheel', onWheel, { passive: false });
    return () => el.removeEventListener('wheel', onWheel);
  }, [showArchived]);

  // groupDevices.length 가 0→N 으로 바뀔 때 (devices 로딩 완료 후) 재fetch
  useEffect(() => {
    if (selectedGroupId && groupDevices.length > 0) fetchPlaylist();
    else { setLanes({}); setSavedState({}); }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedGroupId, groupDevices.length]);

  // 배포 패널 자동 닫기: 기기 dl 감지 후 모두 완료되면 2초 후 닫음
  useEffect(() => {
    if (deployPanel !== 'deploying') { deployHasSeenDl.current = false; return; }
    const anyDl = groupDevices.some(gd => devices.find(x => x.id === gd.id)?.dl != null);
    if (anyDl) { deployHasSeenDl.current = true; return; }
    if (!deployHasSeenDl.current) return; // 아직 다운로드 시작 전
    const t = setTimeout(() => setDeployPanel(null), 2000);
    return () => clearTimeout(t);
  }, [deployPanel, devices, groupDevices]);

  // 15초 안에 dl 감지 없으면 캐시된 파일로 간주하고 자동 닫음
  useEffect(() => {
    if (deployPanel !== 'deploying') return;
    const t = setTimeout(() => { if (!deployHasSeenDl.current) setDeployPanel(null); }, 15000);
    return () => clearTimeout(t);
  }, [deployPanel]);

  const handleDrop = (deviceId, media, target = null) => {
    setLanes(prev => {
      const updated = { ...prev };
      const current = [...(updated[deviceId] || [])];
      if (target && target.mode === 'replace' && target.index >= 0 && target.index < current.length) {
        // 제자리 교체 — 그 칸의 표시시간/전환효과는 유지, 이미지만 교체
        const old = current[target.index];
        current[target.index] = { ...old, mediaId: media.id, media, _key: `${media.id}-${Date.now()}` };
      } else {
        const newItem = { mediaId: media.id, media, duration: 10, transition: 'dissolve', transitionTime: 2000, slideDirection: 'right', _key: `${media.id}-${Date.now()}` };
        const insertIdx = target && target.mode === 'insert' ? target.index : null;
        if (insertIdx !== null && insertIdx >= 0 && insertIdx <= current.length) {
          current.splice(insertIdx, 0, newItem);
        } else {
          current.push(newItem);
        }
      }
      updated[deviceId] = current;
      return updated;
    });
  };
  // 항상 최신 handleDrop 참조 유지
  handleDropRef.current = handleDrop;

  // 드롭 존 계산 (기하 기반 — 카드 사이 간격에서도 정확) :
  //  카드 세로범위 안 + 가로 25~75% = 교체 / 그 외 = 커서 X 기준 가장 가까운 경계에 삽입
  const computeDropTarget = (els, clientX, clientY) => {
    const tl = els.find(el => el.dataset?.deviceId);
    if (!tl) return null;
    const deviceId = tl.dataset.deviceId;
    const wrappers = Array.from(tl.querySelectorAll('.reorder-item-wrapper'));
    if (wrappers.length === 0) return { deviceId, mode: 'insert', index: 0 };
    // 1) 어떤 카드의 가운데(세로범위 내 + 가로 25~75%)에 있으면 교체
    for (let i = 0; i < wrappers.length; i++) {
      const r = wrappers[i].getBoundingClientRect();
      if (clientY >= r.top && clientY <= r.bottom) {
        const frac = (clientX - r.left) / r.width;
        if (frac >= 0.25 && frac <= 0.75) return { deviceId, mode: 'replace', index: i };
      }
    }
    // 2) 그 외(가장자리/카드 사이/빈 영역) → 커서 X가 어느 카드 중심보다 앞인지로 삽입 위치 결정
    let index = wrappers.length;
    for (let i = 0; i < wrappers.length; i++) {
      const r = wrappers[i].getBoundingClientRect();
      if (clientX < r.left + r.width / 2) { index = i; break; }
    }
    return { deviceId, mode: 'insert', index };
  };

  // 포인터 이벤트 기반 라이브러리 드래그 (HTML5 draggable 대체 — crxMouse 충돌 방지)
  useEffect(() => {
    if (!libDragPos) return;
    const handleMove = (e) => {
      setLibDragPos({ x: e.clientX, y: e.clientY });
      const els = document.elementsFromPoint(e.clientX, e.clientY);
      const target = computeDropTarget(els, e.clientX, e.clientY);
      setLibDragOverId(target ? target.deviceId : null);
      setLibDropTarget(target);
    };
    const handleUp = (e) => {
      const els = document.elementsFromPoint(e.clientX, e.clientY);
      const target = computeDropTarget(els, e.clientX, e.clientY);
      if (target && libDragMediaRef.current) {
        handleDropRef.current(target.deviceId, libDragMediaRef.current, target);
      }
      libDragMediaRef.current = null;
      setLibDragPos(null);
      setLibDragOverId(null);
      setLibDropTarget(null);
    };
    window.addEventListener('pointermove', handleMove);
    window.addEventListener('pointerup', handleUp);
    return () => {
      window.removeEventListener('pointermove', handleMove);
      window.removeEventListener('pointerup', handleUp);
    };
  }, [!!libDragPos]);

  const handleRemoveItem = (deviceId, idx) => {
    setLanes(prev => {
      const updated = { ...prev };
      const l = [...(updated[deviceId] || [])]; l.splice(idx, 1); updated[deviceId] = l;
      return updated;
    });
  };

  const handleChangeItem = (deviceId, idx, updates) => {
    setLanes(prev => {
      const updated = { ...prev };
      const l = [...(updated[deviceId] || [])]; if (l[idx]) l[idx] = { ...l[idx], ...updates }; updated[deviceId] = l;
      return updated;
    });
  };

  const handleReorder = (deviceId, fromIdx, toIdx) => {
    if (fromIdx === toIdx) return;
    setLanes(prev => {
      const updated = { ...prev };
      const l = [...(updated[deviceId] || [])];
      const [removed] = l.splice(fromIdx, 1);
      l.splice(toIdx, 0, removed);
      updated[deviceId] = l;
      return updated;
    });
  };

  const handleDeleteDevice = async (deviceId, deviceName) => {
    if (!window.confirm(`'${deviceName}' 기기를 그룹에서 해제하시겠습니까?`)) return;
    try {
      await apiFetch(`${API}/api/devices/${deviceId}/group`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ groupId: null }) });
      fetchDevices?.();
    } catch (e) { alert('해제 실패'); }
  };

  // 현재 기기별 배치(lanes)를 서버 items 형식으로 추출 — 배포·장면저장 공용
  const getCurrentItems = () => {
    const allItems = [];
    const seen = new Set();
    groupDevices.forEach(device => {
      (lanes[device.id] || []).forEach(item => {
        const key = `${item.mediaId}-${device.id}`;
        if (!seen.has(key)) { seen.add(key); allItems.push({ mediaId: item.mediaId, duration: item.duration, transition: item.transition, transitionTime: item.transitionTime, slideDirection: item.slideDirection, targetDeviceId: device.id }); }
      });
    });
    return allItems;
  };

  const handleSave = async () => {
    if (!selectedGroupId || !isDirty) return;
    setSaving(true);
    setDeployPanel('saving');
    try {
      const allItems = getCurrentItems();
      await apiFetch(`${API}/api/groups/${selectedGroupId}/playlist`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ items: allItems }), });
      // 서버 저장 후 서버 상태를 다시 불러와 타임라인과 라이브러리를 정확히 동기화
      await fetchPlaylist();
      await fetchUndo();          // 배포 직후 되돌리기 가능 상태 갱신
      markGroupActive(selectedGroupId); // 배포함 → 되돌리기 버튼 노출(안 누르면 탭 세션 동안 유지)
      setShowArchived(false);
      setDeployPanel('deploying'); // 기기 다운로드 단계로 전환
    } catch (e) {
      setDeployPanel(null);
      alert('저장 실패');
    }
    finally { setSaving(false); }
  };

  const uploadOneFile = (file) => new Promise((resolve, reject) => {
    const form = new FormData();
    if (selectedStoreId) form.append('storeId', selectedStoreId); // 텍스트 필드를 파일보다 먼저 (multer req.body 보장)
    form.append('file', file);
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${API}/api/media`);
    const token = localStorage.getItem('SIGNAGE_TOKEN') || '';
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    xhr.upload.onprogress = (ev) => {
      if (ev.lengthComputable) {
        setUploadProgress({ name: file.name, pct: Math.round((ev.loaded / ev.total) * 100) });
      }
    };
    xhr.onload = () => xhr.status < 400 ? resolve() : reject(new Error(`업로드 실패 (${xhr.status})`));
    xhr.onerror = () => reject(new Error('네트워크 오류'));
    xhr.send(form);
  });

  const handleUpload = async (e) => {
    const files = [...e.target.files];
    e.target.value = '';
    for (const file of files) {
      setUploadProgress({ name: file.name, pct: 0 });
      try {
        await uploadOneFile(file);
      } catch (err) {
        alert(`${file.name}\n${err.message}`);
      }
    }
    setUploadProgress(null);
    fetchMedia();
  };

  const handleDeleteMedia = async (id, filename) => {
    if (!window.confirm(`'${filename}'을(를) 삭제하시겠습니까?\n타임라인에 배치된 경우 함께 제거됩니다.`)) return;
    await apiFetch(`${API}/api/media/${id}`, { method: 'DELETE' });
    fetchMedia();
  };

  const handleDeleteAllMedia = async () => {
    if (mediaList.length === 0) return;
    if (!window.confirm(`에셋 라이브러리의 미디어 ${mediaList.length}개를 모두 삭제하시겠습니까?\n타임라인 배치도 함께 제거되며 되돌릴 수 없습니다.`)) return;
    await apiFetch(`${API}/api/media?storeId=${selectedStoreId}`, { method: 'DELETE' });
    fetchMedia();
  };

  // 현재 그룹의 전 기기 재생목록(타임라인)을 비움 — 라이브러리 미디어는 유지.
  // 비운 뒤 '변경사항 저장 및 배포'를 눌러야 기기에 반영된다.
  const handleClearAllLanes = () => {
    if (totalLaneItems === 0) return;
    if (!window.confirm(`현재 그룹의 재생목록(전 기기 ${totalLaneItems}개 항목)을 모두 비울까요?\n에셋 라이브러리는 유지됩니다. 비운 뒤 '변경사항 저장 및 배포'를 눌러야 기기에 반영됩니다.`)) return;
    setLanes(prev => {
      const next = {};
      Object.keys(prev).forEach(k => { next[k] = []; });
      return next;
    });
  };

  const isDirty = JSON.stringify(lanes) !== JSON.stringify(savedState);
  const totalLaneItems = Object.values(lanes).reduce((n, arr) => n + (arr?.length || 0), 0);

  // 파일 크기 포맷
  const formatSize = (bytes) => {
    if (!bytes) return '';
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)}KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  };

  // 확장자 그룹 접기/펼치기
  const toggleExtGroup = (key) => {
    setCollapsedExts(prev => {
      const next = new Set(prev);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  };

  // 보관함: 날짜 → 확장자별 그룹화 (정렬 방향 반영)
  const getArchivedGroups = (list, sort) => {
    const byDate = {};
    list.forEach(media => {
      const d = new Date(media.createdAt);
      const dateKey = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
      if (!byDate[dateKey]) byDate[dateKey] = {};
      const ext = media.filename.includes('.') ? media.filename.split('.').pop().toLowerCase() : 'unknown';
      if (!byDate[dateKey][ext]) byDate[dateKey][ext] = [];
      byDate[dateKey][ext].push(media);
    });
    return Object.entries(byDate)
      .sort(([a], [b]) => sort === 'date-asc' ? a.localeCompare(b) : b.localeCompare(a))
      .map(([dateKey, byExt]) => ({
        dateKey,
        exts: Object.entries(byExt)
          .sort(([a], [b]) => a.localeCompare(b))
          .map(([ext, items]) => ({ ext, items }))
      }));
  };

  const renderLibraryItem = (media, showSize = false) => (
    <div
      key={media.id}
      className="library-item"
      data-filename={media.filename}
      style={{
        cursor: libDragPos && libDragMediaRef.current?.id === media.id ? 'grabbing' : 'grab',
        userSelect: 'none',
        touchAction: 'none',
      }}
      onPointerDown={e => {
        if (e.button !== 0) return; // 왼쪽 버튼만
        e.preventDefault();
        libDragMediaRef.current = media;
        setLibDragPos({ x: e.clientX, y: e.clientY });
      }}
      onDoubleClick={e => { e.preventDefault(); libDragMediaRef.current = null; setLibDragPos(null); setLibPreview(media); }}
      title="더블클릭: 크게 보기"
    >
      <div className="library-item-thumb">
        <MediaThumb path={media.path} style={{ width: '100%', height: 'auto', pointerEvents: 'none', display: 'block' }} />
      </div>
      <span className="library-item-name">
        {showSize && media.size ? <span className="item-size-badge">{formatSize(media.size)}</span> : null}
        {media.filename}
      </span>
      <button
        className="library-item-del"
        onPointerDown={e => e.stopPropagation()} // 삭제 버튼 클릭 시 드래그 시작 방지
        onClick={() => handleDeleteMedia(media.id, media.filename)}
        title="삭제"
      >
        <Trash2 size={11} />
      </button>
    </div>
  );

  // 보관함: 현재 그룹 플레이리스트에 사용되지 않은(미사용) 미디어만 노출
  const archivedList = mediaList.filter(m => !usedMediaIds.has(m.id));

  // 예약된(미래·미적용) 교체 — 예약교체 버튼 배지/배너용
  const schedNowMs = Date.now();
  const pendingScheds = groupScheds.filter(s => !s.applied && s.switchAt && new Date(s.switchAt).getTime() > schedNowMs);
  const nextSched = pendingScheds.slice().sort((a, b) => new Date(a.switchAt) - new Date(b.switchAt))[0];
  const fmtSched = (iso) => { try { return new Date(iso).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' }); } catch { return iso; } };

  return (
    <div className="mm-root" style={{ cursor: libDragPos ? 'grabbing' : undefined }}>
      {/* 포인터 드래그 고스트 */}
      {libDragPos && libDragMediaRef.current && (
        <div style={{
          position: 'fixed',
          left: libDragPos.x - 30,
          top: libDragPos.y - 30,
          width: 60,
          height: 60,
          borderRadius: 8,
          border: '2px solid #3b82f6',
          overflow: 'hidden',
          pointerEvents: 'none',
          zIndex: 99999,
          opacity: 0.85,
          boxShadow: '0 4px 16px rgba(0,0,0,0.5)',
        }}>
          <MediaThumb path={libDragMediaRef.current.path} style={{ width: '100%', height: '100%' }} />
        </div>
      )}
      <div className="mm-header">
        <div className="mm-title-group">
          <Monitor size={20} color="#3b82f6" />
          <span>미디어 편성</span>
          <select className="glass-select" value={selectedStoreId} onChange={e => setSelectedStoreId(e.target.value)}>
            {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
          <select className="glass-select" value={selectedGroupId} onChange={e => { setSelectedGroupId(e.target.value); if (setSelectedZoneId) setSelectedZoneId(e.target.value); }}>
            <option value="">그룹 선택</option>
            {storeGroups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}
          </select>
        </div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
          {/* 일괄 전환 컨트롤 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '4px 10px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 8 }}>
            <span style={{ fontSize: '0.72rem', color: '#64748b', whiteSpace: 'nowrap' }}>전체 전환:</span>
            <select
              value={bulkTransition}
              onChange={e => setBulkTransition(e.target.value)}
              className="glass-select"
              style={{ fontSize: '0.75rem', padding: '2px 4px' }}
            >
              <option value="fade">FADE</option>
              <option value="slide">SLIDE</option>
              <option value="dissolve">DISSOLVE</option>
              <option value="none">CUT</option>
            </select>
            {bulkTransition !== 'none' && (
              <>
                <input
                  type="number"
                  value={bulkDuration}
                  min={0}
                  step={100}
                  onChange={e => setBulkDuration(Number(e.target.value))}
                  style={{ width: 64, fontSize: '0.75rem', padding: '2px 6px', background: '#1e293b', border: '1px solid #334155', borderRadius: 5, color: '#f1f5f9', textAlign: 'right' }}
                />
                <span style={{ fontSize: '0.7rem', color: '#64748b' }}>ms</span>
              </>
            )}
            <span style={{ width: 1, height: 18, background: 'rgba(255,255,255,0.12)', margin: '0 2px' }} />
            <span style={{ fontSize: '0.72rem', color: '#64748b', whiteSpace: 'nowrap' }}>표시 시간:</span>
            <input
              type="number"
              value={bulkSlideDuration}
              min={1}
              step={1}
              onChange={e => setBulkSlideDuration(Number(e.target.value))}
              style={{ width: 56, fontSize: '0.75rem', padding: '2px 6px', background: '#1e293b', border: '1px solid #334155', borderRadius: 5, color: '#f1f5f9', textAlign: 'right' }}
            />
            <span style={{ fontSize: '0.7rem', color: '#64748b' }}>초</span>
            <button
              onClick={() => {
                setLanes(prev => {
                  const updated = {};
                  Object.entries(prev).forEach(([devId, items]) => {
                    updated[devId] = items.map(item => ({
                      ...item,
                      transition: bulkTransition,
                      transitionTime: bulkTransition === 'none' ? 0 : bulkDuration,
                      duration: Math.max(1, bulkSlideDuration),
                    }));
                  });
                  return updated;
                });
              }}
              style={{ fontSize: '0.72rem', padding: '3px 8px', borderRadius: 5, border: '1px solid rgba(59,130,246,0.5)', background: 'rgba(59,130,246,0.15)', color: '#60a5fa', cursor: 'pointer', whiteSpace: 'nowrap' }}
            >
              일괄 적용
            </button>
          </div>
          <button
            onClick={() => setShowSchedModal(true)}
            disabled={!selectedGroupId}
            title={pendingScheds.length ? `예약된 자동 교체 ${pendingScheds.length}건 — 다음 ${fmtSched(nextSched.switchAt)}` : '시각에 맞춰 자동으로 배치를 교체하도록 예약 (무인)'}
            className={pendingScheds.length ? 'sched-btn-pulse' : ''}
            style={{
              position: 'relative',
              display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.78rem', padding: '8px 14px',
              borderRadius: 8,
              border: `1px solid ${pendingScheds.length ? 'rgba(167,139,250,0.9)' : 'rgba(167,139,250,0.5)'}`,
              background: pendingScheds.length ? 'rgba(167,139,250,0.25)' : 'rgba(167,139,250,0.12)',
              color: pendingScheds.length ? '#c4b5fd' : '#a78bfa', cursor: selectedGroupId ? 'pointer' : 'not-allowed',
              opacity: selectedGroupId ? 1 : 0.5, whiteSpace: 'nowrap',
            }}
          >
            <CalendarClock size={15} /> 예약 교체
            {pendingScheds.length > 0 && (
              <span style={{ background: '#a78bfa', color: '#1e1b4b', fontWeight: 700, fontSize: '0.7rem', borderRadius: 10, padding: '0 6px', minWidth: 16, textAlign: 'center' }}>{pendingScheds.length}</span>
            )}
          </button>
          <button
            onClick={handleClearAllLanes}
            disabled={totalLaneItems === 0}
            title="현재 그룹 전 기기의 재생목록 비우기 (라이브러리는 유지)"
            style={{
              display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.78rem', padding: '8px 14px',
              borderRadius: 8, border: '1px solid rgba(239,68,68,0.5)', background: 'rgba(239,68,68,0.12)',
              color: '#f87171', cursor: totalLaneItems === 0 ? 'not-allowed' : 'pointer',
              opacity: totalLaneItems === 0 ? 0.5 : 1, whiteSpace: 'nowrap',
            }}
          >
            <Trash2 size={15} /> 재생목록 전체 삭제
          </button>
          <button className={`btn-deploy ${isDirty ? '' : 'inactive'}`} onClick={handleSave} disabled={saving || !isDirty}>
            <Save size={18} style={{ marginRight: 8 }} /> {saving ? '저장 중...' : '변경사항 저장 및 배포'}
          </button>
          {/* 되돌리기 ↔ 다시 원복 토글. 배포한 그룹만 노출, 안 누르면 탭 세션 동안 유지 */}
          {activeGroups.has(selectedGroupId) && undoInfo.available && (
            <button
              onClick={handleRevert}
              disabled={reverting || saving}
              title={undoInfo.mode === 'redo' ? '되돌리기를 취소하고 방금 배포 상태로 원복' : '직전 배포로 되돌립니다'}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: 6, marginLeft: 8,
                padding: '0 14px', height: 40, borderRadius: 8, fontSize: '0.85rem', fontWeight: 600,
                whiteSpace: 'nowrap', flexShrink: 0,
                background: 'transparent',
                color: undoInfo.mode === 'redo' ? '#34d399' : '#fbbf24',
                border: `1px solid ${undoInfo.mode === 'redo' ? 'rgba(52,211,153,0.45)' : 'rgba(251,191,36,0.45)'}`,
                cursor: (!reverting && !saving) ? 'pointer' : 'not-allowed',
              }}
            >
              <RotateCcw size={16} style={undoInfo.mode === 'redo' ? { transform: 'scaleX(-1)' } : undefined} />
              {reverting ? '처리 중...' : (undoInfo.mode === 'redo' ? '다시 원복' : '직전 배포 되돌리기')}
            </button>
          )}
        </div>
      </div>

      {/* 배포 진행 패널 */}
      {deployPanel && (
        <div className="deploy-panel">
          <div className="deploy-panel-header">
            <span className="deploy-panel-title">
              {deployPanel === 'saving'
                ? <><span className="deploy-spinner" /> 서버에 저장 중…</>
                : <>✅ 서버 저장 완료 — 기기 동기화 중</>}
            </span>
            <button className="deploy-close-btn" onClick={() => setDeployPanel(null)}>✕</button>
          </div>
          {deployPanel === 'deploying' && (
            <div className="deploy-devices">
              {groupDevices.map(gd => {
                const d = devices.find(x => x.id === gd.id);
                const dl = d?.dl;
                const online = d?.status === 'online';
                return (
                  <div key={gd.id} className="deploy-device-row">
                    <span className="deploy-dot" style={{ color: online ? '#22c55e' : '#ef4444' }}>●</span>
                    <span className="deploy-device-name">{gd.name || gd.id}</span>
                    {!online ? (
                      <span className="deploy-badge offline">오프라인</span>
                    ) : dl ? (
                      <div className="deploy-dl-wrap">
                        <div className="deploy-dl-bar">
                          <div className="deploy-dl-fill" style={{ width: `${dl.pct}%` }} />
                        </div>
                        <span className="deploy-dl-text" title="현재 다운로드 중인 파일의 진행률">
                          파일 {dl.cur}/{dl.total} 다운로드 중 · {dl.pct}%
                        </span>
                      </div>
                    ) : (
                      <span className="deploy-badge done">✔ 준비됨</span>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {nextSched && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 24px', background: 'rgba(167,139,250,0.1)', borderBottom: '1px solid rgba(167,139,250,0.25)', color: '#c4b5fd', fontSize: '0.82rem' }}>
          <CalendarClock size={15} />
          <span><b>다음 자동 교체 예정:</b> {fmtSched(nextSched.switchAt)} → <b>{nextSched.scene?.name || '(장면)'}</b>{pendingScheds.length > 1 ? ` · 외 ${pendingScheds.length - 1}건` : ''}</span>
          <button onClick={() => setShowSchedModal(true)} style={{ marginLeft: 'auto', background: 'transparent', border: '1px solid rgba(167,139,250,0.5)', color: '#c4b5fd', borderRadius: 6, padding: '2px 10px', fontSize: '0.76rem', cursor: 'pointer', whiteSpace: 'nowrap' }}>예약 관리</button>
        </div>
      )}
      <div className="mm-body">
        <div className="mm-library">
          <div className="mm-library-header">
            <span className="mm-library-title">에셋 라이브러리</span>
            <div style={{ display: 'flex', gap: 6 }}>
              <button className="icon-btn" onClick={() => setLibrarySmall(v => !v)} title={librarySmall ? '크게 보기' : '작게 보기'} style={{ fontSize: '0.6rem', fontWeight: 700, color: librarySmall ? '#60a5fa' : undefined }}>{librarySmall ? '2열' : '4열'}</button>
              <button className="icon-btn" onClick={() => fileRef.current?.click()} title="업로드" disabled={!!uploadProgress}><Upload size={14} /></button>
              <button className="icon-btn icon-btn-danger" onClick={handleDeleteAllMedia} disabled={mediaList.length === 0 || !!uploadProgress} title="전체 삭제"><Trash2 size={14} /></button>
            </div>
            <input ref={fileRef} type="file" multiple accept="image/*,video/*,.mov,.avi,.mkv" style={{ display: 'none' }} onChange={handleUpload} />
          </div>
          {uploadProgress && (
            <div className="upload-progress-bar-wrap">
              <div className="upload-progress-filename">{uploadProgress.name}</div>
              <div className="upload-progress-track">
                <div className="upload-progress-fill" style={{ width: `${uploadProgress.pct}%` }} />
              </div>
              <span className="upload-progress-pct">{uploadProgress.pct}%</span>
            </div>
          )}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '4px 8px', fontSize: '0.72rem', background: '#1e293b', borderBottom: '1px solid #334155' }}>
            <span style={{ color: '#94a3b8' }}>
              {showArchived
                ? `미사용 ${archivedList.length}개 / 전체 ${mediaList.length}개`
                : usedMediaIds.size > 0
                  ? `사용 중 ${usedMediaIds.size}개 / 전체 ${mediaList.length}개`
                  : `전체 ${mediaList.length}개`}
            </span>
            <button
              onClick={() => setShowArchived(v => !v)}
              style={{ fontSize: '0.7rem', padding: '2px 8px', borderRadius: 4, border: '1px solid #475569', background: 'transparent', color: showArchived ? '#f59e0b' : '#64748b', cursor: 'pointer' }}
            >
              {showArchived ? '📂 보관함 숨기기' : '📦 보관함 보기'}
            </button>
          </div>
          {showArchived ? (
            <div ref={libraryListRef} className={`mm-library-list archive-mode${librarySmall ? ' small-view' : ''}`}>
              {/* 정렬 버튼 */}
              <div className="archive-sort-bar">
                <button className={`sort-btn ${archiveSort === 'date-desc' ? 'active' : ''}`} onClick={() => setArchiveSort('date-desc')}>최신순</button>
                <button className={`sort-btn ${archiveSort === 'date-asc' ? 'active' : ''}`} onClick={() => setArchiveSort('date-asc')}>오래된순</button>
                <button className={`sort-btn ${archiveSort === 'ext' ? 'active' : ''}`} onClick={() => setArchiveSort('ext')}>확장자별</button>
                <button className={`sort-btn ${archiveSort === 'size-desc' ? 'active' : ''}`} onClick={() => setArchiveSort('size-desc')}>용량순</button>
              </div>

              {archiveSort === 'size-desc' ? (
                /* 용량순: 플랫 그리드 */
                <div className="archive-ext-grid" style={{ padding: '6px' }}>
                  {[...archivedList]
                    .sort((a, b) => (b.size || 0) - (a.size || 0))
                    .map(media => renderLibraryItem(media, true))}
                </div>
              ) : archiveSort === 'ext' ? (
                /* 확장자별 그룹 (날짜 무관) */
                (() => {
                  const byExt = {};
                  archivedList.forEach(media => {
                    const ext = media.filename.includes('.') ? media.filename.split('.').pop().toLowerCase() : 'unknown';
                    if (!byExt[ext]) byExt[ext] = [];
                    byExt[ext].push(media);
                  });
                  return Object.entries(byExt)
                    .sort(([a], [b]) => a.localeCompare(b))
                    .map(([ext, items]) => {
                      const groupKey = `ext-${ext}`;
                      const isCollapsed = collapsedExts.has(groupKey);
                      return (
                        <div key={ext} className="archive-ext-group">
                          <div className="archive-ext-label" onClick={() => toggleExtGroup(groupKey)}>
                            <span className="ext-badge">.{ext}</span>
                            <span className="ext-count">{items.length}개</span>
                            <span className="ext-chevron">{isCollapsed ? '▶' : '▼'}</span>
                          </div>
                          {!isCollapsed && (
                            <div className="archive-ext-grid">
                              {items.map(media => renderLibraryItem(media))}
                            </div>
                          )}
                        </div>
                      );
                    });
                })()
              ) : (
                /* 날짜별 그룹 */
                getArchivedGroups(archivedList, archiveSort).map(({ dateKey, exts }) => (
                  <div key={dateKey} className="archive-date-group">
                    <div className="archive-date-label">📅 {dateKey}</div>
                    {exts.map(({ ext, items }) => {
                      const groupKey = `${dateKey}-${ext}`;
                      const isCollapsed = collapsedExts.has(groupKey);
                      return (
                        <div key={ext} className="archive-ext-group">
                          <div className="archive-ext-label" onClick={() => toggleExtGroup(groupKey)}>
                            <span className="ext-badge">.{ext}</span>
                            <span className="ext-count">{items.length}개</span>
                            <span className="ext-chevron">{isCollapsed ? '▶' : '▼'}</span>
                          </div>
                          {!isCollapsed && (
                            <div className="archive-ext-grid">
                              {items.map(media => renderLibraryItem(media))}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                ))
              )}

              {archivedList.length === 0 && (
                <div style={{ padding: 16, color: '#475569', fontSize: '0.75rem', textAlign: 'center' }}>
                  {mediaList.length === 0 ? '업로드된 미디어가 없습니다' : '미사용 미디어가 없습니다 (전부 사용 중)'}
                </div>
              )}
            </div>
          ) : (
            <div ref={libraryListRef} className={`mm-library-list${librarySmall ? ' small-view' : ''}`}>
              {mediaList
                .filter(media => usedMediaIds.size === 0 || usedMediaIds.has(media.id))
                .map(media => renderLibraryItem(media))}
            </div>
          )}
        </div>

        <div className="mm-timeline-area">
          <div className="mm-timeline-header">
            <span className="mm-timeline-title">재생목록 타임라인</span>
            <button
              className="timeline-compact-toggle"
              onClick={() => setTimelineCompact(v => !v)}
              title={timelineCompact ? '크게 보기' : '작게 보기 (한눈에)'}
            >
              {timelineCompact ? '◰ 크게 보기' : '▦ 작게 보기'}
            </button>
          </div>

          <DndContext
            sensors={useSensors(useSensor(DeviceRowPointerSensor, { activationConstraint: { distance: 8 } }))}
            collisionDetection={closestCenter}
            onDragEnd={({ active, over }) => {
              if (!over || active.id === over.id) return;
              const oldOrder = groupDevices.map(d => d.id);
              const oldIdx = oldOrder.indexOf(active.id);
              const newIdx = oldOrder.indexOf(over.id);
              const newOrder = arrayMove(oldOrder, oldIdx, newIdx);
              onDeviceOrderChange?.(prev => ({ ...prev, [selectedGroupId]: newOrder }));
            }}
          >
            <SortableContext items={groupDevices.map(d => d.id)} strategy={verticalListSortingStrategy}>
              <div className={`mm-lanes${timelineCompact ? ' compact' : ''}`}>
                {groupDevices.map(device => (
                  <DeviceRowV4
                    key={device.id}
                    device={device}
                    items={lanes[device.id] || []}
                    changedSlots={changedByDevice[device.id] || []}
                    isDirty={JSON.stringify(lanes[device.id] || []) !== JSON.stringify(savedState[device.id] || [])}
                    onDrop={media => handleDrop(device.id, media)}
                    onRemoveItem={idx => handleRemoveItem(device.id, idx)}
                    onChangeItem={(idx, upd) => handleChangeItem(device.id, idx, upd)}
                    onDeleteDevice={handleDeleteDevice}
                    onReorder={(fromIdx, toIdx) => handleReorder(device.id, fromIdx, toIdx)}
                    onPreview={() => setPreviewData({ items: lanes[device.id] || [], deviceName: device.name })}
                    onTransitionPreview={(item, next, idx) => setTransPreview({
                      currentItem: item,
                      nextItem: next,
                      laneIdx: device.id,
                      itemIdx: idx
                    })}
                    libDragOver={libDragOverId === device.id}
                    dropTarget={libDropTarget && libDropTarget.deviceId === device.id ? libDropTarget : null}
                  />
                ))}
              </div>
            </SortableContext>
          </DndContext>
        </div>
      </div>

      {transPreview && (
        <TransitionPreviewModal 
          currentItem={transPreview.currentItem} 
          nextItem={transPreview.nextItem} 
          onChange={(upd) => handleChangeItem(transPreview.laneIdx, transPreview.itemIdx, upd)}
          onClose={() => setTransPreview(null)} 
        />
      )}

      {previewData && (
        <PlaylistPreviewModal
          items={previewData.items}
          deviceName={previewData.deviceName}
          onClose={() => setPreviewData(null)}
        />
      )}
      {showSchedModal && selectedGroupId && (
        <ContentScheduleModal
          groupId={selectedGroupId}
          getCurrentItems={getCurrentItems}
          onApplied={() => { fetchPlaylist(); }}
          onClose={() => setShowSchedModal(false)}
        />
      )}
      {libPreview && (
        <div onClick={() => setLibPreview(null)} style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.85)', zIndex: 2000, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12, padding: 24 }}>
          {/\.(mp4|webm|mov)$/i.test(libPreview.path || '')
            ? <video onClick={e => e.stopPropagation()} src={(libPreview.path || '').startsWith('http') ? libPreview.path : `${API}${libPreview.path}`} controls autoPlay loop style={{ maxWidth: '92vw', maxHeight: '82vh', borderRadius: 8, background: '#000' }} />
            : <img onClick={e => e.stopPropagation()} src={(libPreview.path || '').startsWith('http') ? libPreview.path : `${API}${libPreview.path}`} alt={libPreview.filename} style={{ maxWidth: '92vw', maxHeight: '82vh', objectFit: 'contain', borderRadius: 8 }} />}
          <div onClick={e => e.stopPropagation()} style={{ color: '#e2e8f0', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 14 }}>
            <span>{libPreview.filename}</span>
            {libPreview.size ? <span style={{ color: '#94a3b8' }}>{formatSize(libPreview.size)}</span> : null}
            <button onClick={() => setLibPreview(null)} style={{ background: 'rgba(255,255,255,0.12)', border: '1px solid rgba(255,255,255,0.25)', color: '#fff', borderRadius: 6, padding: '4px 14px', cursor: 'pointer' }}>닫기</button>
          </div>
        </div>
      )}
    </div>
  );
};

export default MediaManager;
