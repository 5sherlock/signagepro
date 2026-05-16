import { useState, useEffect } from 'react';
import { Monitor, Plus, FolderTree, Building, HardDrive, Trash2, GripVertical } from 'lucide-react';
import { SOCKET_URL } from '../config';

export default function GroupManager({ devices, fetchDevices, stores, fetchStores, groups, fetchGroups, selectedStoreId, setSelectedStoreId }) {
  const [newStoreName, setNewStoreName] = useState('');
  const [newGroupName, setNewGroupName] = useState('');
  const [newDeviceId, setNewDeviceId] = useState('');
  const [newDeviceName, setNewDeviceName] = useState('');

  // Initial select
  useEffect(() => {
    if (stores.length > 0 && !selectedStoreId) {
      setSelectedStoreId(stores[0].id);
    }
  }, [stores, selectedStoreId, setSelectedStoreId]);

  const handleCreateStore = () => {
    if (!newStoreName.trim()) return;
    fetch(`${SOCKET_URL}/api/stores`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: newStoreName })
    }).then(res => {
      if (!res.ok) throw new Error('서버 응답 오류');
      setNewStoreName('');
      fetchStores();
    }).catch(err => {
      console.error('사업장 추가 실패:', err);
      alert('사업장 추가에 실패했습니다. 서버가 실행 중인지 확인해주세요.');
    });
  };

  const handleDeleteStore = (id) => {
    const targetId = id || selectedStoreId;
    if (!targetId) return;
    const storeName = stores.find(s => s.id === targetId)?.name;
    if (!window.confirm(`사업장 [${storeName}]을(를) 삭제하시겠습니까? \n모든 관련 구역, 재생목록, 미디어가 함께 삭제됩니다.`)) return;

    fetch(`${SOCKET_URL}/api/stores/${targetId}`, {
      method: 'DELETE'
    }).then(res => res.json())
      .then(data => {
        if (data.success) {
          if (selectedStoreId === targetId) setSelectedStoreId('');
          fetchStores();
        } else {
          alert('삭제 실패: ' + data.details);
        }
      });
  };

  const handleCreateGroup = () => {
    if (!newGroupName.trim() || !selectedStoreId) return;
    fetch(`${SOCKET_URL}/api/groups`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: newGroupName, storeId: selectedStoreId })
    }).then(() => {
      setNewGroupName('');
      fetchGroups();
    });
  };

  const handleRegisterDevice = () => {
    if (!newDeviceId.trim() || !newDeviceName.trim() || !selectedStoreId) return;
    fetch(`${SOCKET_URL}/api/devices`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: newDeviceId.trim(), name: newDeviceName.trim(), storeId: selectedStoreId })
    }).then(() => {
      setNewDeviceId('');
      setNewDeviceName('');
      if (fetchDevices) fetchDevices();
    });
  };

  const onDragStart = (e, deviceId) => {
    e.dataTransfer.setData('deviceId', deviceId);
  };

  const onDrop = (e, groupId, storeId) => {
    e.preventDefault();
    const deviceId = e.dataTransfer.getData('deviceId');
    if (!deviceId) return;

    fetch(`${SOCKET_URL}/api/devices/${deviceId}/group`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ groupId: groupId || null, storeId: storeId })
    }).then(() => {
      fetchGroups();
      if (fetchDevices) fetchDevices();
    });
  };

  // 마운트 시 기기 목록 강제 갱신
  useEffect(() => {
    if (fetchDevices) fetchDevices();
  }, [fetchDevices]);

  const activeGroups = groups.filter(g => g.storeId === selectedStoreId);
  
  // 필터링 로직 개선: storeId가 없거나, 현재 사업장과 일치하는 기기 중 그룹이 없는 것
  const unassignedStoreDevices = devices.filter(d => {
    const isNoStore = !d.storeId || d.storeId === "" || d.storeId === null;
    const isThisStore = d.storeId === selectedStoreId;
    const isNoGroup = !d.groupId || d.groupId === "" || d.groupId === null;
    return (isNoStore || isThisStore) && isNoGroup;
  });

  const [draggedStoreId, setDraggedStoreId] = useState(null);
  const [dragOverStoreId, setDragOverStoreId] = useState(null);

  const onStoreDragStart = (e, id) => {
    setDraggedStoreId(id);
    e.dataTransfer.setData('storeId', id);
    e.dataTransfer.effectAllowed = 'move';
    // 드래그 시 잔상 효과를 위해 약간의 지연 후 스타일 변경
    setTimeout(() => {
      e.target.style.opacity = '0.4';
    }, 0);
  };

  const onStoreDragEnd = (e) => {
    e.target.style.opacity = '1';
    setDraggedStoreId(null);
    setDragOverStoreId(null);
  };

  const onStoreDragOver = (e, id) => {
    e.preventDefault();
    if (dragOverStoreId !== id) {
      setDragOverStoreId(id);
    }
  };

  const onStoreDrop = (e, targetId) => {
    e.preventDefault();
    const draggedId = e.dataTransfer.getData('storeId') || draggedStoreId;
    if (!draggedId || draggedId === targetId) return;

    const newStores = [...stores];
    const draggedIdx = newStores.findIndex(s => s.id === draggedId);
    const targetIdx = newStores.findIndex(s => s.id === targetId);

    if (draggedIdx === -1 || targetIdx === -1) return;

    const [removed] = newStores.splice(draggedIdx, 1);
    newStores.splice(targetIdx, 0, removed);

    const storeIds = newStores.map(s => s.id);
    
    // 즉시 서버 전송
    fetch(`${SOCKET_URL}/api/stores/reorder`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ storeIds })
    }).then(() => {
      fetchStores();
    }).catch(err => console.error('Reorder error:', err));
    
    setDraggedStoreId(null);
    setDragOverStoreId(null);
  };

  return (
    <div className="group-manager-container" style={{ display: 'flex', gap: '20px', height: 'calc(100vh - 120px)', flexWrap: 'wrap', overflow: 'auto' }}>

      {/* 1. 왼쪽 사이드바: 사업장 목록 */}
      <div className="glass-card" style={{ width: 'clamp(240px, 25vw, 320px)', flexShrink: 0, display: 'flex', flexDirection: 'column', padding: '20px', minHeight: '300px' }}>
        <h2 style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
          <Building size={20} /> 사업장 목록
        </h2>
        
        <div style={{ flex: 1, overflowY: 'auto', marginBottom: '20px' }}>
          {stores.length === 0 && (
            <div style={{ color: '#666', textAlign: 'center', padding: '20px' }}>등록된 사업장이 없습니다.</div>
          )}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {stores.map(s => (
              <div 
                key={s.id} 
                onClick={() => setSelectedStoreId(s.id)}
                draggable
                onDragStart={(e) => onStoreDragStart(e, s.id)}
                onDragOver={(e) => onStoreDragOver(e, s.id)}
                onDragEnd={onStoreDragEnd}
                onDrop={(e) => onStoreDrop(e, s.id)}
                style={{ 
                  padding: '12px 16px', borderRadius: '10px', cursor: 'grab',
                  background: selectedStoreId === s.id ? 'var(--accent-blue)' : 'rgba(255,255,255,0.05)',
                  border: '1px solid',
                  borderColor: dragOverStoreId === s.id ? 'var(--accent-blue)' : (selectedStoreId === s.id ? 'var(--accent-blue)' : 'rgba(255,255,255,0.1)'),
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  transition: 'all 0.2s',
                  transform: dragOverStoreId === s.id ? 'scale(1.02)' : 'scale(1)',
                  boxShadow: dragOverStoreId === s.id ? '0 4px 12px rgba(59,130,246,0.3)' : 'none',
                  zIndex: dragOverStoreId === s.id ? 10 : 1
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', pointerEvents: 'none' }}>
                  <GripVertical size={16} style={{ color: 'rgba(255,255,255,0.3)' }} />
                  <span style={{ fontWeight: selectedStoreId === s.id ? 'bold' : 'normal' }}>{s.name}</span>
                </div>
                <button 
                  onClick={(e) => { e.stopPropagation(); handleDeleteStore(s.id); }}
                  style={{ 
                    background: 'none', border: 'none', color: selectedStoreId === s.id ? '#fff' : '#EF4444', 
                    cursor: 'pointer', padding: '4px', opacity: 0.7,
                    display: 'flex', alignItems: 'center'
                  }}
                  title="삭제"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
          </div>
        </div>

        <div style={{ borderTop: '1px solid var(--glass-border)', paddingTop: '20px' }}>
          <input 
            type="text" 
            placeholder="새 사업장 이름" 
            value={newStoreName}
            onChange={(e) => setNewStoreName(e.target.value)}
            style={{ 
              width: '100%', padding: '10px 12px', borderRadius: '8px', 
              border: '1px solid var(--glass-border)', background: 'rgba(0,0,0,0.3)', 
              color: '#fff', outline: 'none', marginBottom: '8px'
            }}
          />
          <button className="btn btn-primary" style={{ width: '100%' }} onClick={handleCreateStore}>
            <Plus size={18} /> 사업장 추가
          </button>
        </div>
      </div>

      {/* 2. 오른쪽 메인: 선택된 사업장의 상세 관리 */}
      <div className="glass-card" style={{ flex: '1 1 400px', minWidth: 0, overflowY: 'auto', padding: '24px', opacity: selectedStoreId ? 1 : 0.5, pointerEvents: selectedStoreId ? 'auto' : 'none' }}>
        {selectedStoreId ? (
          <>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '24px', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <FolderTree /> [{stores.find(s => s.id === selectedStoreId)?.name}] 구역 및 기기 관리
              </div>
              <div style={{ display: 'flex', gap: '10px' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', background: 'rgba(255,255,255,0.05)', padding: '4px 8px', borderRadius: '4px', display: 'flex', alignItems: 'center' }}>
                  전체 기기: {devices.length}대 | 미배정: {unassignedStoreDevices.length}대
                </span>
                <button 
                  className="btn btn-secondary" 
                  onClick={() => {
                    console.log('Manual refresh requested');
                    if (fetchDevices) fetchDevices();
                  }}
                  style={{ padding: '6px 12px', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '5px' }}
                >
                  🔄 기기 목록 새로고침
                </button>
              </div>
            </h2>
            
            {/* 기기 수동 등록 폼 */}
            <div style={{ background: 'rgba(0,0,0,0.1)', border: '1px solid var(--glass-border)', borderRadius: '12px', padding: '16px', marginBottom: '24px' }}>
              <h3 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <HardDrive size={16} /> 신규 기기 등록
              </h3>
              <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                <input 
                  type="text" 
                  placeholder="ID / MAC" 
                  value={newDeviceId}
                  onChange={(e) => setNewDeviceId(e.target.value)}
                  style={{ flex: 1, minWidth: '150px', padding: '10px 16px', borderRadius: '8px', border: '1px solid var(--glass-border)', background: 'rgba(0,0,0,0.3)', color: '#fff', outline: 'none' }}
                />
                <input 
                  type="text" 
                  placeholder="기기 별칭" 
                  value={newDeviceName}
                  onChange={(e) => setNewDeviceName(e.target.value)}
                  style={{ flex: 1, minWidth: '150px', padding: '10px 16px', borderRadius: '8px', border: '1px solid var(--glass-border)', background: 'rgba(0,0,0,0.3)', color: '#fff', outline: 'none' }}
                />
                <button className="btn btn-primary" onClick={handleRegisterDevice}>기기 등록</button>
              </div>
            </div>

            {/* 구역 추가 폼 */}
            <div style={{ display: 'flex', gap: '8px', marginBottom: '32px' }}>
              <input 
                type="text" 
                placeholder="새로운 구역(그룹) 이름 입력 (예: 로비, 주방)" 
                value={newGroupName}
                onChange={(e) => setNewGroupName(e.target.value)}
                style={{ 
                  flex: 1, padding: '12px 16px', borderRadius: '8px', 
                  border: '1px solid var(--glass-border)', background: 'rgba(0,0,0,0.3)', 
                  color: '#fff', outline: 'none' 
                }}
              />
              <button className="btn btn-primary" onClick={handleCreateGroup}>
                <Plus size={18} /> 구역 추가
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
              {/* 사업장 내 구역 미배정 기기 */}
              <div 
                className="group-dropzone" 
                onDragOver={(e) => e.preventDefault()} 
                onDrop={(e) => onDrop(e, null, selectedStoreId)}
                style={{ 
                  padding: '20px', border: '2px dashed rgba(255,255,255,0.15)', 
                  borderRadius: '12px', minHeight: '100px', background: 'rgba(0,0,0,0.1)'
                }}
              >
                <h3 style={{ marginBottom: '16px', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                  미배정 기기 (여기로 드래그하여 할당 해제)
                </h3>
                <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                  {unassignedStoreDevices.map(d => (
                    <div 
                      key={d.id} 
                      draggable 
                      onDragStart={(e) => onDragStart(e, d.id)}
                      style={{ 
                        background: 'var(--glass-bg)', padding: '8px 14px', 
                        borderRadius: '8px', cursor: 'grab', 
                        border: '1px solid rgba(255,255,255,0.1)',
                        boxShadow: '0 2px 4px rgba(0,0,0,0.2)', fontSize: '0.9rem'
                      }}
                    >
                      <Monitor size={14} style={{ marginRight: '6px', verticalAlign: 'middle', color: d.status==='online' ? '#10B981' : '#EF4444' }}/>
                      {d.name}
                    </div>
                  ))}
                  {unassignedStoreDevices.length === 0 && (
                    <span style={{ color: '#555', fontSize: '0.85rem' }}>구역에 배치되지 않은 기기가 없습니다.</span>
                  )}
                </div>
              </div>

              {/* 선택된 사업장의 구역(그룹)들 */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '20px' }}>
                {activeGroups.map(g => (
                  <div 
                    key={g.id} 
                    onDragOver={(e) => e.preventDefault()} 
                    onDrop={(e) => onDrop(e, g.id, selectedStoreId)}
                    style={{ 
                      padding: '20px', border: '1px solid var(--glass-border)', 
                      borderRadius: '12px', background: 'rgba(255,255,255,0.02)', 
                      minHeight: '160px', transition: 'all 0.2s'
                    }}
                  >
                    <h3 style={{ marginBottom: '16px', color: 'var(--accent-blue)', fontSize: '1rem', borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '8px' }}>
                      {g.name}
                    </h3>
                    <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                      {devices.filter(d => d.groupId === g.id && d.storeId === selectedStoreId).map(d => (
                        <div 
                          key={d.id} 
                          draggable 
                          onDragStart={(e) => onDragStart(e, d.id)}
                          style={{ 
                            background: 'linear-gradient(135deg, rgba(59,130,246,0.1), rgba(139,92,246,0.1))', 
                            padding: '8px 14px', borderRadius: '8px', cursor: 'grab', 
                            border: '1px solid rgba(139,92,246,0.3)',
                            boxShadow: '0 2px 4px rgba(0,0,0,0.2)', fontSize: '0.9rem'
                          }}
                        >
                          <Monitor size={14} style={{ marginRight: '6px', verticalAlign: 'middle', color: d.status==='online' ? '#10B981' : '#EF4444' }}/>
                          {d.name}
                        </div>
                      ))}
                      {devices.filter(d => d.groupId === g.id && d.storeId === selectedStoreId).length === 0 && (
                        <span style={{ color: '#555', fontSize: '0.85rem' }}>할당된 기기 없음</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
              {selectedStoreId && activeGroups.length === 0 && (
                <div style={{ color: '#666', fontStyle: 'italic', padding: '40px', textAlign: 'center' }}>
                  등록된 구역이 없습니다.
                </div>
              )}
            </div>
          </>
        ) : (
          <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#666', fontSize: '1.1rem' }}>
            왼쪽 사이드바에서 사업장을 선택해주세요.
          </div>
        )}
      </div>
    </div>
  );
}
