import { useState } from 'react';
import { Activity } from 'lucide-react';
import { SOCKET_URL } from '../config';

export default function LoginScreen({ onLogin }) {
  const [mode, setMode] = useState('login'); // 'login' | 'change'
  const [pw, setPw] = useState('');
  const [currentPw, setCurrentPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [confirmPw, setConfirmPw] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const submitLogin = async (e) => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      const r = await fetch(`${SOCKET_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: pw }),
      });
      const d = await r.json();
      if (!r.ok) {
        const msg = typeof d.remaining === 'number'
          ? `${d.error || '로그인 실패'} (남은 시도: ${d.remaining}회)`
          : d.error || '로그인 실패';
        setError(msg); setPw(''); return;
      }
      localStorage.setItem('SIGNAGE_TOKEN', d.token);
      onLogin();
    } catch { setError('서버에 연결할 수 없습니다.'); }
    finally { setLoading(false); }
  };

  const submitChange = async (e) => {
    e.preventDefault();
    setError(''); setSuccess('');
    if (newPw !== confirmPw) { setError('새 비밀번호가 일치하지 않습니다.'); return; }
    if (newPw.length < 4) { setError('새 비밀번호는 4자 이상이어야 합니다.'); return; }
    setLoading(true);
    try {
      const r = await fetch(`${SOCKET_URL}/api/auth/change-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ current: currentPw, newPassword: newPw }),
      });
      const d = await r.json();
      if (!r.ok) { setError(d.error || '변경 실패'); return; }
      setSuccess('비밀번호가 변경되었습니다. 새 비밀번호로 로그인하세요.');
      setCurrentPw(''); setNewPw(''); setConfirmPw('');
      setTimeout(() => { setMode('login'); setSuccess(''); }, 2000);
    } catch { setError('서버에 연결할 수 없습니다.'); }
    finally { setLoading(false); }
  };

  const inputStyle = { padding:'10px 12px', borderRadius:'8px', border:'1px solid var(--border)', background:'var(--bg-primary)', color:'var(--text-primary)', fontSize:'1rem', outline:'none' };

  return (
    <div style={{ display:'flex', alignItems:'center', justifyContent:'center', height:'100vh', background:'var(--bg-primary)' }}>
      <form onSubmit={mode === 'login' ? submitLogin : submitChange}
        style={{ background:'var(--bg-secondary)', border:'1px solid var(--border)', borderRadius:'12px', padding:'40px', width:'320px', display:'flex', flexDirection:'column', gap:'16px' }}>
        <div style={{ display:'flex', alignItems:'center', gap:'10px', marginBottom:'4px' }}>
          <Activity size={28} color="#3B82F6" />
          <span style={{ fontSize:'1.3rem', fontWeight:700, color:'var(--text-primary)' }}>SignagePro</span>
        </div>

        {mode === 'login' ? (
          <>
            <p style={{ color:'var(--text-secondary)', fontSize:'0.85rem', margin:0 }}>관리자 비밀번호를 입력하세요.</p>
            <input type="password" value={pw} onChange={e => setPw(e.target.value)}
              placeholder="비밀번호" autoFocus style={inputStyle} />
            {error && <p style={{ color:'#EF4444', fontSize:'0.8rem', margin:0 }}>{error}</p>}
            <button type="submit" disabled={loading}
              style={{ padding:'10px', borderRadius:'8px', background:'#3B82F6', color:'#fff', border:'none', fontWeight:600, fontSize:'1rem', cursor:'pointer' }}>
              {loading ? '확인 중…' : '로그인'}
            </button>
            <button type="button" onClick={() => { setMode('change'); setError(''); }}
              style={{ padding:'6px', background:'transparent', border:'none', color:'var(--text-secondary)', fontSize:'0.8rem', cursor:'pointer', textDecoration:'underline' }}>
              비밀번호 변경
            </button>
          </>
        ) : (
          <>
            <p style={{ color:'var(--text-secondary)', fontSize:'0.85rem', margin:0 }}>비밀번호를 변경합니다.</p>
            <input type="password" value={currentPw} onChange={e => setCurrentPw(e.target.value)}
              placeholder="현재 비밀번호" autoFocus style={inputStyle} />
            <input type="password" value={newPw} onChange={e => setNewPw(e.target.value)}
              placeholder="새 비밀번호" style={inputStyle} />
            <input type="password" value={confirmPw} onChange={e => setConfirmPw(e.target.value)}
              placeholder="새 비밀번호 확인" style={inputStyle} />
            {error && <p style={{ color:'#EF4444', fontSize:'0.8rem', margin:0 }}>{error}</p>}
            {success && <p style={{ color:'#10B981', fontSize:'0.8rem', margin:0 }}>{success}</p>}
            <button type="submit" disabled={loading}
              style={{ padding:'10px', borderRadius:'8px', background:'#10B981', color:'#fff', border:'none', fontWeight:600, fontSize:'1rem', cursor:'pointer' }}>
              {loading ? '변경 중…' : '비밀번호 변경'}
            </button>
            <button type="button" onClick={() => { setMode('login'); setError(''); }}
              style={{ padding:'6px', background:'transparent', border:'none', color:'var(--text-secondary)', fontSize:'0.8rem', cursor:'pointer', textDecoration:'underline' }}>
              로그인으로 돌아가기
            </button>
          </>
        )}
      </form>
    </div>
  );
}
