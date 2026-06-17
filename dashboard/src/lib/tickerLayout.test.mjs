// 자막 멀티스크린 위치 공식 단위테스트 — 4대 물리 모니터 없이 이음새 연속성을 수치로 검증.
// 실행: node src/lib/tickerLayout.test.mjs
import { tickerLocalX, tickerCycleMs, SCREEN_W } from './tickerLayout.js';

let pass = 0, fail = 0;
const approx = (a, b, eps = 1e-6) => Math.abs(a - b) <= eps;
function check(name, cond) {
  if (cond) { pass++; console.log('PASS', name); }
  else { fail++; console.log('FAIL', name); }
}

const base = { cycleMs: 37000, speed: 50, totalDevices: 4, direction: 'rtl', textW: 600 };

// 1) 이음새 연속성: localX(i+1) === localX(i) - SCREEN_W (모든 시각·모든 i에서)
//    → 기기 i+1은 기기 i보다 정확히 한 화면 왼쪽 → 텍스트가 끊김 없이 이어진다.
{
  let ok = true;
  for (const nowMs of [0, 1234, 9999, 18500, 36999, 123456789, 987654321]) {
    for (let i = 0; i < base.totalDevices - 1; i++) {
      const a = tickerLocalX({ ...base, nowMs, deviceIndex: i });
      const b = tickerLocalX({ ...base, nowMs, deviceIndex: i + 1 });
      if (!approx(b, a - SCREEN_W)) { ok = false; }
    }
  }
  check('이음새 연속성 localX(i+1)==localX(i)-SCREEN_W (전 시각/전 i)', ok);
}

// 2) 사이클 경계 wrap 연속성: t와 t+cycleMs는 같은 위치(주기성)
{
  const t = 5000;
  const x1 = tickerLocalX({ ...base, nowMs: t, deviceIndex: 1 });
  const x2 = tickerLocalX({ ...base, nowMs: t + base.cycleMs, deviceIndex: 1 });
  check('사이클 주기성 localX(t)==localX(t+cycleMs)', approx(x1, x2, 1e-3));
}

// 3) rtl 방향: 시간이 지나면 localX가 감소(좌로 이동)
{
  const x0 = tickerLocalX({ ...base, nowMs: 1000, deviceIndex: 0 });
  const x1 = tickerLocalX({ ...base, nowMs: 1100, deviceIndex: 0 });
  check('rtl: 시간 증가 시 localX 감소(좌로 흐름)', x1 < x0);
}

// 4) ltr 방향: 시간이 지나면 localX가 증가(우로 이동)
{
  const x0 = tickerLocalX({ ...base, direction: 'ltr', nowMs: 1000, deviceIndex: 0 });
  const x1 = tickerLocalX({ ...base, direction: 'ltr', nowMs: 1100, deviceIndex: 0 });
  check('ltr: 시간 증가 시 localX 증가(우로 흐름)', x1 > x0);
}

// 5) cycleMs 미보고 시 textW 기반 추정 사용(0 아님), 보고 시 그 값 사용
{
  const est = tickerCycleMs({ cycleMs: 0, speed: 50, totalDevices: 4, textW: 600 });
  const expected = ((SCREEN_W * 4 + 600) / 50) * 1000;
  check('cycleMs 미보고 → textW 추정', approx(est, expected, 1e-6));
  check('cycleMs 보고 → 보고값 사용', tickerCycleMs({ cycleMs: 37000, speed: 50, totalDevices: 4, textW: 600 }) === 37000);
}

// 6) 음수 nowMs(시계 오프셋 보정으로 음수 가능)에서도 NaN/예외 없음
{
  const x = tickerLocalX({ ...base, nowMs: -1234, deviceIndex: 2 });
  check('음수 nowMs 안전(NaN 아님)', Number.isFinite(x));
}

console.log(`\n${pass}/${pass + fail} 통과`);
process.exit(fail === 0 ? 0 : 1);
