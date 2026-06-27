import React, { useState, useEffect, useRef, useCallback } from 'react';
import { io } from 'socket.io-client';
import { SOCKET_URL, apiFetch, getToken } from '../config';
import { Upload, Trash2, Video, Scissors, X, Play, Square, MonitorPlay, SlidersHorizontal, RotateCcw } from 'lucide-react';
import { encodeWall, webcodecsSupported } from '../wallEncode';

const API = SOCKET_URL;

// "3840x2160" → { w, h } (실패 시 null)
const parseRes = (s) => {
  const m = /^(\d+)\s*[x×]\s*(\d+)/i.exec(String(s || '').trim());
  return m ? { w: parseInt(m[1]), h: parseInt(m[2]) } : null;
};

const VIDEO_RE = /\.(mp4|webm|mov|mkv|avi)$/i;
const fmtSize = (b) => (!b ? '' : b < 1048576 ? `${(b / 1024).toFixed(0)}KB` : `${(b / 1048576).toFixed(1)}MB`);
const fmtTime = (s) => { if (!isFinite(s) || s < 0) return '0:00'; const m = Math.floor(s / 60); const ss = Math.floor(s % 60); return `${m}:${String(ss).padStart(2, '0')}`; };

// 파일명에서 비디오월 세트(베이스명)와 슬라이스 인덱스 추출: "osulloc_wallsync_slice2.mp4" → base=osulloc, idx=2
// greedy(.*) — 이름에 wallsync_slice가 여러 번이면 '마지막' 것을 슬라이스 기준으로(앞부분 전체가 base).
const WALL_SLICE_RE = /^(.*)[_-]?wallsync[_-]?slice(\d+)/i;
// 비디오월 슬라이스를 세트별로 묶고(슬라이스는 좌→우=인덱스 오름차순), 새 세트를 먼저. 일반 동영상은 분리.
const groupVideos = (list) => {
  const setMap = new Map(); // base -> { base, items, newest }
  const others = [];
  for (const v of list) {
    const m = WALL_SLICE_RE.exec(v.filename || '');
    if (m) {
      const base = (m[1] || '').replace(/[_.-]+$/, '') || '(이름 없음)';
      if (!setMap.has(base)) setMap.set(base, { base, items: [], newest: 0 });
      const g = setMap.get(base);
      g.items.push({ ...v, _idx: parseInt(m[2]) });
      const t = v.createdAt ? new Date(v.createdAt).getTime() : 0;
      if (t > g.newest) g.newest = t;
    } else {
      others.push(v);
    }
  }
  const sets = [...setMap.values()];
  sets.forEach((g) => g.items.sort((a, b) => a._idx - b._idx)); // 좌→우
  sets.sort((a, b) => b.newest - a.newest);                     // 새 세트 먼저
  return { sets, others };
};

// 영상 URL의 자연 길이(초) 측정 — 재생목록 슬롯 duration용 (실패 시 10)
const getVideoDuration = (url) => new Promise((resolve) => {
  const v = document.createElement('video');
  v.preload = 'metadata';
  v.onloadedmetadata = () => resolve(Math.max(1, Math.round(v.duration) || 10));
  v.onerror = () => resolve(10);
  v.src = url;
});

const stepBtn = {
  minWidth: 30, padding: '3px 6px', background: 'rgba(255,255,255,0.06)', color: '#cbd5e1',
  border: '1px solid rgba(255,255,255,0.12)', borderRadius: 5, cursor: 'pointer', fontSize: '0.72rem', fontWeight: 600,
};

const setBtn = {
  display: 'inline-flex', alignItems: 'center', gap: 4, padding: '4px 10px', background: 'rgba(255,255,255,0.06)',
  color: '#cbd5e1', border: '1px solid rgba(255,255,255,0.14)', borderRadius: 6, cursor: 'pointer', fontSize: '0.72rem', fontWeight: 600,
};

