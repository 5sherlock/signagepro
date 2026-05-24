import React, { useState, useEffect } from 'react';
import { io } from 'socket.io-client';
import { SOCKET_URL } from './config';

export default function Player() {
  const [machineId, setMachineId] = useState('');
  const [statusText, setStatusText] = useState('초기화 중...');
  const [playlist, setPlaylist] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [activeLayer, setActiveLayer] = useState(1);
  const [groupId, setGroupId] = useState(null);
  
  // 1. 고유 ID 생성 (로컬 스토리지에 저장하여 유지)
  useEffect(() => {
    let id = localStorage.getItem('web_player_id');
    if (!id) {
      id = 'WEB-' + Math.random().toString(36).substring(2, 8).toUpperCase();
      localStorage.setItem('web_player_id', id);
    }
    setMachineId(id);
  }, []);

  // 2. 서버 연결 및 동기화
  useEffect(() => {
    if (!machineId) return;

    const socket = io(SOCKET_URL);
    
    socket.on('connect', () => {
      setStatusText('서버 연결 성공! 대시보드 설정을 기다리는 중...');
      
      // 상태 전송 루프 (온라인 유지)
      const heartbeat = setInterval(() => {
        socket.emit('web_player_heartbeat', { deviceId: machineId, name: 'Web-Preview-' + machineId });
      }, 5000);
      
      // 최초 연결 시 즉시 하트비트 전송하여 DB에 생성되도록 함
      socket.emit('web_player_heartbeat', { deviceId: machineId, name: 'Web-Preview-' + machineId });
      setTimeout(fetchDeviceConfig, 1000);

      return () => clearInterval(heartbeat);
    });

    socket.on('group_assignment_changed', () => fetchDeviceConfig());
    
    socket.on('playlist_updated', (data) => {
      if (data.groupId === groupId) fetchPlaylist(data.groupId);
    });

    socket.on('device_deleted', () => {
      setGroupId(null);
      setPlaylist([]);
      setStatusText('이 기기는 관리 시스템에서 삭제되었습니다. 기기를 대시보드에 새로 추가해 주세요.');
    });

    socket.on('disconnect', () => {
      setStatusText('서버와 연결이 끊어졌습니다. 재접속 대기 중...');
      setPlaylist([]);
    });

    return () => socket.disconnect();
  }, [machineId, groupId]);

  const fetchDeviceConfig = async () => {
    try {
      const res = await fetch(`${SOCKET_URL}/api/devices`);
      const devices = await res.json();
      const myDevice = devices.find(d => d.id === machineId);
      
      if (myDevice && myDevice.groupId) {
        setGroupId(myDevice.groupId);
        fetchPlaylist(myDevice.groupId);
      } else {
        setPlaylist([]);
        setStatusText('이 기기는 구역에 배정되지 않았습니다. 대시보드에서 배정해주세요.');
      }
    } catch (e) {
      console.error(e);
      setTimeout(fetchDeviceConfig, 5000);
    }
  };

  const fetchPlaylist = async (gId) => {
    try {
      const res = await fetch(`${SOCKET_URL}/api/groups/${gId}/playlist`);
      const data = await res.json();
      if (data && data.medias) {
        const filtered = data.medias.filter(m => !m.targetDeviceId || m.targetDeviceId === machineId);
        setPlaylist(filtered);
        if (filtered.length > 0) {
          setCurrentIndex(0);
        } else {
          setStatusText('현재 구역에 재생할 미디어가 없습니다.');
        }
      }
    } catch (e) {
      console.error(e);
    }
  };

  // 3. 미디어 무한 반복 루프
  useEffect(() => {
    if (playlist.length === 0) return;
    
    const item = playlist[currentIndex];
    const durationMs = (item.duration || 10) * 1000;
    
    // 레이어 교체 (크로스페이드)
    const nextLayer = activeLayer === 1 ? 2 : 1;
    setActiveLayer(nextLayer);

    const timer = setTimeout(() => {
      setCurrentIndex((prev) => (prev + 1) % playlist.length);
    }, durationMs);

    return () => clearTimeout(timer);
  }, [currentIndex, playlist.length]);

  if (playlist.length === 0) {
    return (
      <div style={{ width: '100vw', height: '100vh', background: '#000', color: '#fff', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', fontFamily: 'sans-serif' }}>
        <h2 style={{ marginBottom: '10px' }}>Signage Web Player</h2>
        <p style={{ color: '#aaa', marginBottom: '30px' }}>{statusText}</p>
        
        {machineId && (
          <div style={{ background: '#222', padding: '20px', borderRadius: '10px', textAlign: 'center' }}>
            <p style={{ margin: 0, fontSize: '0.9rem', color: '#888' }}>기기 고유 ID</p>
            <p style={{ margin: '5px 0 0 0', fontSize: '2rem', color: '#3b82f6', fontWeight: 'bold' }}>{machineId}</p>
          </div>
        )}
      </div>
    );
  }

  const renderMedia = (layerIndex) => {
    const isCurrent = activeLayer === layerIndex;
    const item = playlist[currentIndex];
    
    // 화면에 보여줄 소스 (현재 활성화된 레이어에만 미디어 렌더링)
    // 최적화를 위해 비활성 레이어는 이전 이미지를 유지하다가 지움
    if (!item) return null;
    
    return (
      <div 
        key={`layer-${layerIndex}`}
        style={{
          position: 'absolute', top: 0, left: 0, width: '100%', height: '100%',
          opacity: isCurrent ? 1 : 0,
          transition: 'opacity 1s ease-in-out',
          zIndex: isCurrent ? 2 : 1
        }}
      >
        {isCurrent ? (
          item.media.type === 'video' ? (
            <video src={`${SOCKET_URL}${item.media.path}`} autoPlay muted loop style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          ) : (
            <img src={`${SOCKET_URL}${item.media.path}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} alt="" />
          )
        ) : null}
      </div>
    );
  };

  return (
    <div style={{ width: '100vw', height: '100vh', background: '#000', position: 'fixed', top: 0, left: 0, overflow: 'hidden' }}>
      {renderMedia(1)}
      {renderMedia(2)}
    </div>
  );
}
