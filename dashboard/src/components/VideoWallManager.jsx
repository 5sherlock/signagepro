import React, { useState, useEffect, useRef, useCallback } from 'react';
import { SOCKET_URL, apiFetch } from '../config';
import { Upload, Trash2, Video } from 'lucide-react';

const API = SOCKET_URL;

const VIDEO_RE = /\.(mp4|webm|mov|mkv|avi)$/i;
const fmtSize = (b) => (!b ? '' : b < 1048576 ? `${(b / 1024).toFixed(0)}KB` : `${(b / 1048576).toFixed(1)}MB`);

/**
 * 동영상 메뉴 — 멀티스크린 비디오월용 동영상(슬라이스) 업로드/관리.
 * 업로드는 기존 미디어와 동일한 POST /api/media (multipart) 를 쓰되, 동영상만 노출.
 * 파일명에 "wallsync" 가 들어가면 플레이어가 wall-동기 비디오로 인식한다(배지 표시).
 */
const VideoWallManager = ({ stores = [], selectedStoreId, setSelectedStoreId }) => {
  const [videos, setVideos] = useState([]);
  const [uploadProgress, setUploadProgress] = useState(null); // null | { name, pct }
  const fileRef = useRef();

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
