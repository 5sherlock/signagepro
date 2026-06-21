import React, { useState, useEffect, useRef, useCallback } from 'react';
import { SOCKET_URL, apiFetch } from '../config';
import { Upload, Trash2, Video } from 'lucide-react';

const API = SOCKET_URL;

const VIDEO_RE = /\.(mp4|webm|mov|mkv|avi)$/i;
const fmtSize = (b) => (!b ? '' : b < 1048576 ? `${(b / 1024).toFixed(0)}KB` : `${(b / 1048576).toFixed(1)}MB`);

const stepBtn = {
  minWidth: 30, padding: '3px 6px', background: 'rgba(255,255,255,0.06)', color: '#cbd5e1',
  border: '1px solid rgba(255,255,255,0.12)', borderRadius: 5, cursor: 'pointer', fontSize: '0.72rem', fontWeight: 600,
};

/**
 * 동영상 메뉴 — 멀티스크린 비디오월용 동영상(슬라이스) 업로드/관리.
 * 업로드는 기존 미디어와 동일한 POST /api/media (multipart) 를 쓰되, 동영상만 노출.
 * 파일명에 "wallsync" 가 들어가면 플레이어가 wall-동기 비디오로 인식한다(배지 표시).
 */
const VideoWallManager = ({ stores = [], selectedStoreId, setSelectedStoreId }) => {
  const [videos, setVideos] = useState([]);
  const [uploadProgress, setUploadProgress] = useState(null); // null | { name, pct }
  const fileRef = useRef();

  // 기기별 위상 오프셋(ms) — 잔여 시계 스큐를 화면별로 직접 상쇄. 세션 동안 설정값 추적.
  const [devices, setDevices] = useState([]);
  const [offsets, setOffsets] = useState({});

  const fetchDevices = useCallback(async () => {
    try {
      const res = await apiFetch(`${API}/api/devices`);
      const list = await res.json();
      const online = (Array.isArray(list) ? list : []).filter((d) => d.status === 'online');
      setDevices(online);
      setOffsets((prev) => {
        const next = { ...prev };
        online.forEach((d) => { if (next[d.id] === undefined) next[d.id] = 0; });
        return next;
      });
    } catch (e) { console.error(e); }
  }, []);

  useEffect(() => {
    fetchDevices();
    const t = setInterval(fetchDevices, 5000);
    return () => clearInterval(t);
  }, [fetchDevices]);

  const applyOffset = async (deviceId, nextMs) => {
    const clamped = Math.max(-600000, Math.min(600000, Math.round(nextMs)));
    try {
      const res = await apiFetch(`${API}/api/devices/${deviceId}/wall-offset`, {
        method: 'POST',
        body: JSON.stringify({ offsetMs: clamped }),
      });
      if (!res.ok) {
        const e = await res.json().catch(() => ({}));
        alert(e.error || '오프셋 전송 실패');
        return;
      }
      setOffsets((p) => ({ ...p, [deviceId]: clamped }));
    } catch (e) { alert(e.message); }
  };

  // 서버가 측정한 기기별 시계 스큐(clockSkew)로 오프셋을 일괄 산출·적용 → 시작점 자동 정렬.
  // offset_i = skew_i - median(skew). 이후 화면 보며 미세조정.
  const autoAlign = async () => {
    const withSkew = devices.filter((d) => Number.isFinite(d.clockSkew));
    if (withSkew.length < 2) { alert('스큐 측정값이 부족합니다. (기기 2대 이상 + 하트비트 수신 필요)'); return; }
    const sorted = withSkew.map((d) => d.clockSkew).sort((a, b) => a - b);
    const ref = sorted[Math.floor(sorted.length / 2)]; // median
    for (const d of withSkew) {
      await applyOffset(d.id, d.clockSkew - ref);
    }
  };

  const fetchVideos = useCallback(async () => {
    if (!selectedStoreId) { setVideos([]); return; }
    try {
      const res = await apiFetch(`${API}/api/media?storeId=${selectedStoreId}`);
      const list = await res.json();
      setVideos(list.filter((m) => m.type === 'video' || VIDEO_RE.test(m.filename || '')));
    } catch (e) { console.error(e); }
  }, [selectedStoreId]);

  useEffect(() => { fetchVideos(); }, [fetchVideos]);

  const uploadOne = (file) => new Promise((resolve, reject) => {
    const form = new FormData();
    if (selectedStoreId) form.append('storeId', selectedStoreId); // 텍스트 필드를 파일보다 먼저
    form.append('file', file);
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${API}/api/media`);
    const token = localStorage.getItem('SIGNAGE_TOKEN') || '';
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    xhr.upload.onprogress = (ev) => {
      if (ev.lengthComputable) setUploadProgress({ name: file.name, pct: Math.round((ev.loaded / ev.total) * 100) });
    };
    xhr.onload = () => (xhr.status < 400 ? resolve() : reject(new Error(`업로드 실패 (${xhr.status})`)));
    xhr.onerror = () => reject(new Error('네트워크 오류'));
    xhr.send(form);
  });

  const handleUpload = async (e) => {
    const files = [...e.target.files];
    e.target.value = '';
    if (!selectedStoreId) { alert('먼저 사업장을 선택하세요.'); return; }
    for (const file of files) {
      const isVideo = /^video\//.test(file.type) || VIDEO_RE.test(file.name);
      if (!isVideo) { alert(`${file.name}: 동영상 파일만 업로드할 수 있습니다.`); continue; }
      setUploadProgress({ name: file.name, pct: 0 });
      try { await uploadOne(file); } catch (err) { alert(`${file.name}\n${err.message}`); }
    }
    setUploadProgress(null);
    fetchVideos();
  };

  const handleDelete = async (id, filename) => {
    if (!window.confirm(`'${filename}'을(를) 삭제할까요?`)) return;
    await apiFetch(`${API}/api/media/${id}`, { method: 'DELETE' });
    fetchVideos();
  };

  return (
    <div style={{ padding: '4px 8px', height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 헤더 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Video size={20} color="#3b82f6" />
          <span style={{ fontSize: '1.15rem', fontWeight: 600 }}>동영상</span>
        </div>
        <select className="glass-select" value={selectedStoreId || ''} onChange={(e) => setSelectedStoreId(e.target.value)}>
          <option value="">사업장 선택</option>
          {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <div style={{ flex: 1 }} />
        <button
          onClick={() => fileRef.current?.click()}
          disabled={!selectedStoreId}
          style={{
            display: 'flex', alignItems: 'center', gap: 6, padding: '8px 16px',
            background: selectedStoreId ? '#3b82f6' : '#334155', color: '#fff',
            border: 'none', borderRadius: 8, cursor: selectedStoreId ? 'pointer' : 'not-allowed',
            fontSize: '0.85rem', fontWeight: 600,
          }}
        >
          <Upload size={16} /> 동영상 업로드
        </button>
        <input ref={fileRef} type="file" accept="video/*" multiple style={{ display: 'none' }} onChange={handleUpload} />
      </div>

      {/* 안내 */}
      <div style={{ fontSize: '0.72rem', color: '#64748b', marginBottom: 12 }}>
        멀티스크린 비디오월 슬라이스는 파일명에 <code style={{ color: '#3b82f6' }}>wallsync</code> 를 포함하면
        플레이어가 자동으로 동기 재생합니다. (예: <code>wallsync_slice0.mp4</code>)
      </div>

      {/* 기기별 동기 미세조정 (위상 오프셋) */}
      {devices.length > 0 && (
        <div style={{ marginBottom: 14, padding: '10px 12px', background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#e2e8f0' }}>기기별 동기 미세조정 (위상 오프셋)</span>
            <div style={{ flex: 1 }} />
            <button onClick={autoAlign} style={{ padding: '4px 12px', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: '0.74rem', fontWeight: 700 }}>
              자동 정렬
            </button>
          </div>
          <div style={{ fontSize: '0.68rem', color: '#64748b', marginBottom: 10 }}>
            <b>자동 정렬</b>로 서버 측정 스큐 기준 시작점을 잡은 뒤, 화면을 보며 <b>늦은 화면</b>을 −, <b>빠른 화면</b>을 + 로 미세조정합니다.
            즉시 적용·기기에 저장됩니다. (<code style={{ color: '#94a3b8' }}>skew</code>=서버가 잰 시계차, 음수 클수록 뒤처짐)
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {devices.map((d) => {
              const cur = offsets[d.id] ?? 0;
              const slideName = (typeof d.slide === 'object' && d.slide) ? d.slide.filename : null;
              const isWall = /wallsync/i.test(slideName || '');
              return (
                <div key={d.id} style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <span style={{ minWidth: 72, fontSize: '0.78rem', color: isWall ? '#3b82f6' : '#cbd5e1', fontWeight: isWall ? 700 : 400 }}>{d.id}</span>
                  <span style={{ minWidth: 78, fontSize: '0.64rem', color: '#94a3b8' }}>
                    skew {Number.isFinite(d.clockSkew) ? `${d.clockSkew > 0 ? '+' : ''}${d.clockSkew}ms` : '—'}
                  </span>
                  {slideName && <span style={{ fontSize: '0.62rem', color: '#64748b', minWidth: 110, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={slideName}>{slideName}</span>}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    {[-50, -10, -1].map((s) => (
                      <button key={s} onClick={() => applyOffset(d.id, cur + s)} style={stepBtn}>{s}</button>
                    ))}
                    <span style={{ minWidth: 64, textAlign: 'center', fontSize: '0.82rem', fontWeight: 700, color: '#fbbf24' }}>{cur > 0 ? `+${cur}` : cur}ms</span>
                    {[1, 10, 50].map((s) => (
                      <button key={s} onClick={() => applyOffset(d.id, cur + s)} style={stepBtn}>+{s}</button>
                    ))}
                    <button onClick={() => applyOffset(d.id, 0)} style={{ ...stepBtn, color: '#f87171' }}>0</button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* 업로드 진행률 */}
      {uploadProgress && (
        <div style={{ marginBottom: 12, padding: '10px 14px', background: 'rgba(59,130,246,0.08)', border: '1px solid rgba(59,130,246,0.25)', borderRadius: 8 }}>
          <div style={{ fontSize: '0.78rem', color: '#cbd5e1', marginBottom: 6 }}>{uploadProgress.name} — {uploadProgress.pct}%</div>
          <div style={{ height: 6, background: '#1e293b', borderRadius: 3, overflow: 'hidden' }}>
            <div style={{ height: '100%', width: `${uploadProgress.pct}%`, background: '#3b82f6', transition: 'width 0.2s' }} />
          </div>
        </div>
      )}

      {/* 동영상 그리드 */}
      {!selectedStoreId ? (
        <div style={{ color: '#64748b', fontSize: '0.85rem', marginTop: 24 }}>사업장을 선택하면 업로드된 동영상이 표시됩니다.</div>
      ) : videos.length === 0 ? (
        <div style={{ color: '#64748b', fontSize: '0.85rem', marginTop: 24 }}>업로드된 동영상이 없습니다. 우측 상단 <b>동영상 업로드</b>로 추가하세요.</div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 14, overflowY: 'auto', paddingBottom: 16 }}>
          {videos.map((v) => {
            const isWall = /wallsync/i.test(v.filename || '');
            const url = (v.path || '').startsWith('http') ? v.path : `${API}${v.path}`;
            return (
              <div key={v.id} style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 10, overflow: 'hidden', position: 'relative' }}>
                <div style={{ aspectRatio: '16 / 9', background: '#000' }}>
                  <video src={url} muted loop playsInline preload="metadata" style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                    onMouseEnter={(e) => e.currentTarget.play().catch(() => {})}
                    onMouseLeave={(e) => { e.currentTarget.pause(); e.currentTarget.currentTime = 0; }} />
                </div>
                {isWall && (
                  <span style={{ position: 'absolute', top: 8, left: 8, padding: '2px 8px', background: '#3b82f6', color: '#fff', fontSize: '0.62rem', fontWeight: 700, borderRadius: 6, letterSpacing: '0.3px' }}>WALL SYNC</span>
                )}
                <button onClick={() => handleDelete(v.id, v.filename)} title="삭제"
                  style={{ position: 'absolute', top: 6, right: 6, width: 26, height: 26, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.55)', border: 'none', borderRadius: 6, color: '#f87171', cursor: 'pointer' }}>
                  <Trash2 size={14} />
                </button>
                <div style={{ padding: '8px 10px' }}>
                  <div style={{ fontSize: '0.78rem', color: '#e2e8f0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={v.filename}>{v.filename}</div>
                  {v.size ? <div style={{ fontSize: '0.66rem', color: '#64748b', marginTop: 2 }}>{fmtSize(v.size)}</div> : null}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default VideoWallManager;
