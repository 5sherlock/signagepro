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
  GripVertical
} from 'lucide-react';
import './PreviewModal.css';
import './MediaManager.css';

const API = SOCKET_URL;

// ── 미디어 렌더러 ─────────────────────────────────────────
const MediaThumb = ({ path, style = {} }) => {
  if (!path) return <div style={{ background: '#111', ...style }} />;
  const url = `${API}${path}`;
  const isVideo = /\.(mp4|webm|mov)$/i.test(path);
  if (isVideo) return <video src={url} muted loop autoPlay playsInline style={{ objectFit: 'contain', ...style }} />;
  return <img src={url} alt="" style={{ objectFit: 'contain', ...style }} />;
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
  const url = `${API}${item.media.path}`;
  const isVideo = /\.(mp4|webm|mov)$/i.test(item.media.path);
  const frames = Array.from({ length: 40 });
  
  return (
    <div style={{ display: 'flex', height: '100%', width: '100%', overflow: 'hidden' }}>
      {frames.map((_, i) => (
        <div key={i} className="track-frame-container">
          {isVideo ? (
            <video className="track-frame-thumb" src={url} muted />
          ) : (
            <img className="track-frame-thumb" src={url} alt="" />
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
                      {itTrans.toUpperCase()} {(itTransTime/1000).toFixed(1)}s
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
  const [transType, setTransType] = useState(currentItem.transition || 'fade');
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
    const url = `${API}${item.media.path}`;
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
  return (
    <div className="transition-bridge-v4">
      <div className="bridge-line" />
      <div className="bridge-box">
        <select 
          className="mini-select" 
          value={item.transition || 'fade'} 
          onChange={e => onChange({ transition: e.target.value })}
        >
          <option value="fade">FADE</option>
          <option value="slide">SLIDE</option>
          <option value="dissolve">DISSOLVE</option>
          <option value="none">없음</option>
        </select>
        <div className="bridge-time-row">
          <input type="number" value={transitionTime} min={0} step={100} onChange={e => onChange({ transitionTime: Number(e.target.value) })} />
          <span>ms</span>
        </div>
        <button className="bridge-preview-btn" onClick={onPreview}>미리보기</button>
      </div>
      <div className="bridge-line" />
      {isLoop && <span className="loop-badge">LOOP ↩</span>}
    </div>
  );
};

// ── [V4] 기기 고정형 행 (dnd-kit sortable) ───────────────────
const DeviceRowV4 = ({ device, items, isDirty, onDrop, onRemoveItem, onChangeItem, onDeleteDevice, onPreview, onTransitionPreview, onReorder, libDragOver = false }) => {
  const [reorderDragIdx, setReorderDragIdx] = useState(null);
  const [reorderOverIdx, setReorderOverIdx] = useState(null);

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

        <div style={{ flex: 1, minHeight: '20px' }}></div>

        {items.length > 0 && (
          <button className="device-full-preview-btn" onClick={onPreview}>
            <ExternalLink size={12} style={{ marginRight: 4 }} />
            전체 미리보기
          </button>
        )}
      </div>

      <div
        className={`device-timeline-v4 ${libDragOver ? 'drag-over' : ''}`}
        data-device-id={device.id}
      >
        {items.length === 0 && <div className="timeline-empty">미디어를 드래그하여 추가하세요</div>}
        {items.map((item, idx) => {
          const isLast = idx === items.length - 1;
          const nextItem = items.length > 1 ? items[(idx + 1) % items.length] : null;
          const isDraggedOver = reorderOverIdx === idx && reorderDragIdx !== idx;
          const isDragging = reorderDragIdx === idx;

          return (
            <React.Fragment key={item._key || idx}>
              <div
                draggable
                className={`reorder-item-wrapper${isDragging ? ' reorder-dragging' : ''}${isDraggedOver ? ' reorder-over' : ''}`}
                onDragStart={e => {
                  e.dataTransfer.setData('timeline-index', String(idx));
                  e.dataTransfer.effectAllowed = 'move';
                  setReorderDragIdx(idx);
                }}
                onDragEnd={() => {
                  setReorderDragIdx(null);
                  setReorderOverIdx(null);
                }}
                onDragOver={e => {
                  if (e.dataTransfer.types.includes('device-row-id')) return;
                  e.preventDefault();
                  if (isReorderDrag(e)) {
                    // 타임라인 내 순서 변경 드래그만 여기서 처리
                    e.stopPropagation();
                    if (reorderDragIdx !== idx) {
                      setReorderOverIdx(idx);
                      setDragOver(false);
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
      </div>
    </div>
  );
};

// ── 메인 MediaManager ─────────────────────────────────────
const MediaManager = ({ stores = [], groups = [], devices = [], selectedStoreId, setSelectedStoreId, fetchDevices, deviceOrder = {}, onDeviceOrderChange }) => {
  const [mediaList, setMediaList] = useState([]);
  const [showArchived, setShowArchived] = useState(false); // 보관함(미사용) 표시 여부
  const [usedMediaIds, setUsedMediaIds] = useState(new Set()); // 저장 후 사용 중인 ID
  const [archiveSort, setArchiveSort] = useState('date-desc'); // 'date-desc' | 'date-asc' | 'ext' | 'size-desc'
  const [collapsedExts, setCollapsedExts] = useState(new Set()); // 접힌 확장자 그룹 키
  const [selectedGroupId, setSelectedGroupId] = useState('');
  const [lanes, setLanes] = useState({});
  const [savedState, setSavedState] = useState({});
  const [saving, setSaving] = useState(false);
  const [previewData, setPreviewData] = useState(null);
  const [transPreview, setTransPreview] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(null); // null | { name, pct }
  const [deployPanel, setDeployPanel] = useState(null); // null | 'saving' | 'deploying'
  const deployHasSeenDl = useRef(false); // 기기 다운로드가 한 번이라도 감지됐는지
  const fileRef = useRef();
  const groupDevicesRef = useRef([]); // fetchPlaylist 스테일 클로저 방지용 ref

  // 라이브러리 → 타임라인 포인터 드래그 (crxMouse 제스처 충돌 방지)
  const libDragMediaRef = useRef(null);   // 드래그 중인 미디어 객체
  const handleDropRef = useRef(null);     // handleDrop 최신 참조
  const [libDragPos, setLibDragPos] = useState(null);   // { x, y } 드래그 중 좌표
  const [libDragOverId, setLibDragOverId] = useState(null); // 현재 hover 중인 deviceId

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

  useEffect(() => {
    setSelectedGroupId('');
    setLanes({});
    setSavedState({});
  }, [selectedStoreId]);

  useEffect(() => {
    if (storeGroups.length > 0 && !selectedGroupId) setSelectedGroupId(storeGroups[0].id);
  }, [storeGroups.length, selectedStoreId]);

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

  useEffect(() => { fetchMedia(); }, [fetchMedia]);
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

  const handleDrop = (deviceId, media) => {
    const newItem = { mediaId: media.id, media, duration: 10, transition: 'fade', transitionTime: 1000, slideDirection: 'right', _key: `${media.id}-${Date.now()}` };
    setLanes(prev => {
      const updated = { ...prev };
      updated[deviceId] = [...(updated[deviceId] || []), newItem];
      return updated;
    });
  };
  // 항상 최신 handleDrop 참조 유지
  handleDropRef.current = handleDrop;

  // 포인터 이벤트 기반 라이브러리 드래그 (HTML5 draggable 대체 — crxMouse 충돌 방지)
  useEffect(() => {
    if (!libDragPos) return;
    const handleMove = (e) => {
      setLibDragPos({ x: e.clientX, y: e.clientY });
      const els = document.elementsFromPoint(e.clientX, e.clientY);
      const tl = els.find(el => el.dataset?.deviceId);
      setLibDragOverId(tl ? tl.dataset.deviceId : null);
    };
    const handleUp = (e) => {
      const els = document.elementsFromPoint(e.clientX, e.clientY);
      const tl = els.find(el => el.dataset?.deviceId);
      if (tl?.dataset.deviceId && libDragMediaRef.current) {
        handleDropRef.current(tl.dataset.deviceId, libDragMediaRef.current);
      }
      libDragMediaRef.current = null;
      setLibDragPos(null);
      setLibDragOverId(null);
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

  const handleSave = async () => {
    if (!selectedGroupId || !isDirty) return;
    setSaving(true);
    setDeployPanel('saving');
    try {
      const allItems = [];
      const seen = new Set();
      groupDevices.forEach(device => {
        (lanes[device.id] || []).forEach(item => {
          const key = `${item.mediaId}-${device.id}`;
          if (!seen.has(key)) { seen.add(key); allItems.push({ mediaId: item.mediaId, duration: item.duration, transition: item.transition, transitionTime: item.transitionTime, slideDirection: item.slideDirection, targetDeviceId: device.id }); }
        });
      });
      await apiFetch(`${API}/api/groups/${selectedGroupId}/playlist`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ items: allItems }), });
      // 서버 저장 후 서버 상태를 다시 불러와 타임라인과 라이브러리를 정확히 동기화
      await fetchPlaylist();
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
    form.append('file', file);
    form.append('storeId', selectedStoreId);
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

  const isDirty = JSON.stringify(lanes) !== JSON.stringify(savedState);

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
    >
      <div className="library-item-thumb">
        <MediaThumb path={media.path} style={{ width: '100%', height: '100%', pointerEvents: 'none' }} />
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
          <select className="glass-select" value={selectedGroupId} onChange={e => setSelectedGroupId(e.target.value)}>
            <option value="">그룹 선택</option>
            {storeGroups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}
          </select>
        </div>
        <div style={{ display: 'flex', gap: 20, alignItems: 'center' }}>
          <button className={`btn-deploy ${isDirty ? '' : 'inactive'}`} onClick={handleSave} disabled={saving || !isDirty}>
            <Save size={18} style={{ marginRight: 8 }} /> {saving ? '저장 중...' : '변경사항 저장 및 배포'}
          </button>
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

      <div className="mm-body">
        <div className="mm-library">
          <div className="mm-library-header">
            <span className="mm-library-title">에셋 라이브러리</span>
            <div style={{ display: 'flex', gap: 6 }}>
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
              {usedMediaIds.size > 0 && !showArchived
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
            <div className="mm-library-list archive-mode">
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
                  {[...mediaList]
                    .sort((a, b) => (b.size || 0) - (a.size || 0))
                    .map(media => renderLibraryItem(media, true))}
                </div>
              ) : archiveSort === 'ext' ? (
                /* 확장자별 그룹 (날짜 무관) */
                (() => {
                  const byExt = {};
                  mediaList.forEach(media => {
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
                getArchivedGroups(mediaList, archiveSort).map(({ dateKey, exts }) => (
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

              {mediaList.length === 0 && (
                <div style={{ padding: 16, color: '#475569', fontSize: '0.75rem', textAlign: 'center' }}>
                  업로드된 미디어가 없습니다
                </div>
              )}
            </div>
          ) : (
            <div className="mm-library-list">
              {mediaList
                .filter(media => usedMediaIds.size === 0 || usedMediaIds.has(media.id))
                .map(media => renderLibraryItem(media))}
            </div>
          )}
        </div>

        <div className="mm-timeline-area">
          <div className="mm-timeline-header">
            <span className="mm-timeline-title">재생목록 타임라인</span>
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
              <div className="mm-lanes">
                {groupDevices.map(device => (
                  <DeviceRowV4
                    key={device.id}
                    device={device}
                    items={lanes[device.id] || []}
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
    </div>
  );
};

export default MediaManager;
