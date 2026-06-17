// 멀티스크린 연속 자막(티커) 위치 계산 — 단일 출처(Single Source of Truth).
// 기기(Android TickerView.onDraw)와 "동일한 공식"을 full-scale(1920px) 좌표로 구현한다.
//   가상 캔버스 폭 = SCREEN_W × totalDevices
//   posInCycle(px) = (nowMs % cycleMs) / 1000 × speed
//   virtualX       = totalW - posInCycle          (rtl 기준; ltr은 -textW + posInCycle)
//   localX(기기i)  = virtualX - deviceIndex × SCREEN_W
//
// 핵심: nowMs를 모든 렌더러(기기 NTP시계 / 대시보드 서버정렬시계)가 같은 기준으로 넣고,
// cycleMs를 그룹 공통값으로 쓰면 N개 화면이 한 타임라인을 공유 → 이음새가 정의상 연속된다.
// (대시보드 preview는 결과 localX에 scale=tileW/SCREEN_W만 곱해 축소 렌더한다.)

export const SCREEN_W = 1920;
export const SCREEN_H = 1080;

// 사이클 길이(ms). 기기가 보고한 cycleMs(그룹 공통)가 있으면 그걸 쓰고,
// 없으면 textW로 추정한다. textW는 full-scale(1920 기준) 폰트 측정 폭(px).
export function tickerCycleMs({ cycleMs, speed, totalDevices, textW = 0 }) {
  if (cycleMs && cycleMs > 0) return cycleMs;
  const N = Math.max(1, totalDevices || 1);
  const cyclePx = SCREEN_W * N + (textW || 0);
  return (cyclePx / Math.max(1, speed || 1)) * 1000;
}

// 특정 기기(deviceIndex)의 full-scale localX(px) 계산.
//
// 비율(fraction) 기반: 사이클 진행도 f=(now%cycleMs)/cycleMs ∈ [0,1)를 자체 strip
// 길이(N×SCREEN_W + textW)에 매핑한다. 기기가 보고한 cycleMs를 그대로 쓰므로
// preview의 한 바퀴 시간 = 기기의 한 바퀴 시간 → "기기 실제 해상도가 1920이 아니어도"
// 시각 속도가 일치한다. (기기 TickerView도 posInCycle = f × cyclePx 로 동작.)
// speed는 cycleMs 미보고 시 fallback 추정에만 쓰인다.
export function tickerLocalX({
  nowMs, cycleMs, speed, totalDevices, deviceIndex,
  direction = 'rtl', textW = 0,
}) {
  const N = Math.max(1, totalDevices || 1);
  const idx = Math.max(0, deviceIndex || 0);
  const totalW = SCREEN_W * N;
  const strip = totalW + (textW || 0);
  const effMs = tickerCycleMs({ cycleMs, speed, totalDevices: N, textW });
  // 음수 modulo 방어(((a % m) + m) % m)
  const f = (((nowMs % effMs) + effMs) % effMs) / effMs;
  const posInCycle = f * strip;
  const virtualX = direction === 'ltr' ? (-(textW || 0) + posInCycle) : (totalW - posInCycle);
  return virtualX - idx * SCREEN_W;
}

// 자막 바의 세로 위치/높이 (full-scale px). barH = fontSize × 2 (기기와 동일).
export function tickerBar({ fontSize, position }) {
  const barH = (fontSize || 48) * 2;
  const top = position === 'top' ? 0
            : position === 'center' ? (SCREEN_H - barH) / 2
            : SCREEN_H - barH; // bottom
  return { top, height: barH };
}