const lbl = { display: 'block', fontSize: '0.7rem', color: '#94a3b8', marginBottom: 5, fontWeight: 600 };
// 해상도 프리셋 버튼 — active면 강조
const presetBtn = (active) => ({
  padding: '4px 9px', borderRadius: 6, cursor: 'pointer', fontSize: '0.68rem', fontWeight: 600,
  background: active ? 'rgba(139,92,246,0.2)' : 'rgba(255,255,255,0.05)',
  color: active ? '#c4b5fd' : '#cbd5e1',
  border: `1px solid ${active ? 'rgba(139,92,246,0.5)' : 'rgba(255,255,255,0.12)'}`,
});
const inp = {
  width: '100%', padding: '7px 10px', background: 'rgba(255,255,255,0.05)', color: '#e2e8f0',
  border: '1px solid rgba(255,255,255,0.12)', borderRadius: 7, fontSize: '0.82rem', boxSizing: 'border-box',
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

  // 미리보기 동기 + 타임라인 — 세트 첫 칸(리더)에 나머지를 맞춤(스크럽 가능). 재생 중일 때만 보정. 리더 위치를 타임라인에 표시.
  const [playInfo, setPlayInfo] = useState({}); // base -> { cur, dur, paused }
  useEffect(() => {
    const id = setInterval(() => {
      const info = {};
      Object.entries(setRefs.current).forEach(([base, el]) => {
        if (!el) return;
        const vids = el.querySelectorAll('video');
        if (!vids.length) return;
        const leader = vids[0];
        if (isFinite(leader.duration)) {
          info[base] = { cur: leader.currentTime, dur: leader.duration, paused: leader.paused };
          // 정지/재생 무관하게 항상 리더 위치로 정렬 → 모든 칸이 같은 프레임(벽 일치)
          const t = leader.currentTime;
          for (let i = 1; i < vids.length; i++) {
            const v = vids[i];
            if (!isFinite(v.duration)) continue;
            if (Math.abs(v.currentTime - t) > 0.18) { try { v.currentTime = t; } catch (_) {} }
            if (!leader.paused && v.paused) { try { v.play(); } catch (_) {} } // 리더 재생 중인데 빠진 칸 합류
          }
        }
      });
      setPlayInfo(info);
    }, 350);
    return () => clearInterval(id);
  }, []);

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
    if (!window.confirm(`'${filename}'을(를) 삭제합니다.\n⚠ 재생목록(기기 화면)에 배포돼 있으면 거기서도 함께 제거되며, 되돌릴 수 없습니다.\n계속할까요?`)) return;
    await apiFetch(`${API}/api/media/${id}`, { method: 'DELETE' });
    fetchVideos();
  };

  // ── 비디오월 세트 단위 동작 (그리드) ─────────────────────────────────────────
  const setRefs = useRef({}); // base -> 세트 그리드 컨테이너 el
  const playSet = (base) => {
    const el = setRefs.current[base];
    if (el) el.querySelectorAll('video').forEach((v) => { try { v.play(); } catch (e) {} }); // 멈춘 지점부터 이어 재생(리더 기준 동기)
  };
  const stopSet = (base) => {
    const el = setRefs.current[base];
    if (el) el.querySelectorAll('video').forEach((v) => { v.pause(); }); // 현재 위치에서 정지(되감기 안 함)
  };
  const handleDeleteSet = async (g) => {
    if (!window.confirm(`'${g.base}' 세트의 슬라이스 ${g.items.length}개를 삭제합니다.\n⚠ 이 슬라이스가 배포된 재생목록(기기 화면)에서도 함께 제거되며, 되돌릴 수 없습니다.\n계속할까요?`)) return;
    for (const v of g.items) {
      await apiFetch(`${API}/api/media/${v.id}`, { method: 'DELETE' }).catch(() => {});
    }
    fetchVideos();
  };

  // ── 재생목록 배정 (드래그앤드롭: 슬라이스 → 기기 슬롯) ────────────────────────
  const [wallOrder, setWallOrder] = useState({}); // base -> [sliceId|null, ...] (index=기기 위치)
  const [deploying, setDeploying] = useState(null); // 배포 중인 세트 base
  const [deployMsg, setDeployMsg] = useState({});   // base -> { type:'ok'|'error', text }
  // 되돌리기 백업: base -> { groupId: prevItems }. 메뉴 이동/새로고침에도 유지되도록 localStorage 영속.
  const [lastBackup, setLastBackup] = useState(() => {
    try { return JSON.parse(localStorage.getItem('SIGNAGE_WALL_BACKUP') || '{}'); } catch (e) { return {}; }
  });
  useEffect(() => {
    try { localStorage.setItem('SIGNAGE_WALL_BACKUP', JSON.stringify(lastBackup)); } catch (e) {}
  }, [lastBackup]);
  const [showOffset, setShowOffset] = useState(false); // 동기 미세조정(위상 오프셋) 모달

  // 칸 수 = max(기기 수, 슬라이스 수). 기기가 더 많으면 오른쪽이 빈 칸.
  const rowCells = (g) => Math.max(devices.length, g.items.length);
  const buildDefaultOrder = (g) => {
    const arr = new Array(rowCells(g)).fill(null);
    g.items.forEach((s, i) => { arr[i] = s.id; });
    return arr;
  };
  const effOrder = (g) => {
    const n = rowCells(g);
    const saved = wallOrder[g.base];
    if (!saved) return buildDefaultOrder(g);
    const arr = saved.slice(0, n);
    while (arr.length < n) arr.push(null);
    return arr;
  };
  // 칸 from -> 칸 to 내용 swap (순서 바꾸기)
  const swapCells = (g, from, to) => {
    if (from === to || from == null || to == null) return;
    setWallOrder((prev) => {
      const n = rowCells(g);
      let arr = prev[g.base] ? prev[g.base].slice(0, n) : buildDefaultOrder(g);
      while (arr.length < n) arr.push(null);
      arr = [...arr];
      const t = arr[to]; arr[to] = arr[from]; arr[from] = t;
      return { ...prev, [g.base]: arr };
    });
  };
  // 칸 순서를 기본값(슬라이스 좌→우 그대로)으로 복원
  const resetOrder = (g) => {
    setWallOrder((prev) => { const n = { ...prev }; delete n[g.base]; return n; });
    setDeployMsg((m) => ({ ...m, [g.base]: null }));
  };
  // 타임라인 클릭 → 세트의 모든 칸을 그 위치로 이동(스크럽)
  const seekSet = (g, clientX, barEl) => {
    const el = setRefs.current[g.base];
    if (!el) return;
    const vids = el.querySelectorAll('video');
    if (!vids.length || !isFinite(vids[0].duration)) return;
    const rect = barEl.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    const t = ratio * vids[0].duration;
    vids.forEach((v) => { try { v.currentTime = t; } catch (_) {} });
    setPlayInfo((p) => ({ ...p, [g.base]: { ...(p[g.base] || {}), cur: t } }));
  };
  // 타임라인 드래그 스크럽 — 누른 채 움직이면 전 칸이 실시간으로 같이 이동
  const startScrub = (g, e) => {
    e.preventDefault();
    const barEl = e.currentTarget;
    seekSet(g, e.clientX, barEl);
    const onMove = (ev) => seekSet(g, ev.clientX, barEl);
    const onUp = () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp); };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
  };

  // 현재 칸 배치대로 배포: 칸 i 슬라이스 -> devices[i] (빈 칸이면 그 기기의 wall 항목 제거)
  const deploySet = async (g) => {
    const order = effOrder(g);
    const pairs = []; const clearIds = [];
    order.forEach((sliceId, i) => {
      const dev = devices[i];
      if (!dev) return;
      if (sliceId) { const slice = g.items.find((s) => s.id === sliceId); if (slice) pairs.push({ deviceId: dev.id, slice, dev }); }
      else clearIds.push({ deviceId: dev.id, dev });
    });
    if (pairs.length === 0) { setDeployMsg((m) => ({ ...m, [g.base]: { type: 'error', text: '배정된 칸이 없습니다.' } })); return; }
    if (!window.confirm(`현재 배치대로 ${pairs.length}개 기기의 재생목록을 덮어씁니다.\n해당 기기의 기존 영상은 교체됩니다. 진행할까요?\n(배포 후 '되돌리기'로 복구 가능)`)) return;
    setDeploying(g.base);
    setDeployMsg((m) => ({ ...m, [g.base]: null }));
    try {
      const enriched = [];
      for (const p of pairs) {
        if (!p.dev.groupId) throw new Error(`${p.dev.name || p.deviceId}: 그룹 미지정으로 배정 불가`);
        const url = (p.slice.path || '').startsWith('http') ? p.slice.path : `${API}${p.slice.path}`;
        const duration = await getVideoDuration(url);
        enriched.push({ ...p, groupId: p.dev.groupId, duration });
      }
      const addByGroup = enriched.reduce((acc, e) => { (acc[e.groupId] ||= []).push(e); return acc; }, {});
      const clearByGroup = {};
      clearIds.forEach((c) => { if (c.dev.groupId) (clearByGroup[c.dev.groupId] ||= new Set()).add(c.deviceId); });
      const groups = new Set([...Object.keys(addByGroup), ...Object.keys(clearByGroup)]);
      const backup = {}; // 되돌리기용: 그룹별 직전 전체 재생목록
      for (const groupId of groups) {
        const adds = addByGroup[groupId] || [];
        const managed = new Set([...adds.map((e) => e.deviceId), ...((clearByGroup[groupId] && [...clearByGroup[groupId]]) || [])]);
        const res = await apiFetch(`${API}/api/groups/${groupId}/playlist`);
        const data = await res.json().catch(() => ({ medias: [] }));
        const existing = Array.isArray(data.medias) ? data.medias : [];
        const toItem = (it) => ({ mediaId: it.mediaId, duration: it.duration || 10, targetDeviceId: it.targetDeviceId || null, transition: it.transition || 'none', transitionTime: it.transitionTime || 0, slideDirection: it.slideDirection || 'right' });
        backup[groupId] = existing.map(toItem); // 덮어쓰기 전 원본 보관
        const kept = existing.filter((it) => !(it.targetDeviceId && managed.has(it.targetDeviceId))).map(toItem);
        const added = adds.map((e) => ({ mediaId: e.slice.id, duration: e.duration, targetDeviceId: e.deviceId, transition: 'none', transitionTime: 0, slideDirection: 'right' }));
        const save = await apiFetch(`${API}/api/groups/${groupId}/playlist`, { method: 'POST', body: JSON.stringify({ items: [...kept, ...added] }) });
        if (!save.ok) { const er = await save.json().catch(() => ({})); throw new Error(er.error || '재생목록 저장 실패'); }
      }
      // 기본 = 전 기기 음소거(의도치 않은 사운드/에코 방지). 모든 슬라이스에 오디오가 있으므로
      // 출력할 기기를 관제에서 음소거 해제(볼륨↑)해 선택하면 된다.
      for (const p of pairs) {
        apiFetch(`${API}/api/devices/${p.deviceId}/volume`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ level: 0 }) }).catch(() => {});
      }
      setLastBackup((b) => ({ ...b, [g.base]: backup }));
      setDeployMsg((m) => ({ ...m, [g.base]: { type: 'ok', text: `${pairs.length}개 기기에 배포 완료 (즉시 반영, 기본 음소거 — 관제에서 출력 기기 선택)` } }));
      fetchDevices();
    } catch (err) {
      setDeployMsg((m) => ({ ...m, [g.base]: { type: 'error', text: err.message } }));
    } finally {
      setDeploying(null);
    }
  };

  // 직전 배포 되돌리기 (배포 전 보관한 원본 재생목록으로 복구)
  const undoDeploy = async (g) => {
    const backup = lastBackup[g.base];
    if (!backup) return;
    if (!window.confirm('직전 배포를 되돌려 이전 재생목록으로 복구합니다. 진행할까요?')) return;
    setDeploying(g.base);
    try {
      for (const [groupId, items] of Object.entries(backup)) {
        const save = await apiFetch(`${API}/api/groups/${groupId}/playlist`, { method: 'POST', body: JSON.stringify({ items }) });
        if (!save.ok) { const er = await save.json().catch(() => ({})); throw new Error(er.error || '복구 실패'); }
      }
      setLastBackup((b) => { const n = { ...b }; delete n[g.base]; return n; });
      setDeployMsg((m) => ({ ...m, [g.base]: { type: 'ok', text: '되돌렸습니다. (이전 재생목록으로 복구)' } }));
      fetchDevices();
    } catch (err) {
      setDeployMsg((m) => ({ ...m, [g.base]: { type: 'error', text: err.message } }));
    } finally {
      setDeploying(null);
    }
  };

  // ── 비디오월 가공(업스케일/크롭/N분할) ──────────────────────────────────────
  const [showProc, setShowProc] = useState(false);
  const [proc, setProc] = useState({ deviceCount: 1, sliceWidth: 3840, sliceHeight: 2160, yOffsetPct: 50, baseName: '' });
  const [procFile, setProcFile] = useState(null);
  const [procSrcUrl, setProcSrcUrl] = useState(null); // 원본 미리보기 objectURL
  const [procSrcDim, setProcSrcDim] = useState(null); // { w, h } 원본 해상도(메타 로드 후)
  const [procMediaId, setProcMediaId] = useState(null);   // 라이브러리에서 고른 원본 mediaId (파일 업로드 대신)
  const [procMediaName, setProcMediaName] = useState(''); // 그 표시명
  const [slcManual, setSlcManual] = useState(false); // 슬라이스 해상도 수동 편집 여부(프리필 덮어쓰기 방지)
  const [job, setJob] = useState(null); // null | { jobId?, status, pct }
  const [cropPaused, setCropPaused] = useState(false); // 크롭 미리보기 재생/정지 상태
  const [cropProg, setCropProg] = useState({ cur: 0, dur: 0 }); // 크롭 미리보기 재생 위치
  const procFileRef = useRef();
  const cropBoxRef = useRef(null); // 크롭 미리보기 컨테이너(드래그 좌표 기준)
  const cropVidRef = useRef(null); // 크롭 미리보기 video
  const cropSeekTarget = useRef(null); // 스크럽 목표 시간(코얼레싱)
  const cropSeeking = useRef(false);   // 시킹 진행 중 플래그
  const overlayDown = useRef(false); // 배경 클릭 판별: mousedown이 오버레이에서 시작됐는지 (텍스트 드래그가 밖에서 끝나도 안 닫히게)

  // 기기들이 보고한 최대 해상도(첫 감지값). stbSpec → tvEdid 순.
  const detectRes = useCallback(() => {
    for (const d of devices) {
      const res = parseRes(d.stbSpec?.maxRes) || parseRes(d.tvEdid?.maxRes);
      if (res) return res;
    }
    return null;
  }, [devices]);

  // 메타 자동 프리필: 온라인 기기 수 → 가로 대수. 슬라이스 해상도는 수동 편집 전에만 감지값으로 채움.
  const prefillFromMeta = useCallback(() => {
    const count = Math.max(1, devices.length);
    setProc((p) => {
      const next = { ...p, deviceCount: count };
      if (!slcManual) {
        const res = detectRes();
        next.sliceWidth = res?.w || 3840;
        next.sliceHeight = res?.h || 2160;
      }
      return next;
    });
  }, [devices, slcManual, detectRes]);

  const openProc = () => {
    if (!selectedStoreId) { alert('먼저 사업장을 선택하세요.'); return; }
    setProcFile(null);
    setProcMediaId(null); setProcMediaName(''); setProcSrcUrl(null); setProcSrcDim(null);
    setJob(null);
    prefillFromMeta();
    setShowProc(true);
  };

  // 가공 진행률 소켓 — 모달 열려 있을 때만 연결
  useEffect(() => {
    if (!showProc) return;
    const socket = io(SOCKET_URL, { auth: { token: getToken() } });
    socket.on('vw_progress', ({ jobId, pct, status }) => {
      setJob((j) => (j && j.jobId === jobId ? { ...j, pct, status } : j));
    });
    socket.on('vw_done', ({ jobId, slices }) => {
      setJob((j) => (j && j.jobId === jobId ? { ...j, status: 'done', pct: 100, slices } : j));
      fetchVideos();
    });
    socket.on('vw_error', ({ jobId, error }) => {
      setJob((j) => (j && j.jobId === jobId ? { ...j, status: 'error', error } : j));
    });
    return () => socket.disconnect();
  }, [showProc, fetchVideos]);

  // 가공 완료 시 모달 자동 닫기(잠깐 100% 보여준 뒤)
  useEffect(() => {
    if (job?.status !== 'done') return;
    const t = setTimeout(() => { setShowProc(false); setJob(null); }, 1500);
    return () => clearTimeout(t);
  }, [job?.status]);

  // 원본 파일 → 미리보기 objectURL. (라이브러리 선택 모드면 procFile=null이라 여기선 안 건드림)
  useEffect(() => {
    if (!procFile) return;
    const url = URL.createObjectURL(procFile);
    setProcSrcUrl(url); setProcSrcDim(null); setCropPaused(false);
    return () => URL.revokeObjectURL(url);
  }, [procFile]);

  // 원본 선택: 디스크 파일 ↔ 라이브러리 (서로 배타 — 하나 고르면 다른 쪽 해제)
  const pickFile = (file) => {
    setProcMediaId(null); setProcMediaName('');
    setProcFile(file);
    if (!file) { setProcSrcUrl(null); setProcSrcDim(null); }
  };
  const pickLibrary = (m) => {
    setProcFile(null);
    if (!m) { setProcMediaId(null); setProcMediaName(''); setProcSrcUrl(null); setProcSrcDim(null); return; }
    setProcMediaId(m.id); setProcMediaName(m.filename);
    setProcSrcUrl((m.path || '').startsWith('http') ? m.path : `${API}${m.path}`);
    setProcSrcDim(null); setCropPaused(false);
  };

  const startProc = () => {
    if (!procFile && !procMediaId) { alert('가공할 원본 동영상을 선택하세요 (파일 업로드 또는 라이브러리).'); return; }
    const n = parseInt(proc.deviceCount);
    if (!Number.isInteger(n) || n < 1 || n > 12) { alert('가로 대수는 1~12 사이여야 합니다.'); return; }
    const form = new FormData();
    form.append('storeId', selectedStoreId);
    form.append('deviceCount', String(n));
    form.append('sliceWidth', String(proc.sliceWidth));
    form.append('sliceHeight', String(proc.sliceHeight));
    form.append('yOffsetPct', String(proc.yOffsetPct));
    if (proc.baseName) form.append('baseName', proc.baseName);
    if (procMediaId) form.append('mediaId', procMediaId); // 라이브러리 원본 (재업로드 없음)
    else form.append('file', procFile);

    setJob({ status: 'uploading', pct: 0 });
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${API}/api/videowall/process`);
    const token = getToken();
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    xhr.upload.onprogress = (ev) => {
      if (ev.lengthComputable) setJob((j) => ({ ...j, status: 'uploading', pct: Math.round((ev.loaded / ev.total) * 100) }));
    };
    xhr.onload = () => {
      if (xhr.status >= 400) {
        let msg = `업로드 실패 (${xhr.status})`;
        try { msg = JSON.parse(xhr.responseText).error || msg; } catch (e) {}
        setJob({ status: 'error', error: msg });
        return;
      }
      const { jobId } = JSON.parse(xhr.responseText);
      // 업로드 완료 → 서버 가공 시작. 이후 진행률은 소켓이 채움.
      setJob({ jobId, status: 'probing', pct: 0 });
    };
    xhr.onerror = () => setJob({ status: 'error', error: '네트워크 오류' });
    xhr.send(form);
  };

  const procAbortRef = useRef(null);
  const cancelProc = () => {
    if (procAbortRef.current) procAbortRef.current.abort();      // 브라우저 가공 중단
    if (job?.jobId) apiFetch(`${API}/api/videowall/jobs/${job.jobId}/cancel`, { method: 'POST' }).catch(() => {}); // 서버 가공 중단
    setJob(null);
  };

  // 브라우저(PC GPU)에서 직접 인코딩 → 슬라이스 mp4를 /api/media 로 업로드. NAS 부담 없음.
  const startProcLocal = async () => {
    if (!procFile && !procMediaId) { alert('가공할 원본 동영상을 선택하세요 (파일 업로드 또는 라이브러리).'); return; }
    if (!webcodecsSupported()) {
      const insecure = typeof window !== 'undefined' && !window.isSecureContext;
      alert(insecure
        ? 'WebCodecs는 보안 컨텍스트(HTTPS 또는 localhost)에서만 동작합니다.\n현재 http+IP 접속이라 비활성화됨.\n\n→ dev PC에서 http://localhost:3001 로 접속하거나, HTTPS(예: Tailscale Serve)로 접속하세요.\n(대신 아래 "서버 가공"도 가능합니다)'
        : '이 브라우저는 WebCodecs를 지원하지 않습니다. Chrome/Edge 최신 버전에서 사용하세요.\n(대신 아래 "서버 가공"도 가능합니다)');
      return;
    }
    const n = parseInt(proc.deviceCount);
    if (!Number.isInteger(n) || n < 1 || n > 12) { alert('가로 대수는 1~12 사이여야 합니다.'); return; }
    const sliceWidth = parseInt(proc.sliceWidth), sliceHeight = parseInt(proc.sliceHeight);
    if (!sliceWidth || !sliceHeight) { alert('슬라이스 해상도를 확인하세요.'); return; }
    const rawName = proc.baseName || (procMediaName || procFile?.name || 'wall').replace(/\.[^.]+$/, '');
    const base = (rawName.replace(/[_-]?wallsync.*$/i, '').replace(/[^\w가-힣.-]+/g, '_').replace(/_+$/, '')) || 'wall';
    const src = procFile || procSrcUrl;
    if (!src) { alert('원본을 불러올 수 없습니다.'); return; }

    const ctrl = new AbortController();
    procAbortRef.current = ctrl;
    setJob({ status: 'encoding', pct: 0 });
    try {
      const slices = await encodeWall({
        src, deviceCount: n, sliceWidth, sliceHeight,
        yOffsetPct: parseFloat(proc.yOffsetPct) || 0,
        onProgress: (p) => setJob((j) => (j && j.status === 'encoding' ? { ...j, pct: Math.round(p * 100) } : j)),
        signal: ctrl.signal,
      });
      for (let i = 0; i < slices.length; i++) {
        setJob({ status: 'registering', pct: Math.round((i / slices.length) * 100) });
        const f = new File([slices[i].blob], `${base}_wallsync_slice${slices[i].index}.mp4`, { type: 'video/mp4' });
        await uploadOne(f);
      }
      setJob({ status: 'done', pct: 100 });
      setUploadProgress(null);
      fetchVideos();
    } catch (e) {
      setJob({ status: 'error', error: e.message || '가공 실패' });
      setUploadProgress(null);
    } finally {
      procAbortRef.current = null;
    }
  };

  const wallW = (parseInt(proc.sliceWidth) || 0) * (parseInt(proc.deviceCount) || 0);
  const procBusy = job && !['done', 'error'].includes(job.status);
  const STATUS_LABEL = { uploading: '업로드 중', queued: '대기 중', probing: '분석 중', encoding: '가공(인코딩) 중', registering: '등록 중', done: '완료', error: '실패' };

  // 서버 크롭 수식 역산: scale(cover) → 가로 중앙 + 세로 yOffset 크롭. 원본 기준 사각형(%)을 반환.
  const cropRect = (() => {
    if (!procSrcDim) return null;
    const sw = procSrcDim.w, sh = procSrcDim.h;
    const cw = wallW, ch = parseInt(proc.sliceHeight) || 0;
    if (!sw || !sh || !cw || !ch) return null;
    const factor = Math.max(cw / sw, ch / sh); // 캔버스를 덮도록 업스케일
    const cropW = cw / factor, cropH = ch / factor; // 원본 픽셀 기준 크롭 크기
    const y = Math.max(0, Math.min(100, parseFloat(proc.yOffsetPct) || 0)) / 100;
    const xSrc = (sw - cropW) / 2, ySrc = (sh - cropH) * y;
    return { leftPct: (xSrc / sw) * 100, topPct: (ySrc / sh) * 100, wPct: (cropW / sw) * 100, hPct: (cropH / sh) * 100 };
  })();

  // 크롭 사각형 세로 드래그 → yOffsetPct 갱신 (가로는 항상 중앙 고정이라 세로만)
  const startCropDrag = (e) => {
    if (procBusy || !cropRect) return;
    e.preventDefault();
    const box = cropBoxRef.current;
    const hFrac = cropRect.hPct / 100;
    if (!box || hFrac >= 1) return; // 세로가 꽉 차면 조절 불가
    const move = (ev) => {
      const rect = box.getBoundingClientRect();
      const rel = (ev.clientY - rect.top) / rect.height;           // 마우스 세로 위치 0~1
      const top = Math.max(0, Math.min(1 - hFrac, rel - hFrac / 2)); // 사각형 top 분율(중앙을 커서에)
      setProc((p) => ({ ...p, yOffsetPct: Math.round((top / (1 - hFrac)) * 100) }));
    };
    move(e);
    const up = () => { window.removeEventListener('mousemove', move); window.removeEventListener('mouseup', up); };
    window.addEventListener('mousemove', move);
    window.addEventListener('mouseup', up);
  };

  // 진행 중 시킹이 끝나면 최신 목표로 한 번 더 시킹(코얼레싱) → 시킹 폭주 방지로 매끄럽게
  const flushCropSeek = () => {
    const v = cropVidRef.current;
    if (!v || cropSeeking.current || cropSeekTarget.current == null || !isFinite(v.duration)) return;
    const t = cropSeekTarget.current;
    cropSeekTarget.current = null;
    if (Math.abs(v.currentTime - t) < 0.02) return;
    cropSeeking.current = true;
    try { v.currentTime = t; } catch (_) { cropSeeking.current = false; }
  };

  // 크롭 미리보기 재생 위치 스크럽 — 바 드래그/클릭으로 원본 재생 위치 이동
  const startCropSeek = (e) => {
    const bar = e.currentTarget;
    const v0 = cropVidRef.current;
    const wasPlaying = v0 && !v0.paused;
    if (v0 && wasPlaying) v0.pause(); // 스크럽 동안 정지 → 재생과 시킹 충돌 방지
    const move = (ev) => {
      const v = cropVidRef.current;
      if (!v || !isFinite(v.duration)) return;
      const rect = bar.getBoundingClientRect();
      const ratio = Math.max(0, Math.min(1, (ev.clientX - rect.left) / rect.width));
      cropSeekTarget.current = ratio * v.duration;
      setCropProg({ cur: cropSeekTarget.current, dur: v.duration }); // 핸들은 즉시 따라감
      flushCropSeek();
    };
    move(e);
    const up = () => {
      window.removeEventListener('mousemove', move);
      window.removeEventListener('mouseup', up);
      const v = cropVidRef.current;
      if (v && wasPlaying) { v.play().catch(() => {}); setCropPaused(false); } // 놓으면 재생 복귀
    };
    window.addEventListener('mousemove', move);
    window.addEventListener('mouseup', up);
  };

  // 동영상 카드 1개 — 비디오월 슬라이스면 sliceIdx(#0,#1…) 배지, 아니면 WALL SYNC/일반.
  const renderCard = (v, sliceIdx, flush, dragProps) => {
    const isWall = /wallsync/i.test(v.filename || '');
    const url = (v.path || '').startsWith('http') ? v.path : `${API}${v.path}`;
    return (
      <div key={v.id} {...(dragProps || {})} style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: flush ? 0 : 10, overflow: 'hidden', position: 'relative', cursor: dragProps ? 'grab' : 'default' }}>
        <div style={{ aspectRatio: '16 / 9', background: '#000' }}>
          <video src={url} muted loop playsInline preload="metadata" style={{ width: '100%', height: '100%', objectFit: 'contain' }}
            onMouseEnter={(e) => e.currentTarget.play().catch(() => {})}
            onMouseLeave={(e) => { e.currentTarget.pause(); e.currentTarget.currentTime = 0; }} />
        </div>
        {sliceIdx != null ? (
          <span style={{ position: 'absolute', top: 8, left: 8, padding: '2px 8px', background: '#3b82f6', color: '#fff', fontSize: '0.62rem', fontWeight: 700, borderRadius: 6 }}>#{sliceIdx}</span>
        ) : isWall ? (
          <span style={{ position: 'absolute', top: 8, left: 8, padding: '2px 8px', background: '#3b82f6', color: '#fff', fontSize: '0.62rem', fontWeight: 700, borderRadius: 6, letterSpacing: '0.3px' }}>WALL SYNC</span>
        ) : null}
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
          onClick={openProc}
          disabled={!selectedStoreId}
          title="원본 동영상 1개를 업스케일·크롭 후 가로 대수만큼 슬라이스로 분할해 자동 등록"
          style={{
            display: 'flex', alignItems: 'center', gap: 6, padding: '8px 16px',
            background: selectedStoreId ? '#8b5cf6' : '#334155', color: '#fff',
            border: 'none', borderRadius: 8, cursor: selectedStoreId ? 'pointer' : 'not-allowed',
            fontSize: '0.85rem', fontWeight: 600,
          }}
        >
          <Scissors size={16} /> 비디오월 가공
        </button>
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

      {/* 동영상 목록 — 비디오월은 세트별 한 줄(좌→우 슬라이스), 새 세트 먼저 */}
      {!selectedStoreId ? (
        <div style={{ color: '#64748b', fontSize: '0.85rem', marginTop: 24 }}>사업장을 선택하면 업로드된 동영상이 표시됩니다.</div>
      ) : videos.length === 0 ? (
        <div style={{ color: '#64748b', fontSize: '0.85rem', marginTop: 24 }}>업로드된 동영상이 없습니다. 우측 상단 <b>동영상 업로드</b>로 추가하세요.</div>
      ) : (() => {
        const { sets, others } = groupVideos(videos);
        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18, overflowY: 'auto', paddingBottom: 16 }}>
            {sets.map((g) => (
              <div key={g.base}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                  <span style={{ padding: '2px 8px', background: 'rgba(59,130,246,0.15)', border: '1px solid rgba(59,130,246,0.35)', color: '#93c5fd', fontSize: '0.66rem', fontWeight: 700, borderRadius: 6 }}>비디오월</span>
                  <span style={{ fontSize: '0.82rem', color: '#e2e8f0', fontWeight: 600 }} title={g.base}>{g.base}</span>
                  <span style={{ fontSize: '0.7rem', color: '#64748b' }}>슬라이스 {g.items.length}개 · 좌→우</span>
                  <div style={{ flex: 1 }} />
                  <button onClick={() => resetOrder(g)} title="칸 순서를 기본(슬라이스 좌→우)으로 되돌리기" style={setBtn}><RotateCcw size={13} /> 순서 원래대로</button>
                  <button onClick={() => deploySet(g)} disabled={deploying === g.base} title="아래 배정대로 재생목록에 배포" style={{ ...setBtn, color: '#c4b5fd', borderColor: 'rgba(139,92,246,0.45)', cursor: deploying === g.base ? 'wait' : 'pointer' }}><MonitorPlay size={13} /> {deploying === g.base ? '배포 중…' : '배포'}</button>
                  {lastBackup[g.base] && (
                    <button onClick={() => undoDeploy(g)} disabled={deploying === g.base} title="직전 배포 취소(이전 재생목록 복구)" style={{ ...setBtn, color: '#fbbf24', borderColor: 'rgba(251,191,36,0.45)' }}><RotateCcw size={13} /> 되돌리기</button>
                  )}
                  <button onClick={() => playSet(g.base)} title="세트 동시 재생" style={setBtn}><Play size={13} /> 재생</button>
                  <button onClick={() => stopSet(g.base)} title="정지" style={setBtn}><Square size={12} /> 정지</button>
                  <button onClick={() => setShowOffset(true)} title="기기별 동기 미세조정(위상 오프셋)" style={setBtn}><SlidersHorizontal size={13} /> 동기 미세조정</button>
                  <button onClick={() => handleDeleteSet(g)} title="세트 전체 삭제" style={{ ...setBtn, color: '#fca5a5', borderColor: 'rgba(248,113,113,0.4)' }}><Trash2 size={13} /> 세트 삭제</button>
                </div>
                {/* 슬라이스(드래그 소스) — '5개 기준' 크기 고정. 직각 맞붙임. */}
                <div style={{ fontSize: '0.66rem', color: '#64748b', marginBottom: 6 }}>
                  칸을 드래그해 순서를 바꾸세요. 칸 위치 = 모니터(아래 기기명). 기기가 더 많으면 오른쪽이 빈 칸. · 배치 후 <b>배포</b>
                </div>
                {(() => {
                  const order = effOrder(g);
                  const n = order.length || 1;
                  return (
                    <div ref={(el) => { setRefs.current[g.base] = el; }} style={{ width: `${Math.min(100, (n / 5) * 100)}%`, display: 'grid', gridTemplateColumns: `repeat(${n}, 1fr)`, gap: 0, border: '1px solid rgba(255,255,255,0.1)' }}>
                      {order.map((sliceId, i) => {
                        const slice = sliceId ? g.items.find((s) => s.id === sliceId) : null;
                        const sUrl = slice ? ((slice.path || '').startsWith('http') ? slice.path : `${API}${slice.path}`) : null;
                        const dev = devices[i];
                        return (
                          <div key={sliceId ?? `empty-${i}`}
                            draggable={!!slice}
                            onDragStart={(e) => { if (slice) { e.dataTransfer.setData('wallcell', JSON.stringify({ base: g.base, from: i })); e.dataTransfer.effectAllowed = 'move'; } }}
                            onDragOver={(e) => { e.preventDefault(); e.dataTransfer.dropEffect = 'move'; }}
                            onDrop={(e) => { e.preventDefault(); try { const data = JSON.parse(e.dataTransfer.getData('wallcell')); if (data.base === g.base) swapCells(g, data.from, i); } catch (_) {} }}
                            style={{ position: 'relative', background: '#000', cursor: slice ? 'grab' : 'default', borderRight: i < n - 1 ? '1px solid rgba(255,255,255,0.12)' : 'none', outline: slice ? 'none' : '1px dashed rgba(255,255,255,0.18)', outlineOffset: -3 }}>
                            <div style={{ aspectRatio: '16 / 9', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                              {slice ? (
                                <video src={sUrl} muted loop playsInline preload="metadata" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                              ) : (
                                <span style={{ color: '#475569', fontSize: '0.68rem' }}>빈 칸</span>
                              )}
                            </div>
                            {slice && <span style={{ position: 'absolute', top: 4, left: 4, padding: '1px 6px', background: '#3b82f6', color: '#fff', fontSize: '0.6rem', fontWeight: 700, borderRadius: 5 }}>#{slice._idx}</span>}
                            {slice && (
                              <button onClick={() => handleDelete(slice.id, slice.filename)} title="삭제"
                                style={{ position: 'absolute', top: 3, right: 3, width: 20, height: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.55)', border: 'none', borderRadius: 4, color: '#f87171', cursor: 'pointer' }}><Trash2 size={12} /></button>
                            )}
                            <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '2px 4px', background: 'rgba(0,0,0,0.55)', fontSize: '0.6rem', color: dev ? '#cbd5e1' : '#64748b', textAlign: 'center', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={dev ? (dev.name || dev.id) : '기기 없음'}>
                              {dev ? (dev.name || dev.id) : '— 기기 없음 —'}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  );
                })()}

                {/* 세트 타임라인 — 리더 칸 재생 위치. 클릭하면 그 지점으로 이동(전 칸 동시). */}
                {playInfo[g.base] && playInfo[g.base].dur > 0 && (
                  <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: '0.66rem', color: '#94a3b8', minWidth: 84, fontVariantNumeric: 'tabular-nums' }}>
                      {fmtTime(playInfo[g.base].cur)} / {fmtTime(playInfo[g.base].dur)}
                    </span>
                    <div onMouseDown={(e) => startScrub(g, e)}
                      title="클릭/드래그해 위치 이동 (전 칸 동시 스크럽)"
                      style={{ flex: 1, height: 10, background: '#1e293b', borderRadius: 5, cursor: 'pointer', position: 'relative' }}>
                      <div style={{ height: '100%', width: `${Math.min(100, (playInfo[g.base].cur / playInfo[g.base].dur) * 100)}%`, background: '#8b5cf6', borderRadius: 5, pointerEvents: 'none' }} />
                      <div style={{ position: 'absolute', top: '50%', left: `${Math.min(100, (playInfo[g.base].cur / playInfo[g.base].dur) * 100)}%`, transform: 'translate(-50%, -50%)', width: 14, height: 14, borderRadius: '50%', background: '#c4b5fd', border: '2px solid #8b5cf6', pointerEvents: 'none' }} />
                    </div>
                    {playInfo[g.base].paused && <span style={{ fontSize: '0.62rem', color: '#64748b' }}>정지</span>}
                  </div>
                )}

                {deployMsg[g.base] && (
                  <div style={{ marginTop: 8, fontSize: '0.72rem', color: deployMsg[g.base].type === 'error' ? '#fca5a5' : '#86efac' }}>
                    {deployMsg[g.base].text}
                  </div>
                )}
              </div>
            ))}
            {others.length > 0 && (
              <div>
                {sets.length > 0 && <div style={{ fontSize: '0.8rem', color: '#94a3b8', fontWeight: 600, marginBottom: 8 }}>일반 동영상</div>}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 14 }}>
                  {others.map((v) => renderCard(v))}
                </div>
              </div>
            )}
          </div>
        );
      })()}

      {/* 비디오월 가공 모달 */}
      {showProc && (
        <div
          onMouseDown={(e) => { overlayDown.current = e.target === e.currentTarget; }}
          onClick={(e) => { if (overlayDown.current && e.target === e.currentTarget && !procBusy) setShowProc(false); }}
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{ width: 560, maxWidth: '92vw', maxHeight: '90vh', overflowY: 'auto', background: '#0f172a', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 14, padding: 22 }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
              <Scissors size={18} color="#8b5cf6" />
              <span style={{ fontSize: '1.05rem', fontWeight: 700, color: '#f1f5f9' }}>비디오월 가공</span>
              <div style={{ flex: 1 }} />
              <button onClick={() => { if (!procBusy) setShowProc(false); }} disabled={procBusy}
                style={{ background: 'none', border: 'none', color: procBusy ? '#475569' : '#94a3b8', cursor: procBusy ? 'not-allowed' : 'pointer' }}>
                <X size={18} />
              </button>
            </div>
            <div style={{ fontSize: '0.7rem', color: '#64748b', marginBottom: 16 }}>
              원본 동영상 1개를 <b>가로 대수만큼</b> 업스케일·크롭 후 슬라이스로 분할해
              <code style={{ color: '#8b5cf6' }}> wallsync </code>파일명으로 자동 등록합니다.
            </div>

            {/* 원본 파일 업로드 또는 라이브러리에서 선택 */}
            <div style={{ marginBottom: 14 }}>
              <label style={lbl}>원본 동영상</label>
              <input ref={procFileRef} type="file" accept="video/*" style={{ display: 'none' }}
                onChange={(e) => pickFile(e.target.files[0] || null)} />
              <button onClick={() => procFileRef.current?.click()} disabled={procBusy}
                style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '9px 14px', background: 'rgba(255,255,255,0.06)', color: procFile ? '#e2e8f0' : '#94a3b8', border: '1px dashed rgba(255,255,255,0.2)', borderRadius: 8, cursor: procBusy ? 'not-allowed' : 'pointer', fontSize: '0.8rem', width: '100%', textAlign: 'left', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                <Upload size={15} /> {procFile ? `${procFile.name} (${fmtSize(procFile.size)})` : '새 파일 업로드…'}
              </button>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, margin: '8px 0 6px', fontSize: '0.64rem', color: '#475569' }}>
                <div style={{ flex: 1, height: 1, background: 'rgba(255,255,255,0.08)' }} />또는<div style={{ flex: 1, height: 1, background: 'rgba(255,255,255,0.08)' }} />
              </div>
              <select value={procMediaId || ''} disabled={procBusy}
                onChange={(e) => pickLibrary(videos.find((v) => v.id === e.target.value) || null)}
                style={{ ...inp, color: procMediaId ? '#e2e8f0' : '#94a3b8', cursor: procBusy ? 'not-allowed' : 'pointer' }}>
                <option value="">라이브러리에서 선택… (재업로드 없이)</option>
                {videos.map((v) => <option key={v.id} value={v.id}>{v.filename}</option>)}
              </select>
            </div>

            {/* 파라미터 */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 14 }}>
              <div>
                <label style={lbl}>가로 대수</label>
                <input type="number" min={1} max={12} value={proc.deviceCount} disabled={procBusy}
                  onChange={(e) => setProc((p) => ({ ...p, deviceCount: e.target.value }))} style={inp} />
              </div>
              <div>
                <label style={lbl}>세로 위치 (%)</label>
                <input type="number" min={0} max={100} value={proc.yOffsetPct} disabled={procBusy}
                  onChange={(e) => setProc((p) => ({ ...p, yOffsetPct: e.target.value }))} style={inp} />
              </div>
              <div>
                <label style={lbl}>슬라이스 폭 (px)</label>
                <input type="number" min={16} max={7680} value={proc.sliceWidth} disabled={procBusy}
                  onChange={(e) => { const v = e.target.value; setProc((p) => ({ ...p, sliceWidth: v })); setSlcManual(true); }} style={inp} />
              </div>
              <div>
                <label style={lbl}>슬라이스 높이 (px)</label>
                <input type="number" min={16} max={4320} value={proc.sliceHeight} disabled={procBusy}
                  onChange={(e) => { const v = e.target.value; setProc((p) => ({ ...p, sliceHeight: v })); setSlcManual(true); }} style={inp} />
              </div>
              {/* 해상도 프리셋 — 원클릭 입력. 감지값은 기기 EDID/스펙 기준. */}
              <div style={{ gridColumn: '1 / 3', display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap', marginTop: -4 }}>
                <span style={{ fontSize: '0.62rem', color: '#64748b' }}>프리셋:</span>
                <button type="button" disabled={procBusy} onClick={() => { setProc((p) => ({ ...p, sliceWidth: 3840, sliceHeight: 2160 })); setSlcManual(true); }} style={presetBtn(proc.sliceWidth == 3840 && proc.sliceHeight == 2160)}>4K · 3840×2160</button>
                <button type="button" disabled={procBusy} onClick={() => { setProc((p) => ({ ...p, sliceWidth: 1920, sliceHeight: 1080 })); setSlcManual(true); }} style={presetBtn(proc.sliceWidth == 1920 && proc.sliceHeight == 1080)}>FHD · 1920×1080</button>
                {(() => { const r = detectRes(); return (
                  <button type="button" disabled={procBusy || !r} onClick={() => { setSlcManual(false); const res = detectRes(); setProc((p) => ({ ...p, sliceWidth: res?.w || 3840, sliceHeight: res?.h || 2160 })); }} style={presetBtn(false)}>감지값{r ? ` · ${r.w}×${r.h}` : ' (없음)'}</button>
                ); })()}
              </div>
              <div style={{ gridColumn: '1 / 3' }}>
                <label style={lbl}>파일명 베이스 (선택)</label>
                <input type="text" placeholder="비우면 원본 파일명 사용" value={proc.baseName} disabled={procBusy}
                  onChange={(e) => setProc((p) => ({ ...p, baseName: e.target.value }))} style={inp} />
              </div>
            </div>

            {/* 원본 크롭 미리보기 — 주황 사각형 안쪽이 벽에 표시됨. 세로 드래그로 위치 조절. */}
            {procSrcUrl && (
              <div style={{ marginBottom: 16 }}>
                <div style={{ fontSize: '0.68rem', color: '#94a3b8', marginBottom: 6 }}>
                  원본 크롭 미리보기 — <b style={{ color: '#fb923c' }}>주황 영역</b>이 벽에 표시됩니다.
                  {cropRect && cropRect.hPct < 99.5 ? ' 위아래로 드래그해 세로 위치 조절.' : ''}
                </div>
                <div ref={cropBoxRef} onMouseDown={startCropDrag}
                  style={{ position: 'relative', width: '100%', background: '#000', borderRadius: 6, overflow: 'hidden', userSelect: 'none', cursor: procBusy || !cropRect || cropRect.hPct >= 99.5 ? 'default' : 'ns-resize' }}>
                  <video ref={cropVidRef} src={procSrcUrl} muted loop autoPlay playsInline preload="auto"
                    onLoadedMetadata={(e) => { setProcSrcDim({ w: e.currentTarget.videoWidth, h: e.currentTarget.videoHeight }); setCropProg({ cur: 0, dur: e.currentTarget.duration || 0 }); }}
                    onTimeUpdate={(e) => setCropProg({ cur: e.currentTarget.currentTime, dur: e.currentTarget.duration || 0 })}
                    onSeeked={() => { cropSeeking.current = false; flushCropSeek(); }}
                    style={{ width: '100%', display: 'block', pointerEvents: 'none' }} />
                  {/* 재생/정지 토글 — 특정 프레임에서 크롭 확인하고 싶을 때 */}
                  <button onMouseDown={(e) => e.stopPropagation()}
                    onClick={(e) => { e.stopPropagation(); const v = cropVidRef.current; if (!v) return; if (v.paused) { v.play().catch(() => {}); setCropPaused(false); } else { v.pause(); setCropPaused(true); } }}
                    title={cropPaused ? '재생' : '정지'}
                    style={{ position: 'absolute', bottom: 8, left: 8, width: 30, height: 30, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.6)', border: '1px solid rgba(255,255,255,0.2)', borderRadius: 6, color: '#fff', cursor: 'pointer', zIndex: 2 }}>
                    {cropPaused ? <Play size={14} /> : <Square size={12} />}
                  </button>
                  {cropRect && (
                    <>
                      {/* 크롭 사각형 + 바깥 어둡게(역마스크) */}
                      <div style={{ position: 'absolute', left: `${cropRect.leftPct}%`, top: `${cropRect.topPct}%`, width: `${cropRect.wPct}%`, height: `${cropRect.hPct}%`, border: '2px solid #fb923c', boxShadow: '0 0 0 9999px rgba(0,0,0,0.5)', boxSizing: 'border-box', pointerEvents: 'none' }} />
                      {/* 기기 경계 분할선 */}
                      {Array.from({ length: Math.max(1, Math.min(12, parseInt(proc.deviceCount) || 1)) - 1 }, (_, i) => {
                        const n = parseInt(proc.deviceCount) || 1;
                        return <div key={i} style={{ position: 'absolute', top: `${cropRect.topPct}%`, height: `${cropRect.hPct}%`, left: `${cropRect.leftPct + cropRect.wPct * (i + 1) / n}%`, borderLeft: '1px dashed rgba(251,146,60,0.8)', pointerEvents: 'none' }} />;
                      })}
                    </>
                  )}
                </div>
                {/* 재생 위치 스크럽 바 — 드래그/클릭으로 원본 재생 위치 이동(크롭 확인용) */}
                <div onMouseDown={startCropSeek}
                  style={{ position: 'relative', height: 10, background: '#1e293b', borderRadius: 5, marginTop: 8, cursor: 'pointer', userSelect: 'none' }}>
                  <div style={{ position: 'absolute', left: 0, top: 0, height: '100%', width: `${cropProg.dur ? Math.min(100, (cropProg.cur / cropProg.dur) * 100) : 0}%`, background: '#fb923c', borderRadius: 5, pointerEvents: 'none' }} />
                  <div style={{ position: 'absolute', top: '50%', left: `${cropProg.dur ? Math.min(100, (cropProg.cur / cropProg.dur) * 100) : 0}%`, transform: 'translate(-50%, -50%)', width: 14, height: 14, borderRadius: '50%', background: '#fed7aa', border: '2px solid #fb923c', pointerEvents: 'none' }} />
                </div>
                <div style={{ fontSize: '0.6rem', color: '#64748b', marginTop: 4, textAlign: 'right' }}>{fmtTime(cropProg.cur)} / {fmtTime(cropProg.dur)}</div>
              </div>
            )}

            {/* 벽 미리보기 */}
            <div style={{ marginBottom: 16 }}>
              <div style={{ fontSize: '0.68rem', color: '#94a3b8', marginBottom: 6 }}>
                벽 전체 <b style={{ color: '#cbd5e1' }}>{wallW || 0}×{proc.sliceHeight || 0}</b> · 슬라이스 {proc.deviceCount || 0}개
              </div>
              <div style={{ display: 'flex', gap: 3, aspectRatio: wallW && proc.sliceHeight ? `${wallW} / ${proc.sliceHeight}` : '16 / 9', width: '100%', background: '#000', borderRadius: 6, overflow: 'hidden' }}>
                {Array.from({ length: Math.max(1, Math.min(12, parseInt(proc.deviceCount) || 1)) }, (_, i) => (
                  <div key={i} style={{ flex: 1, background: 'rgba(139,92,246,0.18)', border: '1px solid rgba(139,92,246,0.45)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#a78bfa', fontSize: '0.72rem', fontWeight: 700 }}>{i}</div>
                ))}
              </div>
            </div>

            {/* 진행 상태 */}
            {job && (
              <div style={{ marginBottom: 16, padding: '10px 12px', background: job.status === 'error' ? 'rgba(248,113,113,0.1)' : 'rgba(139,92,246,0.1)', border: `1px solid ${job.status === 'error' ? 'rgba(248,113,113,0.3)' : 'rgba(139,92,246,0.3)'}`, borderRadius: 8 }}>
                <div style={{ fontSize: '0.76rem', color: job.status === 'error' ? '#fca5a5' : '#cbd5e1', marginBottom: job.status === 'error' ? 0 : 6 }}>
                  {STATUS_LABEL[job.status] || job.status}{job.status !== 'error' && job.pct != null ? ` — ${job.pct}%` : ''}{job.error ? `: ${job.error}` : ''}
                </div>
                {job.status !== 'error' && (
                  <div style={{ height: 6, background: '#1e293b', borderRadius: 3, overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${job.pct || 0}%`, background: job.status === 'done' ? '#22c55e' : '#8b5cf6', transition: 'width 0.3s' }} />
                  </div>
                )}
              </div>
            )}

            {/* 결과 미리보기 — 슬라이스를 좌→우로 붙여 벽 재구성 확인 */}
            {job?.status === 'done' && Array.isArray(job.slices) && job.slices.length > 0 && (
              <div style={{ marginBottom: 16 }}>
                <div style={{ fontSize: '0.68rem', color: '#86efac', marginBottom: 6 }}>
                  ✓ 결과 미리보기 — 슬라이스 {job.slices.length}개가 좌→우로 이어집니다 (벽 재구성)
                </div>
                <div style={{ display: 'flex', gap: 0, width: '100%', aspectRatio: wallW && proc.sliceHeight ? `${wallW} / ${proc.sliceHeight}` : '32 / 9', background: '#000', border: '1px solid rgba(255,255,255,0.1)' }}>
                  {job.slices.map((s, i) => {
                    const url = (s.path || '').startsWith('http') ? s.path : `${API}${s.path}`;
                    return (
                      <video key={s.id || i} src={url} muted loop autoPlay playsInline preload="metadata"
                        title={s.filename}
                        style={{ flex: 1, minWidth: 0, height: '100%', objectFit: 'cover', background: '#000', borderRight: i < job.slices.length - 1 ? '1px solid rgba(255,255,255,0.15)' : 'none' }} />
                    );
                  })}
                </div>
                <div style={{ fontSize: '0.62rem', color: '#64748b', marginTop: 5 }}>
                  각 칸이 한 화면(기기)입니다. 경계에서 영상이 자연스럽게 이어지면 정상.
                </div>
              </div>
            )}

            {/* 액션 */}
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              {procBusy ? (
                <button onClick={cancelProc} style={{ padding: '8px 18px', background: '#7f1d1d', color: '#fecaca', border: 'none', borderRadius: 8, cursor: 'pointer', fontSize: '0.85rem', fontWeight: 600 }}>취소</button>
              ) : (
                <>
                  <button onClick={() => setShowProc(false)} style={{ padding: '8px 18px', background: 'rgba(255,255,255,0.06)', color: '#cbd5e1', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 8, cursor: 'pointer', fontSize: '0.85rem' }}>닫기</button>
                  <button onClick={startProc} disabled={!procFile && !procMediaId} title="서버(NAS)에서 ffmpeg로 가공 — NAS가 약하면 느림" style={{ padding: '8px 18px', background: (procFile || procMediaId) ? 'rgba(139,92,246,0.18)' : '#334155', color: (procFile || procMediaId) ? '#c4b5fd' : '#fff', border: '1px solid rgba(139,92,246,0.4)', borderRadius: 8, cursor: (procFile || procMediaId) ? 'pointer' : 'not-allowed', fontSize: '0.85rem', fontWeight: 600 }}>
                    {job?.status === 'error' ? '서버 재시도' : '서버 가공'}
                  </button>
                  <button onClick={startProcLocal} disabled={!procFile && !procMediaId} title="브라우저(이 PC의 GPU)에서 직접 인코딩 — NAS 부담 없이 빠름. Chrome/Edge 권장." style={{ padding: '8px 18px', background: (procFile || procMediaId) ? '#10b981' : '#334155', color: '#fff', border: 'none', borderRadius: 8, cursor: (procFile || procMediaId) ? 'pointer' : 'not-allowed', fontSize: '0.85rem', fontWeight: 700 }}>
                    {job?.status === 'done' ? '다시 가공' : '⚡ PC에서 가공'}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 동기 미세조정 (위상 오프셋) 모달 */}
      {showOffset && (
        <div
          onMouseDown={(e) => { overlayDown.current = e.target === e.currentTarget; }}
          onClick={(e) => { if (overlayDown.current && e.target === e.currentTarget) setShowOffset(false); }}
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}
        >
          <div onClick={(e) => e.stopPropagation()} style={{ width: 620, maxWidth: '94vw', maxHeight: '90vh', overflowY: 'auto', background: '#0f172a', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 14, padding: 22 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
              <SlidersHorizontal size={18} color="#3b82f6" />
              <span style={{ fontSize: '1.05rem', fontWeight: 700, color: '#f1f5f9' }}>기기별 동기 미세조정 (위상 오프셋)</span>
              <div style={{ flex: 1 }} />
              <button onClick={autoAlign} style={{ padding: '5px 14px', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: '0.76rem', fontWeight: 700 }}>자동 정렬</button>
              <button onClick={() => setShowOffset(false)} style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', marginLeft: 4 }}><X size={18} /></button>
            </div>
            <div style={{ fontSize: '0.68rem', color: '#64748b', marginBottom: 14 }}>
              <b>자동 정렬</b>로 서버 측정 스큐 기준 시작점을 잡은 뒤, 화면을 보며 <b>늦은 화면</b>을 −, <b>빠른 화면</b>을 + 로 미세조정합니다.
              즉시 적용·기기에 저장됩니다. (<code style={{ color: '#94a3b8' }}>skew</code>=서버가 잰 시계차, 음수 클수록 뒤처짐)
            </div>
            {devices.length === 0 ? (
              <div style={{ color: '#64748b', fontSize: '0.8rem', padding: '12px 0' }}>온라인 기기가 없습니다.</div>
            ) : (
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
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default VideoWallManager;
