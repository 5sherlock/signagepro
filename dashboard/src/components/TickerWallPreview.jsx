import { useEffect, useRef, useState } from 'react';
import { tickerLocalX, SCREEN_W, SCREEN_H } from '../lib/tickerLayout';

// 그룹의 N개 화면을 이음새 없이 가로로 붙여 렌더하는 비디오월 동기 자막 미리보기.
// 물리 모니터 4대 없이도 한 화면에서 자막이 화면 경계를 넘어 연속으로 흐르는지 검증할 수 있다.
// 모든 타일이 같은 시각(now)·같은 cycleMs·같은 textW를 쓰고 각자 deviceIndex만 다르므로
// (tickerLocalX 내부에서 idx×SCREEN_W를 뺌) 이음새는 정의상 연속된다.

let _canvas;
function measureTextFull(text, fontSize, bold, family) {
  if (!_canvas) _canvas = document.createElement('canvas');
  const ctx = _canvas.getContext('2d');
  ctx.font = `${bold ? '700' : '400'} ${fontSize}px '${family || 'sans-serif'}', sans-serif`;
  return ctx.measureText(text || '').width;
}

function hexToRgba(hex, alpha) {
  const h = (hex || '#000000').replace('#', '');
  const r = parseInt(h.slice(0, 2), 16) || 0;
  const g = parseInt(h.slice(2, 4), 16) || 0;
  const b = parseInt(h.slice(4, 6), 16) || 0;
  return `rgba(${r},${g},${b},${alpha})`;
}

export default function TickerWallPreview({ ticker, deviceLabels = [], cycleMs = 0 }) {
  const N = Math.max(1, deviceLabels.length || 1);
  const wrapRef = useRef(null);
  const [now, setNow] = useState(() => Date.now());
  const [wrapW, setWrapW] = useState(0);

  useEffect(() => {
    let raf;
    const loop = () => { setNow(Date.now()); raf = requestAnimationFrame(loop); };
    raf = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(raf);
  }, []);

  useEffect(() => {
    const el = wrapRef.current;
    if (!el) return;
    const update = () => setWrapW(el.clientWidth);
    update();
    const ro = new ResizeObserver(update);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const tileW = wrapW > 0 ? wrapW / N : 0;
  const scale = tileW / SCREEN_W;
  const tileH = tileW * SCREEN_H / SCREEN_W;

  const fontSize = ticker.fontSize || 48;
  const textW = measureTextFull(ticker.text || '', fontSize, ticker.fontBold, ticker.fontFamily);

  const barH = fontSize * 2 * scale;
  const barTop = ticker.position === 'top' ? 0
    : ticker.position === 'center' ? (tileH - barH) / 2
    : tileH - barH;
  const bg = hexToRgba(ticker.bgColor || '#000000', (ticker.bgOpacity ?? 65) / 100);

  return (
    <div ref={wrapRef} style={{ display: 'flex', width: '100%', borderRadius: 8, overflow: 'hidden', background: '#000', border: '1px solid #334155' }}>
      {Array.from({ length: N }).map((_, idx) => {
        const localX = tileW > 0 ? tickerLocalX({
          nowMs: now, cycleMs, speed: ticker.speed || 50,
          totalDevices: N, deviceIndex: idx, direction: ticker.direction || 'rtl', textW,
        }) * scale : 0;
        return (
          <div key={idx} style={{
            position: 'relative', width: tileW || `${100 / N}%`, height: tileH || 80,
            flex: '0 0 auto', overflow: 'hidden',
            borderRight: idx < N - 1 ? '1px dashed rgba(96,165,250,0.5)' : 'none',
          }}>
            <div style={{ position: 'absolute', top: 3, left: 5, fontSize: 10, color: '#60a5fa', zIndex: 2, fontWeight: 700, pointerEvents: 'none' }}>
              {deviceLabels[idx] || `#${idx}`}
            </div>
            {ticker.text ? (
              <div style={{ position: 'absolute', left: 0, right: 0, top: barTop, height: barH, background: bg, overflow: 'hidden' }}>
                <span style={{
                  position: 'absolute', whiteSpace: 'nowrap', left: 0, top: '50%',
                  transform: `translate(${localX}px, -50%)`,
                  color: ticker.textColor || '#ffffff',
                  fontWeight: ticker.fontBold ? 700 : 400,
                  fontStyle: ticker.fontItalic ? 'italic' : 'normal',
                  fontSize: fontSize * scale,
                  fontFamily: `'${ticker.fontFamily || 'sans-serif'}', sans-serif`,
                  lineHeight: 1,
                }}>
                  {ticker.text}
                </span>
              </div>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}
