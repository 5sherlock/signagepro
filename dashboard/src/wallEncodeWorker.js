// 비디오월 슬라이스 인코딩 워커 — 탭 가시성과 무관하게(백그라운드에서도) 동작.
// <video>/requestVideoFrameCallback(백그라운드서 멈춤) 대신 mp4box 디먹스 + WebCodecs
// VideoDecoder 로 최대속도 디코드 → 슬라이스별 크롭/스케일 → VideoEncoder(H.264) → mp4-muxer.
import { Muxer, ArrayBufferTarget } from 'mp4-muxer';
import { createFile as createMp4File, DataStream } from 'mp4box';

function sliceSrcRects({ srcW, srcH, deviceCount, sliceWidth, sliceHeight, yOffsetPct }) {
  const cw = sliceWidth * deviceCount, ch = sliceHeight;
  const factor = Math.max(cw / srcW, ch / srcH);
  const cropW = cw / factor, cropH = ch / factor;
  const y = Math.max(0, Math.min(100, yOffsetPct)) / 100;
  const xSrc = (srcW - cropW) / 2, ySrc = (srcH - cropH) * y;
  const per = cropW / deviceCount;
  return Array.from({ length: deviceCount }, (_, i) => ({ sx: xSrc + i * per, sy: ySrc, sw: per, sh: cropH }));
}

async function pickEncoderCfg(width, height, bitrate, framerate) {
  const cands = ['avc1.640033', 'avc1.640032', 'avc1.4D0033', 'avc1.42E033', 'avc1.42001F'];
  for (const codec of cands) {
    try {
      const cfg = { codec, width, height, bitrate, framerate, hardwareAcceleration: 'prefer-hardware' };
      const r = await VideoEncoder.isConfigSupported(cfg);
      if (r && r.supported) return cfg;
    } catch (e) { /* 다음 */ }
  }
  return { codec: 'avc1.4D0033', width, height, bitrate, framerate };
}

// avcC/hvcC 박스 → VideoDecoder description(박스 헤더 8바이트 제외)
function descFromBox(box) {
  const ds = new DataStream(undefined, 0, DataStream.BIG_ENDIAN);
  box.write(ds);
  return new Uint8Array(ds.buffer, 8);
}

