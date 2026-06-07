import { useState, useEffect, useCallback, useRef } from 'react';
import { DndContext, closestCenter, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import { SortableContext, horizontalListSortingStrategy, arrayMove, useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

const API = '';

const FONTS = [
  { value: 'NotoSansKR-Regular',  label: 'Noto Sans KR (기본)' },
  { value: 'NotoSansKR-Bold',     label: 'Noto Sans KR Bold' },
  { value: 'NotoSerifKR-Regular', label: 'Noto Serif KR' },
  { value: 'NanumGothic',         label: '나눔고딕' },
  { value: 'NanumGothicBold',     label: '나눔고딕 Bold' },
];

const DEFAULT_TICKER = {
  enabled: false,
  mode: 'individual',
  text: '',
  speed: 50,
  direction: 'rtl',
  position: 'bottom',
  fontFamily: 'NotoSansKR-Regular',
  fontSize: 48,
  fontBold: false,
  fontItalic: false,
  textColor: '#FFFFFF',
  bgColor: '#000000',
  bgOpacity: 65,
  repeatCount: 0,
  deviceOrder: '[]',
};

const PRESET_KEY = 'ticker_presets';
function loadPresets() {
  try { return JSON.parse(localStorage.getItem(PRESET_KEY) || '[]'); } catch { return []; }
}
function savePresets(list) {
  localStorage.setItem(PRESET_KEY, JSON.stringify(list));
}

function SortableDeviceChip({ id, name }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id });
  return (
    <div
      ref={setNodeRef}
      {...attributes}
      {...listeners}
      style={{
        transform: CSS.Transform.toString(transform),
        transition,
        opacity: isDragging ? 0.5 : 1,
        background: isDragging ? 'rgba(59,130,246,0.3)' : 'rgba(255,255,255,0.07)',
        border: '1px solid rgba(255,255,255,0.15)',
        borderRadius: '10px',
        padding: '8px 14px',
        cursor: 'grab',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        fontSize: '0.82rem',
        color: '#e2e8f0',
        userSelect: 'none',
        whiteSpace: 'nowrap',
      }}
    >
      <span style={{ color: '#64748b', fontSize: '1rem' }}>⠿</span>
      {name}
    </div>
  );
}

