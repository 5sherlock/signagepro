// 비디오월 슬라이스를 브라우저(PC GPU)에서 인코딩 — 실제 작업은 Web Worker(wallEncodeWorker.js)에서.
// 워커는 탭 가시성에 영향받지 않아 다른 탭으로 전환/최소화해도 계속 가공된다.
// (WebCodecs 자체는 보안 컨텍스트(HTTPS 또는 localhost) 필요)

export function webcodecsSupported() {
  if (typeof window === 'undefined') return false;
  const secure = (typeof window.isSecureContext === 'undefined') || window.isSecureContext;
  return secure
    && 'VideoEncoder' in window && 'VideoDecoder' in window && 'VideoFrame' in window && 'EncodedVideoChunk' in window
    && typeof Worker !== 'undefined' && typeof OffscreenCanvas !== 'undefined';
}

/**
 * @param {Object} o
 * @param {Blob|string} o.src 원본(디스크 파일 Blob 또는 URL)
 * @param {number} o.deviceCount 슬라이스 수(가로 대수)
 * @param {number} o.sliceWidth 슬라이스 가로(px)
 * @param {number} o.sliceHeight 슬라이스 세로(px)
 * @param {number} [o.yOffsetPct=50] 세로 크롭 위치(%)
 * @param {number} [o.fps=30] 인코더 프레임레이트 힌트
 * @param {(p:number)=>void} [o.onProgress] 0~1
 * @param {AbortSignal} [o.signal]
 * @returns {Promise<Array<{index:number, blob:Blob}>>}
 */
export async function encodeWall({ src, deviceCount, sliceWidth, sliceHeight, yOffsetPct = 50, fps = 30, onProgress, signal }) {
  if (!webcodecsSupported()) {
    const insecure = typeof window !== 'undefined' && !window.isSecureContext;
    throw new Error(insecure
      ? 'WebCodecs는 보안 컨텍스트(HTTPS 또는 localhost)에서만 됩니다. localhost나 HTTPS로 접속하세요.'
      : '이 브라우저는 WebCodecs를 지원하지 않습니다 (Chrome/Edge 최신 권장).');
  }
  // 원본 바이트 확보(파일이면 메모리, URL이면 fetch) → 워커로 zero-copy 전송
  const srcBytes = src instanceof Blob ? await src.arrayBuffer() : await (await fetch(src)).arrayBuffer();

  return await new Promise((resolve, reject) => {
    const worker = new Worker(new URL('./wallEncodeWorker.js', import.meta.url), { type: 'module' });
    let done = false;
    const cleanup = () => { try { worker.terminate(); } catch (e) {} };
    const finish = (fn, arg) => { if (done) return; done = true; cleanup(); fn(arg); };

    if (signal) {
      if (signal.aborted) { cleanup(); return reject(new Error('취소됨')); }
      signal.addEventListener('abort', () => finish(reject, new Error('취소됨')), { once: true });
    }
    worker.onmessage = (e) => {
      const d = e.data;
      if (d.type === 'progress') { if (onProgress) onProgress(d.p); }
      else if (d.type === 'done') finish(resolve, d.slices.map((s) => ({ index: s.index, blob: new Blob([s.buffer], { type: 'video/mp4' }) })));
      else if (d.type === 'error') finish(reject, new Error(d.error || '가공 실패'));
    };
    worker.onerror = (e) => finish(reject, new Error('워커 오류: ' + (e.message || 'unknown')));
    worker.postMessage({ srcBytes, deviceCount, sliceWidth, sliceHeight, yOffsetPct, fps }, [srcBytes]);
  });
}