// mp4 바이트 → 비디오/오디오 인코딩 청크 + 디코더 설정
function demux(bytes) {
  return new Promise((resolve, reject) => {
    const file = createMp4File();
    const r = { video: null, vchunks: [], audio: null };
    file.onError = (e) => reject(new Error('디먹스 실패: ' + e));
    file.onReady = (info) => {
      const vt = info.videoTracks && info.videoTracks[0];
      if (!vt) return reject(new Error('영상 트랙이 없습니다.'));
      const trak = file.getTrackById(vt.id);
      let desc;
      for (const entry of trak.mdia.minf.stbl.stsd.entries) {
        if (entry.avcC) { desc = descFromBox(entry.avcC); break; }
        if (entry.hvcC) { desc = descFromBox(entry.hvcC); break; }
      }
      r.video = {
        codec: vt.codec,
        codedWidth: (vt.video && vt.video.width) || vt.track_width,
        codedHeight: (vt.video && vt.video.height) || vt.track_height,
        description: desc,
      };
      const at = info.audioTracks && info.audioTracks[0];
      if (at) {
        r.audio = { config: { codec: 'aac', sampleRate: at.audio.sample_rate, numberOfChannels: at.audio.channel_count }, description: null, chunks: [] };
        try {
          const ae = file.getTrackById(at.id).mdia.minf.stbl.stsd.entries[0];
          const asc = ae.esds && ae.esds.esd && ae.esds.esd.descs[0] && ae.esds.esd.descs[0].descs[0] && ae.esds.esd.descs[0].descs[0].data;
          if (asc) r.audio.description = asc;
        } catch (e) { /* 기본 ASC */ }
        file.setExtractionOptions(at.id, 'audio', { nbSamples: 1000000 });
      }
      file.setExtractionOptions(vt.id, 'video', { nbSamples: 1000000 });
      file.onSamples = (id, user, samples) => {
        for (const s of samples) {
          const c = {
            type: s.is_sync ? 'key' : 'delta',
            timestamp: Math.round((s.cts / s.timescale) * 1e6),
            duration: Math.round((s.duration / s.timescale) * 1e6),
            data: s.data,
          };
          if (user === 'video') r.vchunks.push(c); else if (r.audio) r.audio.chunks.push(c);
        }
      };
      file.start();
      file.flush();
      setTimeout(() => resolve(r), 0);
    };
    bytes.fileStart = 0;
    file.appendBuffer(bytes);
    file.flush();
  });
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

self.onmessage = async (ev) => {
  const { srcBytes, deviceCount, sliceWidth, sliceHeight, yOffsetPct = 50, fps = 30 } = ev.data;
  try {
    const dem = await demux(srcBytes);
    const srcW = dem.video.codedWidth, srcH = dem.video.codedHeight;
    if (!srcW || !srcH) throw new Error('원본 해상도를 읽을 수 없습니다.');
    const rects = sliceSrcRects({ srcW, srcH, deviceCount, sliceWidth, sliceHeight, yOffsetPct });
    const bitrate = Math.round(sliceWidth * sliceHeight * fps * 0.07);
    const keyEvery = Math.max(1, Math.round(fps * 2));

    // ⚠ wall-sync 안정성: 모든 슬라이스의 컨테이너 길이가 동일해야 한다.
    // 플레이어는 tgt=(NTP wallclock)%dur 로 위치를 잡는데, wallclock이 거대해서 dur이 수십 ms만
    // 달라도 결과가 수십 초씩 어긋난다. 오디오가 비디오보다 길면 슬라이스0만 길어지므로 영상 길이로 트림.
    if (dem.audio && dem.audio.chunks.length && dem.vchunks.length) {
      let vMin = Infinity, vMax = 0;
      for (const c of dem.vchunks) { if (c.timestamp < vMin) vMin = c.timestamp; const end = c.timestamp + (c.duration || 0); if (end > vMax) vMax = end; }
      const videoDurUs = vMax - vMin;
      const aStart = dem.audio.chunks[0].timestamp;
      dem.audio.chunks = dem.audio.chunks.filter((c) => (c.timestamp + (c.duration || 0) - aStart) <= videoDurUs);
    }

    const slices = [];
    for (let i = 0; i < deviceCount; i++) {
      const canvas = new OffscreenCanvas(sliceWidth, sliceHeight);
      const ctx = canvas.getContext('2d', { alpha: false });
      const muxer = new Muxer({
        target: new ArrayBufferTarget(),
        video: { codec: 'avc', width: sliceWidth, height: sliceHeight, frameRate: fps },
        // 모든 슬라이스에 동일 오디오 → ① 어느 기기든 오디오 출력 선택 가능 ② 컨테이너 길이가
        // 4개 모두 같아져 wall-sync((wallclock)%dur)가 정확히 맞음(슬라이스0만 오디오면 길이 달라져 깨짐).
        ...(dem.audio ? { audio: dem.audio.config } : {}),
        // 원본이 0이 아닌 타임스탬프에서 시작해도(에딧리스트/오디오 프라이밍) 0부터 쓰게 보정
        firstTimestampBehavior: 'offset',
        fastStart: 'in-memory',
      });
      const slot = { canvas, ctx, muxer, enc: null, err: null };
      const cfg = await pickEncoderCfg(sliceWidth, sliceHeight, bitrate, fps);
      slot.enc = new VideoEncoder({ output: (c, m) => muxer.addVideoChunk(c, m), error: (e) => { slot.err = e; } });
      slot.enc.configure(cfg);
      slices.push(slot);
    }
    const maxEncQ = () => slices.reduce((m, s) => Math.max(m, s.enc.encodeQueueSize), 0);
    const firstErr = () => { const s = slices.find((x) => x.err); return s ? s.err : null; };

    let decErr = null, outCount = 0;
    const total = dem.vchunks.length || 1;
    const decoder = new VideoDecoder({
      output: (frame) => {
        if (firstErr()) { frame.close(); return; }
        const keyFrame = (outCount % keyEvery) === 0;
        for (let i = 0; i < slices.length; i++) {
          const s = slices[i], rc = rects[i];
          s.ctx.drawImage(frame, rc.sx, rc.sy, rc.sw, rc.sh, 0, 0, sliceWidth, sliceHeight);
          const vf = new VideoFrame(s.canvas, { timestamp: frame.timestamp });
          try { s.enc.encode(vf, { keyFrame }); } finally { vf.close(); }
        }
        frame.close();
        outCount++;
        if ((outCount & 7) === 0) self.postMessage({ type: 'progress', p: Math.min(0.99, outCount / total) });
      },
      error: (e) => { decErr = e; },
    });
    decoder.configure({ codec: dem.video.codec, codedWidth: srcW, codedHeight: srcH, description: dem.video.description });

    for (const chunk of dem.vchunks) {
      if (decErr) throw decErr;
      if (firstErr()) throw firstErr();
      while (decoder.decodeQueueSize > 8 || maxEncQ() > 8) { await sleep(4); if (decErr) throw decErr; if (firstErr()) throw firstErr(); }
      decoder.decode(new EncodedVideoChunk(chunk));
    }
    await decoder.flush();
    if (decErr) throw decErr;

    for (let i = 0; i < slices.length; i++) {
      await slices[i].enc.flush();
      if (slices[i].err) throw slices[i].err;
      if (dem.audio) {  // 모든 슬라이스에 동일 오디오 트랙(출력 기기 선택 가능 + 길이 통일)
        const meta = { decoderConfig: { codec: 'mp4a.40.2', sampleRate: dem.audio.config.sampleRate, numberOfChannels: dem.audio.config.numberOfChannels, ...(dem.audio.description ? { description: dem.audio.description } : {}) } };
        for (const c of dem.audio.chunks) slices[i].muxer.addAudioChunk(new EncodedAudioChunk(c), meta);
      }
      slices[i].muxer.finalize();
    }
    self.postMessage({ type: 'progress', p: 1 });
    const out = slices.map((s, i) => ({ index: i, buffer: s.muxer.target.buffer }));
    self.postMessage({ type: 'done', slices: out }, out.map((o) => o.buffer));
  } catch (err) {
    self.postMessage({ type: 'error', error: (err && err.message) || String(err) });
  }
};