const btnStyle = { background: 'none', border: 'none', cursor: 'pointer', padding: 0 };
const sideLabel = { fontSize: '0.72rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '4px' };

export default function TickerManager({ token, selectedStoreId: propStoreId, stores: propStores }) {
  const [stores, setStores]               = useState(propStores || []);
  const [selectedStore, setSelectedStore] = useState(null);
  const [groups, setGroups]               = useState([]);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [ticker, setTicker]               = useState(DEFAULT_TICKER);
  const [devices, setDevices]             = useState([]);
  const [orderedIds, setOrderedIds]       = useState([]);
  const [saving, setSaving]               = useState(false);
  const [saved, setSaved]                 = useState(false);
  const [presets, setPresets]             = useState(loadPresets);
  const [presetName, setPresetName]       = useState('');

  const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  // 사업장 목록 로드 (prop으로 안 들어오면 직접 fetch)
  useEffect(() => {
    if (propStores && propStores.length > 0) { setStores(propStores); return; }
    fetch(`${API}/api/stores`, { headers }).then(r => r.json()).then(setStores).catch(() => {});
  }, [propStores]);

  // 외부 selectedStoreId 변경 시 자동 선택
  useEffect(() => {
    if (!propStoreId || stores.length === 0) return;
    const match = stores.find(s => s.id === propStoreId);
    if (match && match.id !== selectedStore?.id) loadStore(match);
  }, [propStoreId, stores]);

  const loadStore = useCallback(async (store) => {
    setSelectedStore(store);
    setSelectedGroup(null);
    setTicker(DEFAULT_TICKER);
    setDevices([]);
    setOrderedIds([]);
    const all = await fetch(`${API}/api/groups`, { headers }).then(r => r.json()).catch(() => []);
    setGroups(Array.isArray(all) ? all.filter(g => g.storeId === store.id) : []);
  }, []);

  const loadGroup = useCallback(async (group) => {
    setSelectedGroup(group);
    const [tickerRes, devicesRes] = await Promise.all([
      fetch(`${API}/api/groups/${group.id}/ticker`, { headers }).then(r => r.json()),
      fetch(`${API}/api/devices?groupId=${group.id}`, { headers }).then(r => r.json()),
    ]);
    setTicker({ ...DEFAULT_TICKER, ...tickerRes });
    setDevices(Array.isArray(devicesRes) ? devicesRes : []);
    const savedOrder = JSON.parse(tickerRes.deviceOrder || '[]');
    const allIds = (Array.isArray(devicesRes) ? devicesRes : []).map(d => d.id);
    setOrderedIds([
      ...savedOrder.filter(id => allIds.includes(id)),
      ...allIds.filter(id => !savedOrder.includes(id)),
    ]);
  }, []);

  const handleDragEnd = ({ active, over }) => {
    if (over && active.id !== over.id)
      setOrderedIds(ids => arrayMove(ids, ids.indexOf(active.id), ids.indexOf(over.id)));
  };

  const save = async () => {
    if (!selectedGroup) return;
    setSaving(true);
    await fetch(`${API}/api/groups/${selectedGroup.id}/ticker`, {
      method: 'POST', headers,
      body: JSON.stringify({ ...ticker, deviceOrder: JSON.stringify(orderedIds) }),
    });
    setSaving(false);
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const update = (field, value) => setTicker(t => ({ ...t, [field]: value }));

  const previewBgRgba = (() => {
    const r = parseInt(ticker.bgColor.slice(1, 3), 16) || 0;
    const g = parseInt(ticker.bgColor.slice(3, 5), 16) || 0;
    const b = parseInt(ticker.bgColor.slice(5, 7), 16) || 0;
    return `rgba(${r},${g},${b},${ticker.bgOpacity / 100})`;
  })();

  // 미리보기 — 기기 실제 해상도(1920px) 기준으로 duration 계산
  // 실제 기기: duration = (1920 + actualTextWidth) / speed
  // 미리보기: 같은 duration, 컨테이너 비율로 시각 속도 스케일
  const DEVICE_SCREEN_W = 1920;
  const previewContainerRef = useRef(null);
  const previewTextRef = useRef(null);
  const hiddenTextRef = useRef(null); // 실제 폰트 크기로 숨겨진 요소 — 실제 텍스트 너비 측정용
  const [previewStartPx, setPreviewStartPx] = useState(800);
  const [previewEndPx, setPreviewEndPx] = useState(-300);
  const [previewDuration, setPreviewDuration] = useState(10);

  useEffect(() => {
    const container = previewContainerRef.current;
    const previewText = previewTextRef.current;
    const hiddenText = hiddenTextRef.current;
    if (!container || !previewText || !hiddenText) return;
    const containerW = container.offsetWidth;
    const previewTextW = previewText.offsetWidth;
    const actualTextW = hiddenText.offsetWidth; // 실제 기기 폰트 크기 기준
    // 실제 기기와 동일한 사이클 시간
    const actualDuration = Math.max(1, (DEVICE_SCREEN_W + actualTextW) / ticker.speed);
    setPreviewStartPx(containerW);
    setPreviewEndPx(-previewTextW);
    setPreviewDuration(actualDuration);
  }, [ticker.speed, ticker.text, ticker.fontSize, ticker.fontBold, ticker.fontItalic, ticker.fontFamily]);

  const orderedDevices = orderedIds.map(id => devices.find(d => d.id === id)).filter(Boolean);

  const selBtn = (active) => ({
    background: active ? 'rgba(59,130,246,0.2)' : 'transparent',
    border: active ? '1px solid rgba(59,130,246,0.5)' : '1px solid transparent',
    borderRadius: '8px', padding: '7px 10px', color: '#e2e8f0',
    textAlign: 'left', cursor: 'pointer', fontSize: '0.83rem', width: '100%',
  });

  return (
    <div style={{ display: 'flex', gap: '0', height: '100%', overflow: 'hidden' }}>

      {/* ── 왼쪽 사이드바 ── */}
      <div className="glass-card" style={{ width: '230px', flexShrink: 0, padding: '16px', display: 'flex', flexDirection: 'column', gap: '14px', overflowY: 'auto', borderRadius: 0, borderRight: '1px solid rgba(255,255,255,0.06)' }}>

        {/* 사업장 */}
        <div>
          <div style={sideLabel}>사업장</div>
          {stores.length === 0 && <div style={{ color: '#475569', fontSize: '0.8rem' }}>사업장 없음</div>}
          {stores.map(s => (
            <button key={s.id} onClick={() => loadStore(s)} style={{ ...selBtn(selectedStore?.id === s.id), background: selectedStore?.id === s.id ? 'rgba(99,102,241,0.2)' : 'transparent', border: selectedStore?.id === s.id ? '1px solid rgba(99,102,241,0.5)' : '1px solid transparent', marginBottom: '3px' }}>{s.name}</button>
          ))}
        </div>

        {/* 그룹 */}
        {selectedStore && (
          <div>
            <div style={sideLabel}>그룹</div>
            {groups.length === 0 && <div style={{ color: '#475569', fontSize: '0.8rem' }}>그룹 없음</div>}
            {groups.map(g => (
              <button key={g.id} onClick={() => loadGroup(g)} style={{ ...selBtn(selectedGroup?.id === g.id), marginBottom: '3px' }}>{g.name}</button>
            ))}
          </div>
        )}

        {/* 구분선 */}
        <div style={{ borderTop: '1px solid rgba(255,255,255,0.07)', paddingTop: '12px' }}>
          <div style={sideLabel}>자막 저장목록</div>

          {/* 저장 입력 */}
          <div style={{ display: 'flex', gap: '6px', marginBottom: '8px' }}>
            <input
              value={presetName}
              onChange={e => setPresetName(e.target.value)}
              placeholder="이름 입력"
              style={{ flex: 1, minWidth: 0, background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '6px', padding: '5px 8px', color: '#f1f5f9', fontSize: '0.78rem' }}
            />
            <button
              onClick={() => {
                if (!presetName.trim() || !ticker.text.trim()) return;
                const next = [{ name: presetName.trim(), text: ticker.text, savedAt: Date.now() }, ...presets];
                setPresets(next); savePresets(next); setPresetName('');
              }}
              style={{ padding: '5px 10px', borderRadius: '6px', border: '1px solid rgba(59,130,246,0.4)', background: 'rgba(59,130,246,0.15)', color: '#60a5fa', fontSize: '0.75rem', cursor: 'pointer', whiteSpace: 'nowrap', flexShrink: 0 }}>
              저장
            </button>
          </div>

          {/* 프리셋 목록 */}
          {presets.length === 0 && <div style={{ color: '#475569', fontSize: '0.78rem' }}>저장된 자막 없음</div>}
          {presets.map((p, i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: '6px', background: 'rgba(255,255,255,0.04)', borderRadius: '7px', padding: '7px 8px', marginBottom: '4px' }}>
              <button onClick={() => update('text', p.text)} style={{ ...btnStyle, flex: 1, textAlign: 'left' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#94a3b8' }}>{p.name}</div>
                <div style={{ fontSize: '0.7rem', color: '#475569', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '140px' }}>{p.text}</div>
              </button>
              <button onClick={() => { const next = presets.filter((_, j) => j !== i); setPresets(next); savePresets(next); }}
                style={{ ...btnStyle, color: '#475569', fontSize: '0.8rem', flexShrink: 0, marginTop: '1px' }}>✕</button>
            </div>
          ))}
        </div>
      </div>

      {/* ── 오른쪽 설정 패널 ── */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px', minWidth: 0 }}>
        {!selectedGroup ? (
          <div className="glass-card" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#475569', fontSize: '0.9rem' }}>
            왼쪽에서 사업장 → 그룹을 선택하세요
          </div>
        ) : (<>

          {/* 활성화 토글 */}
          <div className="glass-card" style={{ padding: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ fontWeight: 600, fontSize: '1rem', color: '#f1f5f9' }}>자막 활성화</div>
              <div style={{ fontSize: '0.78rem', color: '#64748b', marginTop: '2px' }}>{selectedGroup.name} 그룹</div>
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
              <div onClick={() => update('enabled', !ticker.enabled)} style={{
                width: '48px', height: '26px', borderRadius: '13px', position: 'relative',
                background: ticker.enabled ? '#3b82f6' : '#1e293b',
                border: '1px solid rgba(255,255,255,0.1)', transition: 'background 0.2s',
              }}>
                <div style={{ position: 'absolute', top: '3px', left: ticker.enabled ? '24px' : '3px', width: '18px', height: '18px', borderRadius: '50%', background: '#fff', transition: 'left 0.2s' }} />
              </div>
              <span style={{ color: ticker.enabled ? '#3b82f6' : '#475569', fontSize: '0.85rem', fontWeight: 600 }}>
                {ticker.enabled ? 'ON' : 'OFF'}
              </span>
            </label>
          </div>

          {ticker.enabled && (<>

            {/* 자막 내용 */}
            <div className="glass-card" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#38bdf8', textTransform: 'uppercase', letterSpacing: '0.06em' }}>자막 내용</div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>텍스트</label>
                <textarea value={ticker.text} onChange={e => update('text', e.target.value)}
                  rows={2} style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', padding: '10px 12px', color: '#f1f5f9', fontSize: '0.9rem', resize: 'vertical' }} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>모드</label>
                  <select value={ticker.mode} onChange={e => update('mode', e.target.value)}
                    style={{ background: '#1e293b', border: '1px solid rgba(255,255,255,0.15)', borderRadius: '8px', padding: '8px 10px', color: '#f1f5f9', fontSize: '0.85rem', colorScheme: 'dark' }}>
                    <option value="individual">개별 (단일 화면)</option>
                    <option value="group">그룹 (연속 흐름)</option>
                  </select>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>방향</label>
                  <select value={ticker.direction} onChange={e => update('direction', e.target.value)}
                    style={{ background: '#1e293b', border: '1px solid rgba(255,255,255,0.15)', borderRadius: '8px', padding: '8px 10px', color: '#f1f5f9', fontSize: '0.85rem', colorScheme: 'dark' }}>
                    <option value="rtl">→← 오른쪽에서 왼쪽</option>
                    <option value="ltr">←→ 왼쪽에서 오른쪽</option>
                  </select>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>위치</label>
                  <select value={ticker.position} onChange={e => update('position', e.target.value)}
                    style={{ background: '#1e293b', border: '1px solid rgba(255,255,255,0.15)', borderRadius: '8px', padding: '8px 10px', color: '#f1f5f9', fontSize: '0.85rem', colorScheme: 'dark' }}>
                    <option value="top">상단</option>
                    <option value="center">중앙</option>
                    <option value="bottom">하단</option>
                  </select>
                </div>
              </div>

              {/* 속도 + 반복 횟수 */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>속도: {ticker.speed} px/s</label>
                  <input type="range" min={1} max={100} step={1} value={ticker.speed} onChange={e => update('speed', Number(e.target.value))}
                    style={{ accentColor: '#3b82f6' }} />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                    반복: {ticker.repeatCount === 0 ? '무한 반복' : `${ticker.repeatCount}회`}
                  </label>
                  <input type="range" min={0} max={20} step={1} value={ticker.repeatCount} onChange={e => update('repeatCount', Number(e.target.value))}
                    style={{ accentColor: '#3b82f6' }} />
                  <div style={{ fontSize: '0.72rem', color: '#475569' }}>0 = 무한, 1~20 = 지정 횟수 후 종료</div>
                </div>
              </div>
            </div>

            {/* 폰트 설정 */}
            <div className="glass-card" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#38bdf8', textTransform: 'uppercase', letterSpacing: '0.06em' }}>폰트 설정</div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr auto auto', gap: '12px', alignItems: 'end' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>폰트</label>
                  <select value={ticker.fontFamily} onChange={e => update('fontFamily', e.target.value)}
                    style={{ background: '#1e293b', border: '1px solid rgba(255,255,255,0.15)', borderRadius: '8px', padding: '8px 10px', color: '#f1f5f9', fontSize: '0.85rem', colorScheme: 'dark' }}>
                    {FONTS.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}
                  </select>
                </div>
                <button onClick={() => update('fontBold', !ticker.fontBold)}
                  style={{ padding: '8px 16px', borderRadius: '8px', border: ticker.fontBold ? '1px solid rgba(59,130,246,0.8)' : '1px solid rgba(255,255,255,0.25)', background: ticker.fontBold ? 'rgba(59,130,246,0.4)' : 'rgba(255,255,255,0.12)', color: ticker.fontBold ? '#93c5fd' : '#e2e8f0', fontWeight: 700, cursor: 'pointer', fontSize: '0.85rem' }}>
                  Bold
                </button>
                <button onClick={() => update('fontItalic', !ticker.fontItalic)}
                  style={{ padding: '8px 16px', borderRadius: '8px', border: ticker.fontItalic ? '1px solid rgba(59,130,246,0.8)' : '1px solid rgba(255,255,255,0.25)', background: ticker.fontItalic ? 'rgba(59,130,246,0.4)' : 'rgba(255,255,255,0.12)', color: ticker.fontItalic ? '#93c5fd' : '#e2e8f0', fontStyle: 'italic', cursor: 'pointer', fontSize: '0.85rem' }}>
                  Italic
                </button>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>크기: {ticker.fontSize}px</label>
                <input type="range" min={16} max={120} step={2} value={ticker.fontSize} onChange={e => update('fontSize', Number(e.target.value))}
                  style={{ accentColor: '#3b82f6' }} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>글자 색</label>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <input type="color" value={ticker.textColor} onChange={e => update('textColor', e.target.value)}
                      style={{ width: '36px', height: '36px', borderRadius: '6px', border: 'none', cursor: 'pointer' }} />
                    <span style={{ fontSize: '0.82rem', color: '#94a3b8' }}>{ticker.textColor}</span>
                  </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>배경 색</label>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <input type="color" value={ticker.bgColor} onChange={e => update('bgColor', e.target.value)}
                      style={{ width: '36px', height: '36px', borderRadius: '6px', border: 'none', cursor: 'pointer' }} />
                    <span style={{ fontSize: '0.82rem', color: '#94a3b8' }}>{ticker.bgColor}</span>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>배경 투명도: {ticker.bgOpacity}%</label>
                <input type="range" min={0} max={100} step={5} value={ticker.bgOpacity} onChange={e => update('bgOpacity', Number(e.target.value))}
                  style={{ accentColor: '#3b82f6' }} />
              </div>
            </div>

            {/* 기기 순서 (그룹 모드) */}
            {ticker.mode === 'group' && (
              <div className="glass-card" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#38bdf8', textTransform: 'uppercase', letterSpacing: '0.06em' }}>기기 물리 순서 (드래그로 조정)</div>
                <div style={{ fontSize: '0.78rem', color: '#64748b' }}>왼쪽이 첫 번째 화면입니다.</div>
                {orderedDevices.length === 0
                  ? <div style={{ color: '#475569', fontSize: '0.82rem' }}>이 그룹에 배정된 기기가 없습니다.</div>
                  : (
                    <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                      <SortableContext items={orderedIds} strategy={horizontalListSortingStrategy}>
                        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                          {orderedDevices.map((d, i) => (
                            <div key={d.id} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
                              <div style={{ fontSize: '0.7rem', color: '#3b82f6', fontWeight: 700 }}>{i + 1}번</div>
                              <SortableDeviceChip id={d.id} name={d.name} />
                            </div>
                          ))}
                        </div>
                      </SortableContext>
                    </DndContext>
                  )
                }
              </div>
            )}

            {/* 미리보기 */}
            <div className="glass-card" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#38bdf8', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                미리보기 <span style={{ fontWeight: 400, color: '#475569', textTransform: 'none', letterSpacing: 0 }}>({ticker.speed}px/s 기준)</span>
              </div>
              {/* 실제 폰트 크기로 텍스트 너비 측정 (화면 밖에 숨김) */}
              <span ref={hiddenTextRef} style={{ position: 'fixed', top: '-9999px', left: '-9999px', whiteSpace: 'nowrap', fontSize: `${ticker.fontSize}px`, fontWeight: ticker.fontBold ? 700 : 400, fontStyle: ticker.fontItalic ? 'italic' : 'normal', visibility: 'hidden', pointerEvents: 'none' }}>
                {ticker.text || '자막 텍스트를 입력하세요'}
              </span>

              <div ref={previewContainerRef} style={{ position: 'relative', width: '100%', height: '80px', background: '#111', borderRadius: '8px', overflow: 'hidden' }}>
                <div style={{
                  position: 'absolute', left: 0, right: 0,
                  top: ticker.position === 'top' ? 0 : ticker.position === 'center' ? '50%' : 'auto',
                  bottom: ticker.position === 'bottom' ? 0 : 'auto',
                  transform: ticker.position === 'center' ? 'translateY(-50%)' : 'none',
                  height: `${Math.min(ticker.fontSize * 1.5, 60)}px`,
                  background: previewBgRgba,
                  overflow: 'hidden',
                  display: 'flex',
                  alignItems: 'center',
                  position: 'relative',
                }}>
                  <span
                    ref={previewTextRef}
                    style={{
                      display: 'inline-block',
                      whiteSpace: 'nowrap',
                      color: ticker.textColor,
                      fontSize: `${Math.min(ticker.fontSize * 0.4, 24)}px`,
                      fontWeight: ticker.fontBold ? 700 : 400,
                      fontStyle: ticker.fontItalic ? 'italic' : 'normal',
                      position: 'absolute',
                      animationName: 'ticker-preview',
                      animationDuration: `${previewDuration}s`,
                      animationTimingFunction: 'linear',
                      animationIterationCount: ticker.repeatCount === 0 ? 'infinite' : ticker.repeatCount,
                      animationFillMode: 'forwards',
                    }}
                  >
                    {ticker.text || '자막 텍스트를 입력하세요'}
                  </span>
                </div>
              </div>
              <style>{`
                @keyframes ticker-preview {
                  from { transform: translateX(${previewStartPx}px); }
                  to   { transform: translateX(${previewEndPx}px); }
                }
              `}</style>
            </div>

          </>)}

          {/* 저장 버튼 */}
          <button onClick={save} disabled={saving}
            style={{ padding: '12px 32px', borderRadius: '10px', border: 'none', background: saved ? '#10b981' : '#3b82f6', color: '#fff', fontWeight: 700, fontSize: '0.95rem', cursor: saving ? 'not-allowed' : 'pointer', opacity: saving ? 0.7 : 1, alignSelf: 'flex-start', transition: 'background 0.3s' }}>
            {saved ? '✓ 저장됨' : saving ? '저장 중…' : '저장'}
          </button>

        </>)}
      </div>
    </div>
  );
}
