# SignagePro 보안 문서 (통합본)

> 최종 재작성: 2026-06-13 — 기존 `SECURITY.md` + `SECURITY_REPORT.md`를 통합하고,
> **실제 코드(`server/index.js`) 및 실측 환경 토폴로지** 기준으로 상태를 재검증했습니다.
> 환경 구성(포트/폴더/브랜치)은 [DEPLOYMENT.md](DEPLOYMENT.md) 참조.

대상: dev(3001) · staging(3300) 로컬 백엔드 + 현장/운영 서버(`jeju-osulloc:3300`). <span style="color:red;text-decoration:line-through">원격 운영 서버(https://signage-pro.com, Cloudflare 터널 노출)</span> → **Cloudflare 차단·Tailscale 전환(2026-06-13)**.
점검 방식: 코드 리뷰 + 외부 블랙박스 관점.

> **기준 코드: `dev` 브랜치(dea5d59).** 본 문서의 line 번호·"패치됨" 판정은 모두 dev 기준이다. 보안 작업은 dev에서 진행하고, dev→staging→main 승격 후 staging에서 재검증한다.
>
> ⚠️ **운영 실상태 경고 (2026-06-13):** dev→staging→main 승격이 **한 번도 수행된 적 없음.** 운영이 받는 `main`은 **v0.4.27(1e8a80e, 06-07)** 로 dev보다 뒤처져 있고, **로그인 rate-limit/잠금 기능이 아예 없다**(아래 2.1의 "패치됨"은 dev 한정). 즉 **운영의 실제 노출 수준은 본 문서보다 더 나쁠 수 있다** — 승격 전까지는 dev 패치가 운영에 반영되지 않는다. 운영 실버전은 AnyDesk로 직접 확인 필요.
>
> 🔄 **인프라 변경 (2026-06-13 — Tailscale 전환):** Cloudflare 공개 터널(`signage-pro.com`) **차단 완료**(현장 서버 cloudflared 중지/비활성) → **Tailscale 사설망 전환.** 현장 서버(jeju-osulloc 100.122.76.95)·기기·관리 PC를 tailnet으로 묶고 MagicDNS(`jeju-osulloc:3300`)로 접속. **dev-204 루팅 + Tailscale 원격 ADB 작동 확인**(`adb connect 100.75.80.108:5555`). → **인터넷 직접 노출이 tailnet 내부로 축소**돼 아래 취약점들의 공개 위험이 크게 낮아짐.
> **단, 잔존 공개 포트포워딩**(field `121.189.102.108:3300`, dev `211.184.50.200:3001`)이 아직 인터넷에 열려 있음 → 기기 Tailscale 전환 완료 후 닫을 것. 그전까지 §3.2~3.13은 그 포트로 여전히 노출.

---

## 0. 요약 — 지금 가장 먼저 볼 것

| # | 항목 | 심각도 | 상태 |
|---|------|--------|------|
| 1 | `trust proxy` 미설정 → Rate Limit 무력화 + 전역 잠금 DoS | Critical | ✅ 패치됨(dev) · main/운영 미반영 |
| 2 | dev-mode 토큰 백도어 | Critical | 미패치 |
| 3 | Socket.io 채널 완전 무인증 (기기 위장·DB 위조) | Critical | 미패치 |
| 4 | 환경 간 비밀값 공유 (dev=staging=운영 동일 secret/비번) | Critical | 미패치 |
| 5 | adminPassword 약함 / 미설정 시 인증 전면 비활성 | High | 부분 |

---

## 1. 환경 분리 위험 (신규 — 토폴로지 점검에서 발견)

dev · staging · 운영이 **동일한 비밀값**을 쓰고 있어, 환경을 IP:포트로 분리해도 **신뢰(인증) 격리는 없습니다.**

- 모든 `.env`의 `DEVICE_SECRET`이 예측 가능한 동일값(`signagepro-secret-2026`). 코드 기본값도 `'changeme'` → [index.js:79](server/index.js#L79).
- 모든 `adminPassword`가 동일하고 약함(단순 영숫자).
- 참고: `.env`는 `.gitignore`에 있어 **git에는 안 올라감**(양호). 노출 경로는 **USB 백업 + LAN 평문 전송**.

**정확한 위험 (IP:포트 분리로 *못* 막는 것)**
- IP:포트는 "경로"만 분리한다(dev→:3001, 운영→원격). 평상시 환경이 섞이진 않으므로 "기기가 실수로 다른 환경에 붙는" 시나리오는 **확률 낮음**.
- 진짜 문제는 **비밀값 재사용**이다. dev API(:3001)는 `0.0.0.0` 바인딩 + 평문 HTTP라 **같은 LAN의 누구나 접근**할 수 있고, 약한 비밀번호 추측 또는 평문 스니핑으로 자격증명을 얻을 수 있다. 그 값이 모든 환경과 동일하므로 → **한 곳만 털려도 전 환경이 함께 침해** = 단일 실패점. **(2026-06-13 dev-204 실측: `device_secret = signagepro-secret-2026` 그대로 — 위험 실증.)** <span style="color:red;text-decoration:line-through">공개된 signage-pro.com 경유 즉시 침해</span> Cloudflare는 차단됐지만 잔존 포트포워딩·tailnet 내부로는 여전히 도달 가능.
<span style="color:red;text-decoration:line-through">- staging이 운영을 충실히 흉내내지 못함: R2 키가 dev `.env`에만 존재 → dev는 R2, staging은 로컬 디스크.</span>
- **(2026-06-14) dev `.env`의 R2 비활성화(주석 처리)** → **dev·staging 모두 로컬 `/uploads` 서빙으로 통일.** R2(공용 클라우드) 의존 폐기 방향(운영/현장도 순차 전환 예정). ⚠️ **현장/운영 서버는 아직 R2 사용 중**(DB 미디어 경로가 R2 URL) — 전환 완료 전까지 R2 버킷·키 삭제 금지.

**조치**
- 환경별로 **서로 다른** `DEVICE_SECRET` / `adminPassword` 발급(랜덤 32바이트+). 이게 핵심 — 그러면 접속을 :3001로 하든 :3300으로 하든 한 곳 침해가 다른 곳으로 번지지 않는다.
- 운영용 비밀값은 dev와 절대 공유 금지. USB 백업 평문 보관·LAN 평문 전송 주의. <span style="color:red;text-decoration:line-through">(운영은 Cloudflare로 HTTPS 종단)</span> → **Tailscale(WireGuard) 암호화로 대체.**
- staging은 운영 "리허설" 환경이므로 **구성은 운영과 동일하게** 맞추되([DEPLOYMENT.md §1.5](DEPLOYMENT.md)), 비밀값(secret/비번)은 staging 전용 값을 써서 **테스트 기기가 운영 기기로 새지 않도록** 한다. (staging이 운영과 같은 `DEVICE_SECRET`이면 실기기가 staging TCP에 붙어 OTA/명령을 받을 수 있음 — §3.3 참조)
- **(신규 2026-06-14) 미사용 R2 자격증명 폐기 예정**: R2를 더 이상 쓰지 않으므로, **현장/운영까지 로컬 전환을 끝낸 뒤** `.env`의 R2 액세스/시크릿 키를 **Cloudflare에서 revoke·삭제**한다. 방치된 활성 키는 노출 위험(이미 코드·문서에 노출됨). dev `.env`에서도 주석 → 완전 제거 권장. **단 현장 전환 전 삭제 금지(현장 미디어가 R2에 있음).**

---

## 2. 코드 취약점 — 현재 상태 (실제 `server/index.js` 기준)

### 2.1 이미 패치됨 (재작업 불필요)

| 항목 | 근거 | 비고 |
|------|------|------|
| 다수 관리 API 무인증 | [index.js:198](server/index.js#L198) | `app.use('/api', …)` 블랭킷 `requireAuth` 적용. DEVICE_OPEN GET·`/auth`만 예외 |
| RCE `/api/debug/run-cmd` 무인증 | [index.js:1468](server/index.js#L1468) | 위 블랭킷 미들웨어 뒤로 이동되어 인증 필요 상태 |
| 로그인 Rate Limit 없음 | [index.js:136-159](server/index.js#L136-L159) | **dev 한정** 자체 구현(5회→15분 잠금). ⚠️ **main(v0.4.27)·운영엔 아직 없음** — 승격 필요 |
| `trust proxy` 미설정(전역 잠금 DoS) | [index.js:70-71](server/index.js#L70-L71) | **dev 패치 완료(2026-06-13)** — `trust proxy:'loopback'` + `getClientIp()`(CF-Connecting-IP 우선). ⚠️ main/운영 미반영 |
| 업로드 형식 무검증 | [index.js:222](server/index.js#L222) | `ALLOWED_MIME` 화이트리스트 |
| 토큰 서버 재시작 시 소멸 | [index.js:107](server/index.js#L107) | HMAC 서명 토큰 — **단, §3.5 키 약함** |

> ※ 구 `SECURITY_REPORT.md`의 "20+ API 무인증", "run-cmd RCE 무인증", "rate limit 없음", "MIME 무검증"은 **해소됨**.

### 2.2 미패치 (위치 확인됨)

#### ✅ 3.1 `trust proxy` 미설정 → Rate Limit 무력화 + 전역 잠금 DoS  *(dev 패치 완료 2026-06-13)*
- 증상(과거): `trust proxy` 미설정 → Cloudflare 터널 뒤에서 모든 `req.ip`가 loopback 하나로 고정 → 로그인 잠금이 **전역**이 되어 공격자 5회 실패 시 **정상 관리자까지 15분 잠김(DoS)**.
- 패치: [index.js:70-71](server/index.js#L70-L71) — `app.set('trust proxy', 'loopback')` + `getClientIp()`(`CF-Connecting-IP` 우선) 도입. 요청 로거·로그인 잠금이 이 헬퍼 사용.
- ⚠️ **dev에만 적용됨** — main(v0.4.27)·운영엔 미반영, 승격 필요.
- 전제: origin 포트는 터널 경유로만 외부 노출돼야 함(직접 노출 시 `CF-Connecting-IP` 위조 가능).

#### 🔴 3.2 dev-mode 토큰 백도어
- 위치: [index.js:186](server/index.js#L186) — `if (token === 'dev-mode') return next();`
- `Authorization: Bearer dev-mode` 한 줄로 전 API 우회. 또한 `adminPassword` 미설정 시 로그인이 `dev-mode` 토큰을 발급([index.js:141](server/index.js#L141)).
- 조치: 해당 라인 삭제. dev 편의는 환경변수 분기로 대체(운영 빌드엔 미포함).

#### 🔴 3.3 Socket.io 채널 완전 무인증  *(신규)*
- 위치: [index.js:251-262](server/index.js#L251-L262) — `io.use()` 인증 미들웨어 없음, CORS `*`.
- 누구나 접속해서:
  - `register_device`([263](server/index.js#L263))에 임의 deviceId 등록 → 그 기기 룸 가입 → 서버가 보내는 `run_cmd`/`update_apk` **수신(기기 위장)**.
  - `run_cmd_result`([319](server/index.js#L319))로 임의 명령 결과 **위조 주입**.
  - `web_player_heartbeat`([294](server/index.js#L294)) → `prisma.device.upsert`로 **임의 기기 DB 생성/온라인 위장/임의 IP 기록**.
- TCP 채널은 `DEVICE_SECRET`로 인증하는데([1586](server/index.js#L1586)) Socket.io만 무방비 — 일관성 구멍.
- 조치: `io.use()` 핸드셰이크에서 토큰/기기 secret 검증, CORS 도메인 제한.

#### 🔴 3.4 adminPassword 미설정 시 인증 전면 비활성
- 위치: [index.js:85](server/index.js#L85), [index.js:184](server/index.js#L184) — `if (!adminPassword) return next();`
- 운영 `.env`에 반드시 강한 `adminPassword` 설정 + §1대로 환경별 분리.

#### 🟡 3.5 HMAC 토큰 서명키 = adminPassword
- 위치: [index.js:107-110](server/index.js#L107-L110) — HMAC 키로 사용자 비밀번호를 직접 사용.
- 비밀번호가 약하면(아래 3.8) 토큰 오프라인 위조/브루트포스 가능.
- 조치: 별도 강한 서버 시크릿(`TOKEN_SECRET`)으로 서명키 분리.

#### 🟡 3.6 CORS 전체 허용
- 위치: [index.js:122](server/index.js#L122)(HTTP), [index.js:252](server/index.js#L252)(Socket.io) — `origin: '*'`.
- 조치: 실제 사용 출처만 허용 <span style="color:red;text-decoration:line-through">(https://signage-pro.com)</span> → 이제 **Tailscale 주소(`jeju-osulloc:3300` 등)** 기준.

#### 🟡 3.7 `/uploads` 무인증 공개
- 위치: [index.js:208](server/index.js#L208) — `express.static`이 인증 없이 노출.
- **(2026-06-14) 맥락 변화**: 미디어가 **공개 R2 URL(링크만 알면 누구나) → 로컬 `/uploads`** 로 이전(dev 적용·현장 예정) → **외부 공개 노출은 사라지고 tailnet/LAN 내부 무인증만 남음**(노출 범위 축소). 단 내부 무인증은 여전히 유효.
- 조치: `requireAuth` 적용 <span style="color:red;text-decoration:line-through">또는 Cloudflare Access 규칙</span> (Cloudflare 차단됨).

#### 🟡 3.8 비밀번호 최소 4자
- 위치: [index.js:172](server/index.js#L172) — 변경 엔드포인트의 실제 게이트.
- 조치: 최소 12자 + 복잡도 요구.

#### 🟡 3.9 업로드 파일명 Path Traversal
- 위치: [index.js:220](server/index.js#L220) — `Date.now() + '-' + file.originalname` (sanitize 없음).
- 조치: `path.basename()` + `[^a-zA-Z0-9._-]` → `_` 치환.

#### 🟢 3.10 DEVICE_SECRET 로그 평문 노출
- 위치: [index.js:1587](server/index.js#L1587) — 인증 실패 시 `secret`/`expected`를 평문 로깅.
- 조치: 로그에서 secret 값 제거.

#### 🟢 3.11 비상수시간 비교 (타이밍)
- 위치: [index.js:152](server/index.js#L152)(비번), [index.js:1586](server/index.js#L1586)(기기 secret) — `!==` 직접 비교.
- 조치: `crypto.timingSafeEqual`.

#### 🟢 3.12 에러 메시지 내부정보 노출
- 위치: [index.js:364](server/index.js#L364) 외 다수 — `details: err.message` 직접 반환.
- 조치: `NODE_ENV==='production'`에서 `details` 제거.

#### 🟢 3.13 보안 헤더(helmet) 전무
- 위치: [index.js:132](server/index.js#L132) 부근 — `helmet` 미적용 (HSTS/X-Frame-Options/CSP 없음).
- 조치: `helmet()` 추가.

---

## 3. 패치 우선순위 로드맵

| 순위 | 항목 | 위치 | 난이도 |
|------|------|------|--------|
| <span style="color:red;text-decoration:line-through">1</span> | ✅ <span style="color:red;text-decoration:line-through">trust proxy + CF-Connecting-IP</span> **dev 완료(2026-06-13)** — 승격 필요 | §3.1 | — |
| 2 | dev-mode 토큰 삭제 | §3.2 | 1줄 |
| 3 | 환경별 secret/비밀번호 분리 (dev≠staging≠운영) | §1 | 30분 |
| 4 | Socket.io `io.use()` 인증 | §3.3 | 1시간 |
| 5 | CORS 도메인 제한 (HTTP+Socket.io) | §3.6 | 10분 |
| 6 | DEVICE_SECRET 로그 제거 | §3.10 | 1줄 |
| 7 | 업로드 파일명 sanitize | §3.9 | 10분 |
| 8 | `/uploads` 인증 | §3.7 | 10분 |
| 9 | 토큰 서명키 분리 + 비번 최소 12자 | §3.5, §3.8 | 30분 |
| 10 | helmet · timingSafeEqual · 에러 details 제거 | §3.13/3.11/3.12 | 30분 |

> 운영 서버 변경은 자율 배포 금지 — staging에서 검증 후 AnyDesk로 사람이 직접 적용. ([DEPLOYMENT.md](DEPLOYMENT.md))

---

## 4. 인프라 보안 (Cloudflare · 공유기)

### <span style="color:red;text-decoration:line-through">Cloudflare (signage-pro.com)</span> → ✅ 2026-06-13 차단·Tailscale로 대체
- **현재: Cloudflare 터널 차단 완료.** 현장 서버(jeju-osulloc)의 `Cloudflared` 서비스 중지/비활성 → `signage-pro.com` 다운. 접근은 **Tailscale**(`jeju-osulloc:3300` / MagicDNS, tailnet `tailafea2d.ts.net`)로 전환.
- <span style="color:red;text-decoration:line-through">확정: https://signage-pro.com → 원격 운영 서버 IP:3300 으로 Cloudflare 터널링. 운영 API·대시보드가 외부 공개 상태이며 모든 취약점이 외부 노출.</span> (차단됨)
- <span style="color:red;text-decoration:line-through">즉효 완충: Cloudflare Access(Zero Trust) 인증 게이트, WAF Rate Limiting Rule, Bot Fight Mode 즉시 적용 권장.</span> (터널 차단으로 **불필요**)
- ⚠️ **잔존 공개 포트포워딩 (남은 과제)**: field `121.189.102.108:3300`, dev `211.184.50.200:3001` 은 아직 라우터 포워딩으로 인터넷 노출. **기기 Tailscale 전환 완료 후 이 포워딩들을 닫으면** 완전 비공개(Tailscale 전용)가 됨. 그전까지 §3.2~3.13 취약점이 이 포트로 노출.
- ✅ **원격 ADB 복구 경로 확보**: 루팅 기기 + Tailscale로 `adb connect <tailscale-ip>:5555` 가능(204 실증). OTA 장애 시 원격 복구. ADB 5555는 **tailnet 내부로만** 제한할 것(매장 LAN 노출 금지 — iptables).

### 공유기(iptime) 체크리스트
- [ ] 관리자 비밀번호 변경(기본 admin/admin 금지, 12자+)
- [ ] 외부 원격 관리 포트 비활성화
- [ ] 최신 펌웨어 업데이트
- [ ] UPnP / WPS 비활성화
- [ ] DNS 고정(1.1.1.1 / 8.8.8.8)
- [ ] 불필요한 포트포워딩 제거, 필요한 포트만 최소 개방
- [ ] Wi-Fi WPA2/WPA3 + 강한 비밀번호

### (계획) 토폴로지 전환 — NAS 운영서버 + 현장별 NAS + STB Tailscale (2026-06-14 결정)
> 아직 미구현. 결정된 방향과 보안 영향만 기록.
- **운영 서버를 현장 NAS(Docker)로 이전** + 미디어·DB도 NAS. 데스크탑 PC=로컬 백업, dev PC=오프사이트 백업, keystore=USB 추가본.
- **R2(공용 클라우드) 완전 제거** → 미디어는 현장 NAS 로컬 서빙(STB는 같은 LAN에서 수신).
- **멀티사이트**: 현장이 늘면 현장별 최소형 NAS(로컬 미디어 캐시)로 WAN 병목 회피. 중앙 DB/대시보드는 1개 유지.
- **STB도 Tailscale** 연결(공유기 설정 의존 제거). 같은 LAN이면 Tailscale이 직접 LAN 경로로 연결 → 속도 유지.
- 보안 영향:
  - **CORS 허용 출처**(§3.6)가 NAS/tailnet 주소 기준으로 바뀜.
  - **시크릿·keystore·DB가 NAS에 상주** → 접근통제·백업 대상이 NAS로 이동(NAS 관리자 비번·2FA·tailnet ACL 중요).
  - R2 제거 후 §1·§3.7의 R2 언급 정리, **R2 키 폐기**(위 §1 조치).

---

## 변경 이력
- 2026-06-14: **R2 전 환경 미사용 확정** — 현장/운영 서버 원격·직접 검사 결과 total 22개 전부 로컬, `.env`에 R2 키 없음. dev·staging도 0개. → R2 키 폐기·버킷 삭제 가능(orphan 옛 파일만 남음). Cloudflare 탈퇴 가능(도메인 처리 후).
- 2026-06-14: **R2 비활성화 반영** — dev `.env` R2 주석 → dev·staging 로컬 `/uploads` 통일(§1). `/uploads` 노출 맥락 변화(공개 R2→로컬 tailnet, §3.7). **미사용 R2 키 폐기 예정** 조치 추가(§1) — *현장/운영 R2 전환 완료 전 삭제 금지*. §4에 **NAS 운영서버 전환 계획 토폴로지** 추가.
- 2026-06-13: `SECURITY.md` + `SECURITY_REPORT.md` 통합. 실제 코드/토폴로지 기준 재검증, 환경 분리 위험·trust proxy·Socket.io 무인증 신규 반영. 기존 `SECURITY_REPORT.md` 삭제.
- 2026-06-13: **줄 번호를 dev 실파일 기준으로 보정** (dev에 R2 업로드/storeId 미커밋 변경 + trust proxy 패치로 staging 대비 라인 이동). **§3.1 trust proxy: dev 패치 완료** (`trust proxy:'loopback'` + `getClientIp`/CF-Connecting-IP). main/운영 미반영 상태.
- 2026-06-13: **인프라 변경 반영** — Cloudflare 터널(`signage-pro.com`) **차단 + Tailscale 전환.** §4 Cloudflare 권장(Access/WAF/Bot Fight) 및 공개노출 관련 문장 **줄긋기(불필요)**, 잔존 포트포워딩 경고·원격 ADB 경로 추가. dev-204 **루팅 + Tailscale 원격 ADB 작동** 확인, `device_secret` 공유(`signagepro-secret-2026`) **실측 반영**(§1).
