import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Film, Image as ImageIcon, Upload, Play, Clock, Save, Trash2, GripVertical, Eye, X, ChevronLeft, ChevronRight, ChevronDown, ChevronUp, Maximize, Minimize } from 'lucide-react';

const SOCKET_URL = 'http://localhost:3000';

export default function MediaManager({ stores, groups, devices, fetchDevices }) {
  const [selectedStoreId, setSelectedStoreId] = useState('');
  const [selectedGroupId, setSelectedGroupId] = useState('');
  const [previewData, setPreviewData] = useState(null); // { playlist, startIndex }
  const [libraryCollapsed, setLibraryCollapsed] = useState(false);
  const [collapsedDevices, setCollapsedDevices] = useState({}); // { deviceId: boolean }
  
  const [mediaList, setMediaList] = useState([]);
  const [playlist, setPlaylist] = useState([]);
  const [initialPlaylist, setInitialPlaylist] = useState([]);
  const [syncStatus, setSyncStatus] = useState('idle'); // 'idle', 'syncing', 'success'
  
  // 새로 추가된 상태
  const [syncEnabled, setSyncEnabled] = useState(false); // 재생 동기화 여부
  const [initialSyncEnabled, setInitialSyncEnabled] = useState(false);
  
  const [uploading, setUploading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  
  const [newDeviceId, setNewDeviceId] = useState('');
  const [newDeviceName, setNewDeviceName] = useState('');
  
  const fileInputRef = useRef(null);

  // 초기 설정
  useEffect(() => {
    if (stores.length > 0 && !selectedStoreId) {
      setSelectedStoreId(stores[0].id);
    }
  }, [stores, selectedStoreId]);

  useEffect(() => {
    const storeGroups = groups.filter(g => g.storeId === selectedStoreId);
    if (storeGroups.length > 0) {
      setSelectedGroupId(storeGroups[0].id);
    } else {
      setSelectedGroupId('');
    }
  }, [selectedStoreId, groups]);

  // 미디어 목록 불러오기
  useEffect(() => {
    if (!selectedStoreId) return;
    fetch(`${SOCKET_URL}/api/media?storeId=${selectedStoreId}`)
      .then(res => res.json())
      .then(setMediaList)
      .catch(err => console.error(err));
  }, [selectedStoreId]);

  // 재생목록 불러오기
  useEffect(() => {
    if (!selectedGroupId) {
      setPlaylist([]);
      setInitialPlaylist([]);
      return;
    }
    fetch(`${SOCKET_URL}/api/groups/${selectedGroupId}/playlist`)
      .then(res => res.json())
      .then(data => {
        if (data && data.medias) {
          // 정렬된 medias 배열을 state로 저장
          const mapped = data.medias.map(pm => ({
            ...pm.media, // 원본 media 정보
            duration: pm.duration, // 설정된 재생 시간
            playlistMediaId: pm.id, // 연결 ID
            targetDeviceId: pm.targetDeviceId || null,
            transition: pm.transition || 'fade' // 기본 전환 효과
          }));
          setPlaylist(mapped);
          setInitialPlaylist(JSON.parse(JSON.stringify(mapped)));
        } else {
          setPlaylist([]);
          setInitialPlaylist([]);
        }
        setSyncStatus('idle');
      })
      .catch(err => console.error(err));
  }, [selectedGroupId]);

  // 변경사항 감지 (스마트 비교)
  const stagedDeviceIds = useMemo(() => {
    const changed = new Set();
    const currentDevices = [...new Set([...playlist.map(i => i.targetDeviceId), ...initialPlaylist.map(i => i.targetDeviceId)])].filter(Boolean);
    
    currentDevices.forEach(dId => {
      const curr = playlist.filter(i => i.targetDeviceId === dId);
      const init = initialPlaylist.filter(i => i.targetDeviceId === dId);
      
      if (curr.length !== init.length) { 
        changed.add(dId); 
        return; 
      }
      
      for (let i = 0; i < curr.length; i++) {
        if (curr[i].id !== init[i].id || curr[i].duration !== init[i].duration || curr[i].transition !== init[i].transition) {
          changed.add(dId); 
          break;
        }
      }
    });
    return Array.from(changed);
  }, [playlist, initialPlaylist]);

  useEffect(() => {
    console.log('Playlist state updated. New length:', playlist.length);
  }, [playlist]);

  const hasChanges = useMemo(() => {
    if (syncEnabled !== initialSyncEnabled) return true;
    if (playlist.length !== initialPlaylist.length) return true;
    
    // 개별 항목 비교
    for (let i = 0; i < playlist.length; i++) {
      const p = playlist[i];
      const init = initialPlaylist[i];
      if (
        p.id !== init.id || 
        p.duration !== init.duration || 
        p.transition !== init.transition || 
        p.transitionTime !== init.transitionTime ||
        p.targetDeviceId !== init.targetDeviceId
      ) {
        return true;
      }
    }
    return false;
  }, [playlist, initialPlaylist, syncEnabled, initialSyncEnabled]);

  // 다중 미디어 업로드
  const handleUpload = async (e) => {
    const files = Array.from(e.target.files);
    if (files.length === 0 || !selectedStoreId) return;

    setUploading(true);
    
    try {
      const uploadPromises = files.map(file => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('storeId', selectedStoreId);
        
        return fetch(`${SOCKET_URL}/api/media`, {
          method: 'POST',
          body: formData
        }).then(res => res.json());
      });

      const newMedias = await Promise.all(uploadPromises);
      setMediaList(prev => [...newMedias, ...prev]);
    } catch (err) {
      console.error('업로드 실패', err);
      alert('일부 파일 업로드에 실패했습니다.');
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  // 라이브러리 항목 삭제
  const handleLibraryDelete = async (mediaId) => {
    if (!window.confirm('서버에서 이 미디어를 완전히 삭제하시겠습니까?')) return;
    try {
      const res = await fetch(`${SOCKET_URL}/api/media/${mediaId}`, { method: 'DELETE' });
      if (res.ok) {
        setMediaList(prev => prev.filter(m => m.id !== mediaId));
      }
    } catch (err) {
      console.error('삭제 실패', err);
      alert('미디어 삭제에 실패했습니다.');
    }
  };

  // 라이브러리 전체 삭제
  const handleDeleteAll = async () => {
    if (!selectedStoreId || mediaList.length === 0) return;
    if (!window.confirm('서버에 업로드된 이 사업장의 모든 미디어를 정말 삭제하시겠습니까? (이 작업은 되돌릴 수 없습니다)')) return;
    
    try {
      const res = await fetch(`${SOCKET_URL}/api/media?storeId=${selectedStoreId}`, { method: 'DELETE' });
      if (res.ok) {
        setMediaList([]);
      }
    } catch (err) {
      console.error('전체 삭제 실패', err);
      alert('전체 삭제에 실패했습니다.');
    }
  };

  // 모달 내 기기 할당
  const handleAssignDevice = async (deviceId) => {
    try {
      await fetch(`${SOCKET_URL}/api/devices/${deviceId}/group`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ groupId: selectedGroupId, storeId: selectedStoreId })
      });
      if (fetchDevices) fetchDevices(); // 기기 목록 새로고침
    } catch (err) {
      console.error('기기 할당 실패', err);
    }
  };

  // 모달 내 신규 기기 등록 및 할당
  const handleRegisterAndAssign = async () => {
    if (!newDeviceId || !newDeviceName) return alert('기기의 물리적 주소(ID)와 별칭을 모두 입력하세요.');
    try {
      // 1. 기기 등록 (Upsert)
      await fetch(`${SOCKET_URL}/api/devices`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: newDeviceId, name: newDeviceName, storeId: selectedStoreId })
      });
      // 2. 현재 구역으로 할당
      await fetch(`${SOCKET_URL}/api/devices/${newDeviceId}/group`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ groupId: selectedGroupId, storeId: selectedStoreId })
      });
      
      setNewDeviceId('');
      setNewDeviceName('');
      if (fetchDevices) fetchDevices();
      alert('새 기기가 성공적으로 등록되고 구역에 추가되었습니다.');
    } catch (err) {
      console.error(err);
      alert('기기 등록 실패');
    }
  };

  // 드래그 앤 드롭: 라이브러리 -> 재생목록
  const onDragStartLibrary = (e, media) => {
    e.dataTransfer.setData('application/json', JSON.stringify(media));
  };

  const onDropPlaylist = (e) => {
    e.preventDefault();
    const mediaStr = e.dataTransfer.getData('application/json');
    if (!mediaStr) return;
    const media = JSON.parse(mediaStr);
    
    // 재생목록 맨 끝에 추가 (기본 10초)
    setPlaylist([...playlist, { ...media, duration: 10 }]);
  };

  // 기기 개별 드롭
  const onDropDevice = (e, deviceId) => {
    e.preventDefault();
    e.stopPropagation(); // 부모 컨테이너(onDropPlaylist)로 이벤트 전달 방지
    const mediaStr = e.dataTransfer.getData('application/json');
    if (!mediaStr) return;
    const media = JSON.parse(mediaStr);
    
    setPlaylist([...playlist, { ...media, duration: 10, targetDeviceId: deviceId }]);
  };

  // 드래그 앤 드롭: 재생목록 순서 변경
  const [draggedItemIndex, setDraggedItemIndex] = useState(null);

  const onDragStartPlaylist = (e, index) => {
    setDraggedItemIndex(index);
    e.dataTransfer.effectAllowed = 'move';
  };

  const onDragOverPlaylist = (e, index) => {
    e.preventDefault();
    if (draggedItemIndex === null || draggedItemIndex === index) return;
    
    const items = [...playlist];
    const draggedItem = items[draggedItemIndex];
    items.splice(draggedItemIndex, 1);
    items.splice(index, 0, draggedItem);
    
    setDraggedItemIndex(index);
    setPlaylist(items);
  };

  // 재생목록에서 항목 제거
  const removeFromPlaylist = (index) => {
    const newPlaylist = [...playlist];
    newPlaylist.splice(index, 1);
    setPlaylist(newPlaylist);
  };

  // 시간 수정
  const updateDuration = (index, value) => {
    const newPlaylist = [...playlist];
    newPlaylist[index].duration = parseInt(value, 10) || 10;
    setPlaylist(newPlaylist);
  };

  // 전환 효과 수정
  const updateTransition = (index, value) => {
    const newPlaylist = [...playlist];
    newPlaylist[index].transition = value;
    setPlaylist(newPlaylist);
  };

  // 전환 시간 수정
  const updateTransitionTime = (index, value) => {
    const items = [...playlist];
    items[index].transitionTime = parseInt(value) || 1000;
    setPlaylist(items);
  };

  // 재생목록 저장 및 배포
  const savePlaylist = async () => {
    if (!selectedGroupId) return;

    console.log('Saving playlist. Item count:', playlist.length);
    setSyncStatus('syncing');

    const items = playlist.map(item => ({
      mediaId: item.id,
      duration: item.duration,
      targetDeviceId: item.targetDeviceId,
      transition: item.transition,
      transitionTime: item.transitionTime
    }));

    try {
      const response = await fetch(`${SOCKET_URL}/api/groups/${selectedGroupId}/playlist`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ items })
      });
      const data = await response.json();
      
      if(data.success) {
        console.log('Save successful');
        const currentPlaylistCopy = JSON.parse(JSON.stringify(playlist));
        setInitialPlaylist(currentPlaylistCopy);
        setInitialSyncEnabled(syncEnabled);
        setSyncStatus('success');
        setTimeout(() => setSyncStatus('idle'), 3000);
      } else {
        console.error('Save failed:', data.error);
        setSyncStatus('idle');
        alert('저장에 실패했습니다: ' + (data.error || '알 수 없는 오류'));
      }
    } catch (err) {
      console.error('저장 네트워크 에러:', err);
      setSyncStatus('idle');
      alert('서버와 통신 중 오류가 발생했습니다.');
    }
  };

  const storeGroups = groups.filter(g => g.storeId === selectedStoreId);

  return (
    <div style={{ display: 'flex', gap: '20px', height: '100%', flexDirection: 'column' }}>
      
      {/* 상단 컨트롤 바 */}
      <div className="glass-card" style={{ flex: 'none', height: 'auto', padding: '12px 20px', display: 'flex', flexDirection: 'row', gap: '20px', alignItems: 'center' }}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px', margin: 0, fontSize: '1.1rem' }}>
          <Film size={20} /> 미디어 편성
        </h2>
        
        <div style={{ width: '1px', height: '30px', background: 'var(--glass-border)' }}></div>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span style={{ color: 'var(--text-secondary)' }}>사업장:</span>
          <select value={selectedStoreId} onChange={e => setSelectedStoreId(e.target.value)} className="glass-select">
            {stores.length === 0 && <option value="">없음</option>}
            {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span style={{ color: 'var(--text-secondary)' }}>편성 구역(Group):</span>
          <select value={selectedGroupId} onChange={e => setSelectedGroupId(e.target.value)} className="glass-select">
            {storeGroups.length === 0 && <option value="">구역 없음</option>}
            {storeGroups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}
          </select>
        </div>

        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '16px' }}>
          {selectedGroupId && (
            <label 
              style={{ 
                display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', 
                fontSize: '0.9rem', color: syncEnabled ? '#10B981' : 'var(--text-secondary)',
                padding: '6px 12px', background: 'rgba(0,0,0,0.2)', borderRadius: '6px',
                border: syncEnabled ? '1px solid rgba(16, 185, 129, 0.3)' : '1px solid transparent'
              }}
            >
              <input 
                type="checkbox" 
                checked={syncEnabled} 
                onChange={(e) => setSyncEnabled(e.target.checked)} 
                style={{ cursor: 'pointer' }}
              />
              재생 시간 동기화 (Sync)
            </label>
          )}
          <button 
            className="btn" 
            onClick={savePlaylist} 
            disabled={!selectedGroupId || !hasChanges || syncStatus === 'syncing'} 
            style={{ 
              background: hasChanges ? '#F59E0B' : 'rgba(255,255,255,0.08)', 
              color: hasChanges ? '#fff' : 'rgba(255,255,255,0.3)',
              border: hasChanges ? '1px solid #F59E0B' : '1px solid rgba(255,255,255,0.1)',
              padding: '10px 20px',
              borderRadius: '8px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontWeight: '600',
              cursor: hasChanges ? 'pointer' : 'not-allowed',
              boxShadow: hasChanges ? '0 4px 15px rgba(245, 158, 11, 0.4)' : 'none',
              transform: hasChanges ? 'scale(1.05)' : 'scale(1)',
              transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
            }}
          >
            <Save size={18} /> {syncStatus === 'syncing' ? '배포 중...' : syncStatus === 'success' ? '배포 완료!' : hasChanges ? '변경사항 저장 및 배포' : '저장 및 기기 배포'}
          </button>
        </div>
      </div>

      <div style={{ display: 'flex', gap: '10px', flex: 1, minHeight: '400px', overflow: 'hidden', position: 'relative' }}>
        
        {/* 왼쪽: 미디어 라이브러리 */}
        <div 
          className="glass-card" 
          style={{ 
            flex: libraryCollapsed ? '0 0 40px' : '0 0 280px', 
            height: '100%', 
            display: 'flex', 
            flexDirection: 'column', 
            padding: libraryCollapsed ? '12px 5px' : '12px',
            transition: 'all 0.3s ease',
            overflow: 'hidden'
          }}
        >
          {!libraryCollapsed ? (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h3 style={{ margin: 0, fontSize: '0.85rem', whiteSpace: 'nowrap', color: 'var(--text-secondary)' }}>에셋 라이브러리</h3>
                <input type="file" ref={fileInputRef} onChange={handleUpload} style={{ display: 'none' }} accept="video/*,image/*" multiple />
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button 
                    className="btn btn-secondary" 
                    style={{ padding: '6px 10px', fontSize: '0.8rem', background: 'rgba(239, 68, 68, 0.1)', color: '#EF4444', border: '1px solid rgba(239, 68, 68, 0.3)' }} 
                    onClick={handleDeleteAll} 
                    disabled={mediaList.length === 0}
                    title="전체 삭제"
                  >
                    <Trash2 size={14} />
                  </button>
                  <button className="btn btn-primary" style={{ padding: '6px 12px', fontSize: '0.8rem' }} onClick={() => fileInputRef.current.click()} disabled={uploading}>
                    <Upload size={14} />
                  </button>
                </div>
              </div>
              
              <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '4px', paddingRight: '4px' }}>
                {mediaList.length === 0 && (
                  <div style={{ color: 'var(--text-secondary)', textAlign: 'center', marginTop: '40px', fontSize: '0.8rem' }}>
                    업로드된 미디어가 없습니다.
                  </div>
                )}
                {mediaList.map(media => (
                  <div 
                    key={media.id} 
                    draggable 
                    onDragStart={(e) => onDragStartLibrary(e, media)}
                    style={{ 
                      padding: '6px 8px', cursor: 'grab', display: 'flex', gap: '10px', alignItems: 'center', 
                      background: 'rgba(255,255,255,0.02)', borderBottom: '1px solid rgba(255,255,255,0.05)',
                      borderRadius: '4px', transition: 'background 0.2s'
                    }}
                    onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.05)'}
                    onMouseLeave={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.02)'}
                  >
                    <div style={{ width: '40px', height: '30px', background: '#000', borderRadius: '3px', overflow: 'hidden', flexShrink: 0 }}>
                      {media.type === 'video' ? (
                        <video src={`${SOCKET_URL}${media.path}#t=1`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      ) : (
                        <img src={`${SOCKET_URL}${media.path}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} alt="thumb" />
                      )}
                    </div>
                    <div style={{ flex: 1, overflow: 'hidden' }}>
                      <div style={{ fontSize: '0.75rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', color: 'rgba(255,255,255,0.8)' }}>
                        {media.filename}
                      </div>
                    </div>
                    <button 
                      onClick={() => handleLibraryDelete(media.id)}
                      style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.3)', cursor: 'pointer', padding: '4px' }}
                      onMouseEnter={(e) => e.currentTarget.style.color = '#EF4444'}
                      onMouseLeave={(e) => e.currentTarget.style.color = 'rgba(255,255,255,0.3)'}
                    >
                      <Trash2 size={12} />
                    </button>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '20px', height: '100%' }}>
              <ImageIcon size={20} color="var(--text-secondary)" />
              <div style={{ writingMode: 'vertical-rl', textOrientation: 'mixed', color: 'var(--text-secondary)', fontSize: '0.8rem', letterSpacing: '2px' }}>LIBRARY</div>
            </div>
          )}
          
        </div>

        {/* 라이브러리 토글 버튼 (초소형 삼각형, 뷰포트 중앙 고정) */}
        {!previewData && (
          <button 
            onClick={() => setLibraryCollapsed(!libraryCollapsed)}
            style={{ 
              position: 'fixed', 
              top: '50%',
              // 사이드바(260px) + 메인패딩(24px) + 라이브러리 너비에 맞춘 위치 계산
              left: `calc(260px + 24px + ${libraryCollapsed ? '8px' : '272px'})`,
              transform: 'translate(-50%, -50%)',
              width: '6px', 
              height: '12px',
              background: '#3B82F6', 
              border: 'none',
              cursor: 'pointer', 
              zIndex: 9999, 
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              clipPath: libraryCollapsed ? 'polygon(0 0, 100% 50%, 0 100%)' : 'polygon(100% 0, 0 50%, 100% 100%)',
              filter: 'drop-shadow(0 0 2px rgba(0,0,0,0.8))',
              transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
              padding: 0
            }}
            title={libraryCollapsed ? "라이브러리 펼치기" : "라이브러리 접기"}
          />
        )}

        {/* 오른쪽: 재생목록 (타임라인) 또는 미리보기 */}
        <div className="glass-card" style={{ flex: 1, display: 'flex', flexDirection: 'column', padding: previewData ? '0' : '20px', overflow: 'hidden', position: 'relative' }}>
          {previewData ? (
            <DevicePlaylistPreview 
              playlist={previewData.playlist} 
              startIndex={previewData.startIndex}
              onUpdateItem={(idx, updates) => {
                const items = [...playlist];
                const item = items[idx];
                if (item) {
                  items[idx] = { ...item, ...updates };
                  setPlaylist(items);
                }
              }}
              onClose={() => setPreviewData(null)} 
            />
          ) : (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', padding: '0 20px' }}>
                <h3 style={{ margin: 0, fontSize: '1.1rem' }}>
                  {storeGroups.find(g => g.id === selectedGroupId)?.name || '선택됨'} - 재생목록 타임라인
                </h3>
                
                {selectedGroupId && (
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button 
                      onClick={() => {
                        if (playlist.length === 0) return;
                        setPlaylist([]);
                      }}
                      disabled={playlist.length === 0}
                      style={{ 
                        display: 'flex', alignItems: 'center', gap: '4px',
                        padding: '6px 10px', borderRadius: '6px', 
                        border: '1px solid rgba(239, 68, 68, 0.3)',
                        background: 'rgba(239, 68, 68, 0.1)', color: '#EF4444',
                        fontSize: '0.85rem', cursor: playlist.length === 0 ? 'not-allowed' : 'pointer',
                        opacity: playlist.length === 0 ? 0.5 : 1
                      }}
                    >
                      <Trash2 size={14} /> 전체 비우기
                    </button>
                    <button 
                      onClick={() => setIsModalOpen(true)}
                      style={{ 
                        display: 'flex', alignItems: 'center', gap: '4px',
                        padding: '6px 12px', borderRadius: '6px', 
                        border: '1px solid rgba(59, 130, 246, 0.5)',
                        background: 'rgba(59, 130, 246, 0.1)', color: '#3B82F6',
                        fontSize: '0.85rem', cursor: 'pointer'
                      }}
                    >
                      + 기기 추가
                    </button>
                  </div>
                )}
              </div>
              
              <div 
                style={{ 
                  flex: 1, overflowY: 'auto', background: 'rgba(0,0,0,0.1)', 
                  borderRadius: '12px', border: '2px dashed rgba(255,255,255,0.2)',
                  display: 'flex', flexDirection: 'column', padding: '16px'
                }}
              >
                {selectedGroupId && devices && devices.filter(d => d.groupId === selectedGroupId).length > 0 ? (
                  devices.filter(d => d.groupId === selectedGroupId).map(d => {
                    const isStaged = stagedDeviceIds.includes(d.id);
                    const isCollapsed = collapsedDevices[d.id];
                    const devicePlaylist = playlist.map((item, idx) => ({ ...item, actualIndex: idx }))
                                                   .filter(item => item.targetDeviceId === d.id || item.targetDeviceId == null);
                    
                    return (
                      <div 
                        key={d.id} 
                        style={{ 
                          display: 'flex', gap: '16px', marginBottom: '16px', 
                          alignItems: 'stretch'
                        }}
                      >
                        {/* 왼쪽: 기기 정보 뱃지 */}
                        <div style={{ flex: '0 0 220px', display: 'flex', flexDirection: 'column' }}>
                          <div 
                            style={{ 
                              display: 'flex', alignItems: 'center', gap: '8px',
                              padding: '12px 16px', borderRadius: '8px', 
                              border: isStaged ? '1px solid #F59E0B' : syncStatus === 'success' ? '1px solid #10B981' : '1px solid rgba(255,255,255,0.2)',
                              background: syncStatus === 'success' ? 'rgba(16, 185, 129, 0.2)' : isStaged ? 'rgba(245, 158, 11, 0.1)' : 'rgba(0,0,0,0.3)',
                              fontSize: '0.9rem', transition: 'all 0.3s ease', cursor: 'pointer'
                            }}
                            onClick={() => setCollapsedDevices(prev => ({ ...prev, [d.id]: !prev[d.id] }))}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1 }}>
                              {syncStatus === 'syncing' ? (
                                <span style={{ fontSize: '1rem' }}>🔄</span>
                              ) : syncStatus === 'success' ? (
                                <span style={{ color: '#10B981', fontSize: '1rem' }}>✅</span>
                              ) : (
                                <span style={{ color: d.status === 'online' ? '#10B981' : '#EF4444' }}>●</span>
                              )}
                              <span style={{ fontWeight: '600' }}>{d.name}</span>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              {isStaged && syncStatus === 'idle' && (
                                <span style={{ fontSize: '0.75rem', color: '#F59E0B' }}>(배포 대기)</span>
                              )}
                              {isCollapsed ? <ChevronDown size={16} /> : <ChevronUp size={16} />}
                            </div>
                          </div>
                        </div>

                        {/* 오른쪽: 기기별 재생목록 (가로 스크롤) */}
                        {!isCollapsed && (
                          <div 
                            onDragOver={(e) => e.preventDefault()}
                            onDrop={(e) => onDropDevice(e, d.id)}
                            style={{ 
                              flex: 1, display: 'flex', gap: '12px', overflowX: 'auto', 
                              background: 'rgba(255,255,255,0.03)', borderRadius: '8px', 
                              padding: '12px', minHeight: '100px', border: '1px dashed rgba(255,255,255,0.1)'
                            }}
                          >
                          {devicePlaylist.length === 0 ? (
                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: 'auto', pointerEvents: 'none' }}>
                              여기에 에셋을 드래그하세요
                            </div>
                          ) : (
                            devicePlaylist.map((item, localIndex) => {
                              const nextItem = devicePlaylist[(localIndex + 1) % devicePlaylist.length];
                              const isLast = localIndex === devicePlaylist.length - 1;
                              
                              return (
                                <React.Fragment key={`${item.id}-${item.actualIndex}`}>
                                  <div 
                                    draggable
                                    onDragStart={(e) => onDragStartPlaylist(e, item.actualIndex)}
                                    onDragOver={(e) => {
                                      e.preventDefault();
                                      if (draggedItemIndex !== null && draggedItemIndex !== item.actualIndex) {
                                        const items = [...playlist];
                                        const draggedItem = items[draggedItemIndex];
                                        draggedItem.targetDeviceId = d.id;
                                        items.splice(draggedItemIndex, 1);
                                        items.splice(item.actualIndex, 0, draggedItem);
                                        setDraggedItemIndex(item.actualIndex);
                                        setPlaylist(items);
                                      }
                                    }}
                                    onDragEnd={() => setDraggedItemIndex(null)}
                                    style={{
                                      flex: '0 0 140px', display: 'flex', flexDirection: 'column', 
                                      background: 'var(--glass-bg)', border: '1px solid var(--glass-border)',
                                      borderRadius: '8px', opacity: draggedItemIndex === item.actualIndex ? 0.5 : 1,
                                      cursor: 'grab', padding: '6px', position: 'relative',
                                      zIndex: 5
                                    }}
                                  >
                                    <div style={{ width: '100%', height: '80px', background: 'rgba(0,0,0,0.3)', borderRadius: '4px', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', position: 'relative' }}>
                                      {item.type === 'video' ? (
                                        <>
                                          <video src={`${SOCKET_URL}${item.path}#t=1`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} preload="metadata" />
                                          <div style={{ position: 'absolute', right: '4px', bottom: '4px', background: 'rgba(0,0,0,0.7)', borderRadius: '2px', padding: '2px 4px', fontSize: '10px', color: '#fff' }}>
                                            VID
                                          </div>
                                        </>
                                      ) : (
                                        <img src={`${SOCKET_URL}${item.path}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} alt="thumb" />
                                      )}
                                    </div>
                                    
                                    <div style={{ fontSize: '0.85rem', fontWeight: '500', marginTop: '8px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                      {item.filename}
                                    </div>
                                    
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '12px', background: 'rgba(0,0,0,0.2)', padding: '6px', borderRadius: '6px' }}>
                                      <Clock size={14} color="var(--text-secondary)" />
                                      <input 
                                        type="number" 
                                        value={item.duration} 
                                        onChange={(e) => updateDuration(item.actualIndex, e.target.value)}
                                        style={{ 
                                          width: '50px', padding: '4px 6px', borderRadius: '6px', 
                                          border: '1px solid var(--glass-border)', background: 'rgba(255,255,255,0.05)', 
                                          color: '#fff', textAlign: 'center', fontSize: '0.85rem',
                                          fontWeight: '600'
                                        }}
                                      />
                                      <span style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>초 재생</span>
                                    </div>
                                    
                                    <button 
                                      onClick={() => removeFromPlaylist(item.actualIndex)}
                                      style={{ position: 'absolute', top: '4px', right: '4px', background: 'rgba(0,0,0,0.6)', border: 'none', color: '#EF4444', cursor: 'pointer', padding: '4px', borderRadius: '4px' }}
                                    >
                                      <Trash2 size={14} />
                                    </button>
                                  </div>

                                  {/* 전환 효과 선택기 (이 아이템 -> 다음 아이템) */}
                                  {devicePlaylist.length > 1 && (
                                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '4px', padding: '0 6px', zIndex: 10 }}>
                                      <div style={{ width: '2px', height: '10px', background: 'rgba(255,255,255,0.1)', borderRadius: '2px' }}></div>
                                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px', position: 'relative' }}>
                                        {isLast && (
                                          <div style={{ position: 'absolute', top: '-15px', fontSize: '0.55rem', color: '#F59E0B', whiteSpace: 'nowrap', fontWeight: 'bold' }}>
                                            LOOP ↩
                                          </div>
                                        )}
                                        <select 
                                          value={nextItem.transition || 'fade'}
                                          onChange={(e) => updateTransition(nextItem.actualIndex, e.target.value)}
                                          style={{
                                            padding: '3px 4px', borderRadius: '4px', fontSize: '0.7rem',
                                            background: 'rgba(30, 41, 59, 0.8)', color: '#fff', border: '1px solid rgba(255, 255, 255, 0.2)',
                                            width: '75px', cursor: 'pointer', outline: 'none'
                                          }}
                                        >
                                          <option value="none">컷</option>
                                          <option value="fade">페이드</option>
                                          <option value="crossfade">디졸브</option>
                                          <option value="slide">밀기</option>
                                        </select>

                                        <div style={{ position: 'relative', width: '75px' }}>
                                          <input 
                                            type="text" 
                                            value={nextItem.transitionTime || 1000} 
                                            onChange={(e) => {
                                              const val = e.target.value.replace(/[^0-9]/g, '');
                                              updateTransitionTime(nextItem.actualIndex, val);
                                            }}
                                            style={{ 
                                              width: '100%', padding: '3px 22px 3px 4px', borderRadius: '4px', 
                                              border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(0,0,0,0.4)', 
                                              color: '#fff', textAlign: 'left', fontSize: '0.75rem',
                                              fontWeight: '600', outline: 'none', boxSizing: 'border-box'
                                            }}
                                          />
                                          <span style={{ 
                                            position: 'absolute', right: '4px', top: '50%', transform: 'translateY(-50%)',
                                            fontSize: '0.6rem', color: 'rgba(255,255,255,0.4)', pointerEvents: 'none' 
                                          }}>ms</span>
                                        </div>
                                        
                                        <button 
                                          onClick={() => setPreviewData({ playlist: devicePlaylist, startIndex: localIndex })}
                                          style={{ 
                                            background: 'rgba(255, 255, 255, 0.08)', border: '1px solid rgba(255,255,255,0.15)', 
                                            borderRadius: '4px', padding: '3px 0', cursor: 'pointer',
                                            color: '#fff', fontSize: '0.65rem', width: '75px', whiteSpace: 'nowrap',
                                            fontWeight: '500'
                                          }}
                                          onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(59, 130, 246, 0.4)'}
                                          onMouseLeave={(e) => e.currentTarget.style.background = 'rgba(255, 255, 255, 0.08)'}
                                        >
                                          미리보기
                                        </button>
                                      </div>
                                      <div style={{ width: '2px', height: '10px', background: 'rgba(255,255,255,0.1)', borderRadius: '2px' }}></div>
                                    </div>
                                  )}
                                </React.Fragment>
                              );
                            })
                          )}
                        </div>
                      )}
                    </div>
                  );
                })
                ) : (
                  <div style={{ margin: 'auto', color: 'var(--text-secondary)', textAlign: 'center', paddingTop: '40px' }}>
                    <p>연결된 기기가 없습니다. 기기를 추가해 주세요.</p>
                  </div>
                )}
              </div>
            </>
          )}
        </div>

      </div>

      {/* 기기 추가 모달 */}
      {isModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }}>
          <div className="glass-card" style={{ width: '450px', padding: '24px', display: 'block' }}>
            <h3 style={{ marginTop: 0 }}>'{storeGroups.find(g => g.id === selectedGroupId)?.name}' 구역에 기기 추가</h3>
            
            <div style={{ background: 'rgba(0,0,0,0.2)', padding: '16px', borderRadius: '8px', marginBottom: '20px', border: '1px solid rgba(255,255,255,0.1)' }}>
              <h4 style={{ margin: '0 0 12px 0', fontSize: '0.95rem', color: '#60A5FA' }}>+ 신규 물리 기기 등록</h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <input 
                  type="text" 
                  placeholder="물리적 주소 (예: MAC 또는 시리얼번호)" 
                  value={newDeviceId} 
                  onChange={e => setNewDeviceId(e.target.value)} 
                  style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(0,0,0,0.3)', color: '#fff' }}
                />
                <input 
                  type="text" 
                  placeholder="기기 별칭 (예: 로비 우측 사이니지)" 
                  value={newDeviceName} 
                  onChange={e => setNewDeviceName(e.target.value)} 
                  style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(0,0,0,0.3)', color: '#fff' }}
                />
                <button className="btn btn-primary" onClick={handleRegisterAndAssign} style={{ marginTop: '8px', padding: '10px' }}>
                  등록 후 이 구역에 즉시 할당
                </button>
              </div>
            </div>

            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', margin: '0 0 12px 0' }}>또는 현재 사업장의 타 구역/미배정 기기 가져오기</p>
            
            <div style={{ maxHeight: '200px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '8px', paddingRight: '4px' }}>
              {devices && devices.filter(d => d.storeId === selectedStoreId && d.groupId !== selectedGroupId).length === 0 ? (
                <div style={{ textAlign: 'center', padding: '20px', color: 'var(--text-secondary)' }}>가져올 수 있는 기기가 없습니다.</div>
              ) : (
                devices && devices.filter(d => d.storeId === selectedStoreId && d.groupId !== selectedGroupId).map(d => (
                  <div key={d.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px', background: 'rgba(255,255,255,0.05)', borderRadius: '8px' }}>
                    <div>
                      <div>{d.name} <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>({d.id})</span></div>
                      <div style={{ fontSize: '0.8rem', color: d.status === 'online' ? '#10B981' : '#EF4444' }}>
                        {d.status === 'online' ? '온라인' : '오프라인'}
                        <span style={{ color: 'var(--text-secondary)', marginLeft: '4px' }}>
                          (현재: {groups.find(g => g.id === d.groupId)?.name || '미배정'})
                        </span>
                      </div>
                    </div>
                    <button className="btn btn-primary" style={{ padding: '4px 12px', fontSize: '0.8rem' }} onClick={() => handleAssignDevice(d.id)}>가져오기</button>
                  </div>
                ))
              )}
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '24px', paddingTop: '16px', borderTop: '1px dashed rgba(255,255,255,0.1)' }}>
              <button className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>닫기</button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}

function DevicePlaylistPreview({ playlist, startIndex = 0, onUpdateItem, onClose }) {
  const [currentIndex, setCurrentIndex] = useState(startIndex);
  const [prevIndex, setPrevIndex] = useState(-1);
  const [currentTime, setCurrentTime] = useState(0); 
  const [isPaused, setIsPaused] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [isFullScreen, setIsFullScreen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const containerRef = useRef(null);
  const timerRef = useRef(null);

  const activeItem = playlist[currentIndex];
  const prevIndexCalculated = prevIndex !== -1 ? prevIndex : (playlist.length > 1 ? (currentIndex - 1 + playlist.length) % playlist.length : -1);
  const prevItem = prevIndexCalculated !== -1 ? playlist[prevIndexCalculated] : null;
  
  const durationMs = (activeItem?.duration || 10) * 1000;
  const transType = activeItem?.transition || 'fade';
  const transTime = activeItem?.transitionTime || 1000;

  useEffect(() => {
    if (!playlist.length || isPaused || isDragging) return;
    const tick = 33; 
    timerRef.current = setInterval(() => {
      setCurrentTime(prev => {
        const next = prev + tick;
        if (next >= durationMs) {
          setPrevIndex(currentIndex);
          setCurrentIndex((prevIdx) => (prevIdx + 1) % playlist.length);
          return 0;
        }
        return next;
      });
    }, tick);
    return () => clearInterval(timerRef.current);
  }, [currentIndex, playlist.length, durationMs, isPaused, isDragging]);

  const toggleFullScreen = () => {
    if (!document.fullscreenElement) {
      const elem = containerRef.current;
      if (elem.requestFullscreen) elem.requestFullscreen();
      else if (elem.webkitRequestFullscreen) elem.webkitRequestFullscreen();
      else if (elem.msRequestFullscreen) elem.msRequestFullscreen();
    } else {
      if (document.exitFullscreen) document.exitFullscreen();
    }
  };

  useEffect(() => {
    const handleFSChange = () => setIsFullScreen(!!document.fullscreenElement);
    document.addEventListener('fullscreenchange', handleFSChange);
    return () => document.removeEventListener('fullscreenchange', handleFSChange);
  }, []);

  const renderMedia = (item) => {
    if (!item) return null;
    const media = item.media || item;
    const style = { width: '100%', height: '100%', objectFit: 'contain' };
    return media.type === 'video' ? (
      <video src={`${SOCKET_URL}${media.path}`} style={style} autoPlay muted loop />
    ) : (
      <img src={`${SOCKET_URL}${media.path}`} style={style} alt="preview" />
    );
  };

  const progress = Math.min(currentTime / transTime, 1);
  const getActiveOpacity = () => (transType === 'none' || currentTime > transTime) ? 1 : progress;
  const getPrevOpacity = () => (transType === 'crossfade' && currentTime <= transTime) ? 1 - progress : 1;
  const getTransform = () => (transType === 'slide' && currentTime < transTime) ? `translateX(${(1 - progress) * 100}%)` : 'none';

  return (
    <div 
      ref={containerRef}
      style={{
        width: '100%', height: '100%',
        background: '#000', display: 'flex', flexDirection: 'column',
        borderRadius: '12px', overflow: 'hidden'
      }}
    >
      {/* 상단: 미리보기 영역 (전환 결과물) */}
      <div style={{ 
        position: isEditMode ? 'relative' : 'absolute',
        top: 0, left: 0, width: '100%',
        height: isEditMode ? '40%' : '100%', 
        overflow: 'hidden', background: '#000', transition: 'all 0.3s ease',
        zIndex: 1
      }}>
        {prevItem && currentTime < transTime && (
          <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 1, opacity: getPrevOpacity() }}>
            {renderMedia(prevItem)}
          </div>
        )}
        <div style={{ 
          position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 2,
          opacity: (transType === 'fade' || transType === 'crossfade') ? getActiveOpacity() : 1,
          transform: getTransform(),
          transition: isDragging ? 'none' : 'opacity 0.05s linear, transform 0.05s linear'
        }}>
          {renderMedia(activeItem)}
        </div>
        {isEditMode && <div style={{ position: 'absolute', bottom: '10px', left: '10px', background: 'rgba(0,0,0,0.6)', color: '#3B82F6', padding: '2px 8px', borderRadius: '4px', fontSize: '0.7rem', fontWeight: 'bold', zIndex: 10 }}>PREVIEW</div>}
      </div>

      {/* 하단: 프로 전환 편집기 (Based on user sketch) */}
      <div style={{ 
        position: isEditMode ? 'relative' : 'absolute',
        bottom: 0, left: 0, width: '100%',
        height: isEditMode ? '60%' : '80px', 
        background: isEditMode ? '#1a1a1a' : 'rgba(0,0,0,0.7)', 
        backdropFilter: isEditMode ? 'none' : 'blur(10px)', 
        display: 'flex', flexDirection: 'column', 
        borderTop: '1px solid rgba(255,255,255,0.1)', 
        zIndex: 10002, transition: 'all 0.4s ease',
        padding: isEditMode ? '30px' : '0 20px', 
        overflow: 'hidden'
      }}>
        {isEditMode && (
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '20px', position: 'relative' }}>
            {/* 전환 구간 브라켓 (Dissolve Start -> End) */}
            <div style={{ position: 'relative', height: '30px', display: 'flex', justifyContent: 'center', marginBottom: '10px' }}>
              <div style={{ 
                width: '300px', height: '20px', border: '2px solid rgba(59, 130, 246, 0.5)', 
                borderBottom: 'none', position: 'relative' 
              }}>
                <span style={{ position: 'absolute', top: '-18px', left: '-10px', fontSize: '0.7rem', color: '#fff' }}>Dissolve start</span>
                <span style={{ position: 'absolute', top: '-18px', right: '-10px', fontSize: '0.7rem', color: '#fff' }}>end</span>
                <span style={{ position: 'absolute', top: '-5px', left: '50%', transform: 'translateX(-50%)', fontSize: '0.75rem', fontWeight: 'bold', color: '#3B82F6' }}>{transTime}ms</span>
              </div>
            </div>

            {/* 메인 타임라인 트랙 (Dual Track) */}
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px', padding: '0 50px', position: 'relative' }}>
              {/* Track 1: image1 (Fading Out) */}
              <div style={{ height: '70px', display: 'flex', gap: '2px', alignItems: 'center' }}>
                <div style={{ width: '80px', color: '#fff', fontSize: '0.8rem', fontWeight: 'bold' }}>image 1</div>
                <div style={{ flex: 1, display: 'flex', gap: '2px', position: 'relative' }}>
                  {[100, 100, 100, 90, 50, 10].map((opacity, i) => (
                    <div key={i} style={{ width: '60px', height: '60px', background: 'rgba(255,255,255,0.05)', borderRadius: '4px', border: '1px solid rgba(255,255,255,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative', opacity: opacity/100 }}>
                      <img src={`${SOCKET_URL}${prevItem?.path}`} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '4px' }} />
                      <div style={{ position: 'absolute', bottom: '2px', right: '2px', fontSize: '10px', color: '#fff', fontWeight: 'bold', textShadow: '0 0 4px #000' }}>{opacity}%</div>
                    </div>
                  ))}
                  <div style={{ flex: 1, borderBottom: '1px dashed rgba(255,255,255,0.1)' }}></div>
                </div>
              </div>

              {/* Track 2: image2 (Fading In) */}
              <div style={{ height: '70px', display: 'flex', gap: '2px', alignItems: 'center' }}>
                <div style={{ width: '80px', color: '#fff', fontSize: '0.8rem', fontWeight: 'bold' }}>image 2</div>
                <div style={{ flex: 1, display: 'flex', gap: '2px', position: 'relative' }}>
                  <div style={{ width: '186px', borderBottom: '1px dashed rgba(255,255,255,0.1)' }}></div>
                  {[10, 50, 90, 100, 100, 100].map((opacity, i) => (
                    <div key={i} style={{ width: '60px', height: '60px', background: 'rgba(255,255,255,0.05)', borderRadius: '4px', border: '1px solid rgba(255,255,255,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative', opacity: opacity/100 }}>
                      <img src={`${SOCKET_URL}${activeItem?.path}`} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '4px' }} />
                      <div style={{ position: 'absolute', bottom: '2px', right: '2px', fontSize: '10px', color: '#fff', fontWeight: 'bold', textShadow: '0 0 4px #000' }}>{opacity}%</div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Seeker / Ruler */}
              <div style={{ position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)', height: '100%', width: '1px', background: '#3B82F6', zIndex: 10 }}>
                <div style={{ position: 'absolute', top: '-10px', left: '-25px', width: '50px', background: '#3B82F6', color: '#fff', fontSize: '10px', textAlign: 'center', borderRadius: '2px' }}>{Math.floor(currentTime)}ms</div>
              </div>
            </div>

            {/* 전환 타입 메뉴 (Bottom Selection) */}
            <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', padding: '10px' }}>
              {[
                { id: 'crossfade', label: 'Dissolve' },
                { id: 'none', label: 'Cut' },
                { id: 'fade', label: '페이드' },
                { id: 'slide', label: '밀어내기' }
              ].map(type => (
                <button 
                  key={type.id}
                  onClick={() => onUpdateItem?.(activeItem.actualIndex, { transition: type.id })}
                  style={{ 
                    padding: '8px 20px', background: transType === type.id ? '#3B82F6' : '#2a2a2a', 
                    border: '1px solid rgba(255,255,255,0.1)', color: '#fff', borderRadius: '4px', 
                    cursor: 'pointer', fontSize: '0.8rem', fontWeight: 'bold', minWidth: '100px'
                  }}
                >
                  {type.label}
                </button>
              ))}
            </div>

            <div style={{ position: 'absolute', bottom: 0, width: '100%', padding: '0 20px' }}>
              <input 
                type="range" min={0} max={durationMs} value={currentTime}
                onChange={(e) => setCurrentTime(parseInt(e.target.value))}
                onMouseDown={() => setIsDragging(true)}
                onMouseUp={() => setIsDragging(false)}
                style={{ width: '100%', accentColor: '#3B82F6' }}
              />
            </div>
          </div>
        )}

        {/* 통합 하단 컨트롤 줄 (Preview Only Mode) */}
        <div style={{ height: '80px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', position: 'relative' }}>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button 
              onClick={() => setIsPaused(!isPaused)} 
              style={{ background: isPaused ? '#3B82F6' : 'rgba(255,255,255,0.1)', border: 'none', borderRadius: '6px', padding: '8px 16px', color: '#fff', fontWeight: '700', cursor: 'pointer', fontSize: '0.8rem' }}
            >
              {isPaused ? '▶ PLAY' : '⏸ PAUSE'}
            </button>
            <button 
              onClick={() => setIsEditMode(!isEditMode)} 
              style={{ background: isEditMode ? '#3B82F6' : 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.2)', borderRadius: '6px', padding: '8px 16px', color: '#fff', fontWeight: '700', cursor: 'pointer', fontSize: '0.8rem' }}
            >
              {isEditMode ? '편집 종료' : '🛠 편집'}
            </button>
          </div>

          <div style={{ display: 'flex', gap: '6px', alignItems: 'center', position: 'absolute', left: '50%', transform: 'translateX(-50%)' }}>
            {playlist.map((_, idx) => (
              <div key={idx} style={{ width: idx === currentIndex ? '16px' : '6px', height: '6px', borderRadius: '3px', background: idx === currentIndex ? '#3B82F6' : 'rgba(255,255,255,0.3)', transition: 'all 0.3s ease' }} />
            ))}
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <button onClick={toggleFullScreen} style={{ background: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.15)', borderRadius: '6px', padding: '8px 12px', color: '#fff', cursor: 'pointer', fontSize: '0.8rem' }}>
              <Maximize size={14} />
            </button>
            <button onClick={onClose} style={{ background: '#EF4444', border: 'none', borderRadius: '6px', padding: '8px 16px', color: '#fff', cursor: 'pointer', fontWeight: '700', fontSize: '0.8rem' }}>
              미리보기 종료
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

