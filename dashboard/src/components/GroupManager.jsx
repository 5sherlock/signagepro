import { useState, useEffect } from 'react';
import { Monitor, Plus, FolderTree, Building, HardDrive } from 'lucide-react';

const SOCKET_URL = 'http://localhost:3000';

export default function GroupManager({ devices, fetchDevices, stores, fetchStores, groups, fetchGroups }) {
  const [newStoreName, setNewStoreName] = useState('');
  const [newGroupName, setNewGroupName] = useState('');
  const [newDeviceId, setNewDeviceId] = useState('');
  const [newDeviceName, setNewDeviceName] = useState('');
  const [selectedStoreId, setSelectedStoreId] = useState('');

  // Initial select
  useEffect(() => {
    if (stores.length > 0 && !selectedStoreId) {
      setSelectedStoreId(stores[0].id);
    }
  }, [stores, selectedStoreId]);

  const handleCreateStore = () => {
    if (!newStoreName.trim()) return;
    fetch(`${SOCKET_URL}/api/stores`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: newStoreName })
    }).then(() => {
      setNewStoreName('');
      fetchStores();
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

  const activeGroups = groups.filter(g => g.storeId === selectedStoreId);
  const unassignedStoreDevices = devices.filter(d => d.storeId === selectedStoreId && !d.groupId);

  return (
    <div className="group-manager-layout" style={{ display: 'flex', gap: '24px', height: '100%', flexDirection: 'column' }}>
      
      {/* 1. 상단: 사업장 관리 */}
      <div className="glass-card" style={{ flex: 'none', padding: '24px' }}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
          <Building /> 사업장(Store) 선택 및 관리
        </h2>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
          <select 
            value={selectedStoreId} 
            onChange={(e) => setSelectedStoreId(e.target.value)}
            className="glass-select"
            style={{ minWidth: '200px' }}
          >
            {stores.length === 0 && <option value="">등록된 사업장이 없습니다</option>}
            {stores.map(s => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>

          <div style={{ width: '1px', height: '24px', background: 'var(--glass-border)', margin: '0 8px' }}></div>

          <input 
            type="text" 
            placeholder="새 사업장 이름 입력" 
            value={newStoreName}
            onChange={(e) => setNewStoreName(e.target.value)}
            style={{ 
              width: '240px', padding: '10px 16px', borderRadius: '8px', 
              border: '1px solid var(--glass-border)', background: 'rgba(0,0,0,0.3)', 
              color: '#fff', outline: 'none' 
            }}
          />
          <button className="btn btn-primary" onClick={handleCreateStore}>
            <Plus size={18} /> 사업장 추가
          </button>
        </div>
      </div>

      {/* 2. 하단: 기기 등록 및 구역 관리 (선택된 사업장 종속) */}
      <div className="glass-card" style={{ flex: 1, overflowY: 'auto', opacity: selectedStoreId ? 1 : 0.5, pointerEvents: selectedStoreId ? 'auto' : 'none' }}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
          <FolderTree /> [{stores.find(s => s.id === selectedStoreId)?.name || '사업장 선택 안됨'}] 구역 및 기기 관리
        </h2>
        
        {/* 기기 수동 등록 폼 */}
        <div style={{ background: 'rgba(0,0,0,0.1)', border: '1px solid var(--glass-border)', borderRadius: '12px', padding: '16px', marginBottom: '24px' }}>
          <h3 style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <HardDrive size={16} /> 신규 기기(셋탑박스) 물리적 주소로 등록
          </h3>
          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            <input 
              type="text" 
              placeholder="물리적 주소 / MAC (예: dev-101)" 
              value={newDeviceId}
              onChange={(e) => setNewDeviceId(e.target.value)}
              style={{ flex: 1, minWidth: '200px', padding: '10px 16px', borderRadius: '8px', border: '1px solid var(--glass-border)', background: 'rgba(0,0,0,0.3)', color: '#fff', outline: 'none' }}
            />
            <input 
              type="text" 
              placeholder="기기 별칭 (예: 1번 모니터)" 
              value={newDeviceName}
              onChange={(e) => setNewDeviceName(e.target.value)}
              style={{ flex: 1, minWidth: '200px', padding: '10px 16px', borderRadius: '8px', border: '1px solid var(--glass-border)', background: 'rgba(0,0,0,0.3)', color: '#fff', outline: 'none' }}
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
            <Plus size={18} /> 구역(그룹) 추가
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* 사업장 내 구역 미배정 기기 */}
          <div 
            className="group-dropzone" 
            onDragOver={(e) => e.preventDefault()} 
            onDrop={(e) => onDrop(e, null, selectedStoreId)}
            style={{ 
              padding: '24px', border: '2px dashed rgba(255,255,255,0.2)', 
              borderRadius: '12px', minHeight: '120px', background: 'rgba(0,0,0,0.1)'
            }}
          >
            <h3 style={{ marginBottom: '16px', color: 'var(--text-secondary)', fontSize: '1rem' }}>
              이 사업장의 구역 미배정 기기 (대기열 - 여기로 드래그하여 구역 할당 해제)
            </h3>
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
              {unassignedStoreDevices.map(d => (
                <div 
                  key={d.id} 
                  draggable 
                  onDragStart={(e) => onDragStart(e, d.id)}
                  style={{ 
                    background: 'var(--glass-bg)', padding: '10px 16px', 
                    borderRadius: '8px', cursor: 'grab', 
                    border: '1px solid rgba(255,255,255,0.1)',
                    boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
                  }}
                >
                  <Monitor size={14} style={{ marginRight: '8px', verticalAlign: 'middle', color: d.status==='online' ? '#10B981' : '#EF4444' }}/>
                  {d.name} <span style={{ fontSize: '0.8rem', color: '#888', marginLeft: '6px' }}>({d.id})</span>
                </div>
              ))}
              {unassignedStoreDevices.length === 0 && (
                <span style={{ color: '#666', fontSize: '0.9rem' }}>구역에 배치되지 않은 등록 기기가 없습니다.</span>
              )}
            </div>
          </div>

          {/* 선택된 사업장의 구역(그룹)들 */}
          {activeGroups.map(g => (
            <div 
              key={g.id} 
              onDragOver={(e) => e.preventDefault()} 
              onDrop={(e) => onDrop(e, g.id, selectedStoreId)}
              style={{ 
                padding: '24px', border: '1px solid var(--glass-border)', 
                borderRadius: '12px', background: 'rgba(255,255,255,0.03)', 
                minHeight: '140px', transition: 'all 0.2s'
              }}
            >
              <h3 style={{ marginBottom: '16px', color: 'var(--accent-blue)', fontSize: '1.1rem' }}>
                {g.name}
              </h3>
              <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                {devices.filter(d => d.groupId === g.id && d.storeId === selectedStoreId).map(d => (
                  <div 
                    key={d.id} 
                    draggable 
                    onDragStart={(e) => onDragStart(e, d.id)}
                    style={{ 
                      background: 'linear-gradient(135deg, rgba(59,130,246,0.1), rgba(139,92,246,0.1))', 
                      padding: '10px 16px', borderRadius: '8px', cursor: 'grab', 
                      border: '1px solid rgba(139,92,246,0.3)',
                      boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
                    }}
                  >
                    <Monitor size={14} style={{ marginRight: '8px', verticalAlign: 'middle', color: d.status==='online' ? '#10B981' : '#EF4444' }}/>
                    {d.name} <span style={{ fontSize: '0.8rem', color: '#888', marginLeft: '6px' }}>({d.id})</span>
                  </div>
                ))}
                {devices.filter(d => d.groupId === g.id && d.storeId === selectedStoreId).length === 0 && (
                  <span style={{ color: '#666', fontSize: '0.9rem' }}>이 구역에 할당된 기기가 없습니다. 위에서 기기를 끌어다 놓으세요.</span>
                )}
              </div>
            </div>
          ))}
          {selectedStoreId && activeGroups.length === 0 && (
            <div style={{ color: '#666', fontStyle: 'italic', padding: '20px', textAlign: 'center' }}>
              이 사업장에는 아직 구성된 구역이 없습니다. 위에서 구역을 추가해주세요.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
