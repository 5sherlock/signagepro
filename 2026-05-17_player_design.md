# SignagePro 현장 Player 구조 설계 (2026-05-17)

> Track A-1 (Android 클라이언트) 진입 전 설계 문서.
> 작성: Claude Opus 4.7 — 사용자 확인 후 다음 컴퓨터에서 이어서 진행.

---

## 0. 이 문서의 목적

내일 다른 컴퓨터에서 이어 작업할 수 있도록, 현재까지 파악한 player 구조와
다음 단계 진입을 위해 **결정해야 할 4가지 사항**을 정리한 핸드오프 문서.

작업 재개 시 → 본 문서의 §6 "다음 단계" 항목부터 보고 사용자에게 결정 사항 확답
받은 뒤 §7의 진입점 작업으로 들어갈 것.

---

## 1. Player가 만족해야 할 외부 계약 (서버/대시보드 측 현황)

현재 서버 코드를 역산해서 정의한 contract. **player는 아래를 지키도록 구현되어야 함.**

| 구분 | 위치 | 형식 |
|---|---|---|
| 식별자 | `server/prisma/schema.prisma` `Device.id` | UUID 또는 MAC (수동 등록 가능, `server/index.js:181`) |
| 하트비트 | `server/index.js:397` | TCP `:10080`, `status:<deviceId>/cpu:<%>/mem:<%>` → 서버 `ok:\n` ACK |
| 재생목록 fetch | `server/index.js:325` | `GET /api/groups/:groupId/playlist` → `{medias:[{media{path,type,filename}, order, duration, targetDeviceId, transition, transitionTime}]}` |
| 재생목록 변경 알림 | `server/index.js:378` | Socket.io 이벤트 `playlist_updated {groupId}` |
| 미디어 다운로드 | `server/index.js:16` | `GET /uploads/<file>` (정적 서빙) |
| 기기 배정 변경 | `server/index.js:189` | Socket.io 이벤트 `group_assignment_changed` |

**핵심 규칙:**
Player는 "자기 deviceId가 속한 groupId의 playlist를 가져와,
`targetDeviceId === self || null`만 필터링해서 재생" 해야 함.
(`dashboard/src/App.jsx:29` 미리보기 로직과 동일하게 맞추면 호환됨)

---

## 2. 권장 모듈 분해 (Android Native, Kotlin 기준 가정)

```
┌─────────────────────────────────────────────────────────┐
│  KioskActivity (FullScreen, Immersive)                  │
│   └─ PlayerView (ExoPlayer 영상 / ImageView 이미지)      │
│      + TransitionLayer (fade/dissolve/slide 대응)        │
└─────────────────────────────────────────────────────────┘
              ▲                            ▲
              │ play(next)                 │ status push
┌─────────────┴─────────────┐  ┌──────────┴────────────────┐
│ PlaylistEngine            │  │ HeartbeatService          │
│  - 현재 index, 듀레이션    │  │  - TCP 10080 keepalive    │
│  - targetDeviceId 필터     │  │  - 주기적 cpu/mem 전송    │
│  - NTP 시각 기준 동기 재생 │  │  - 끊김 시 지수 백오프     │
└─────────────┬─────────────┘  └──────────┬────────────────┘
              │ load                       │
┌─────────────┴─────────────┐  ┌──────────┴────────────────┐
│ MediaCacheRepo            │  │ ControlChannel (Socket.io)│
│  - /uploads/<file> 다운로드│  │  - playlist_updated 구독  │
│  - SHA/사이즈 기반 캐시키  │  │  - group_assignment_…     │
│  - LRU + 오프라인 부팅 지원│  │  - 재접속 자동 처리        │
└───────────────────────────┘  └───────────────────────────┘
              ▲
              │
┌─────────────┴─────────────┐
│ ApiClient (Retrofit/OkHttp)│
│  - GET /api/groups/:id/playlist
│  - GET /uploads/<file>
└───────────────────────────┘
```

추가 필수 컴포넌트:

- **NtpClient**: 5대 동기 재생 핵심. `pool.ntp.org` 또는 서버 자체 NTP에서 epoch ms 받아
  `nextSlideAt = floor(now/duration)*duration` 식으로 슬롯 정렬.
- **WatchdogService**: ANR/크래시 시 `JobScheduler` + `BOOT_COMPLETED` 리스너로 자가 부활.
- **ConfigStore**: SharedPreferences에 `serverUrl`, `deviceId`, `lastPlaylistHash`.

---

## 3. Player 상태머신 (싱크 + 오프라인 대응)

```
   [BOOT]
     │ load deviceId, serverUrl
     ▼
   [CONNECT]──fail──▶[OFFLINE_PLAY] (캐시 last playlist 반복)
     │ ok                   │ 네트워크 복구
     ▼                      └──────┐
   [FETCH_PLAYLIST]                │
     │                             │
     ▼                             │
   [DOWNLOAD_MISSING]◀──────────────┘
     │ done
     ▼
   [SYNC_WAIT]  ← NTP 기반 슬롯 경계 대기
     │
     ▼
   [PLAY_SLIDE]──duration timeout──▶ [PLAY_SLIDE next]
     │ playlist_updated 이벤트
     └──────────────▶ [FETCH_PLAYLIST]
```

**핵심 원칙 3가지:**

1. 재생 중에는 절대 끊기지 않게 — 새 playlist 도착 시 현재 슬라이드 끝나고 적용
2. 네트워크 끊겨도 멈추지 않게 — 캐시된 마지막 playlist 무한 루프
3. 5대 싱크는 NTP 기준 시각 슬롯 — 보드별 currentIndex 계산이 동일해지도록

---

## 4. 와이브로드 레퍼런스에서 가져갈 점 / 버릴 점

`reference/y-board-premium/net/ybroad/dispy/lib/PlayData.java`, `ClientData.java`에서:

**가져갈 것:**
- `type` 분류 (video/image/web/text) — 미디어 종류 확장 시 유용
- `sync` / `synclan` 두 종류 동기화 모드 분리 개념
- 4-cell 분할 화면(`cell[4]`) — 향후 PIP/멀티존 확장 대비 인지

**버릴 것:**
- `˼` ~ `˿` 같은 비프린터블 구분자 → JSON으로 대체
- 큰 ClientData 단일 객체 push 방식 → REST + 이벤트 분리 모델이 이미 우월
- 4-cell 동시 재생 로직 → 오설록은 1셀 풀스크린만 필요

---

## 5. 현재 코드의 갭 (player 구현 전 서버 보강 필요)

확인 결과 다음은 player가 정상 동작하려면 서버 쪽에 **선결**이 필요함:

1. **deviceId → groupId 조회 API 부재**
   player가 부팅 시 자기 그룹을 찾을 방법이 없음.
   `GET /api/devices/:id` 또는 TCP 헬로 응답에 `groupId` 포함 필요.

2. **TCP 프로토콜이 텍스트 한 줄짜리** (`server/index.js:397`)
   재생 명령(play/stop/reload)을 내려보낼 채널이 없음.
   Socket.io를 player에서 직접 쓰거나 TCP에 명령 프레이밍 추가 필요.

3. **NTP 동기화 기준점 미정의**
   어떤 시계를 기준으로 슬라이드 슬롯을 자를지 결정 필요.
   서버에서 `playlist.epochStart` 같은 필드 내려보내는 게 깔끔.

4. **미디어 무결성 체크 없음**
   `Media` 모델에 `size`만 있고 `hash` 없음.
   캐시 검증용으로 추가 권장.

---

## 6. 다음 단계 — 사용자에게 받아야 할 결정 (작업 재개 시 최우선)

| # | 결정 사항 | 선택지 |
|---|---|---|
| A | Player 런타임 | (1) Android Native(Kotlin + ExoPlayer) / (2) WebView 기반 (현재 대시보드 DevicePreview 재활용) |
| B | 제어 채널 | (1) Socket.io 클라이언트 직접 연결 / (2) TCP 프로토콜 확장 |
| C | 싱크 기준 | (1) 외부 NTP / (2) 서버 자체 시각 브로드캐스트 |
| D | 캐시 정책 | (1) playlist 전체 prefetch / (2) 현재+다음 1개만 미리 |

이 4가지가 정해진 뒤 §7 진입점으로 들어가야 함. **결정 없이 코딩 시작 금지**
(ruleforai.md 1조 — 사용자 명시 지시 없이 독단 코딩 금지).

---

## 7. 작업 재개 시 진입점

§6의 4가지 결정이 끝나면 아래 순서로 진행:

1. **서버 보강** (player 의존성 선결 — §5 + §9.1 항목)
   - **§9.1 #1 TCP 메시지 프레이밍 수정** (줄바꿈 구분자 + 버퍼 누적)
   - **§9.1 #2 디바이스 오프라인 감지 구현** (소켓→deviceId 매핑 + close 핸들러)
   - **§9.1 #3 최소 인증 추가** (TCP 핸드셰이크 + REST 토큰 헤더)
   - `GET /api/devices/:id` 추가 (group/store include)
   - `Media` 모델에 `hash` 필드 추가 + 업로드 시 SHA-256 계산
   - `Playlist`에 `epochStart` 또는 동등 동기화 anchor 추가 (C번 결정에 따라)

2. **player 프로젝트 골조 생성**
   - 결정 A에 따라 `android/` 또는 `player/` 디렉토리 신설
   - ConfigStore + 부팅 시 deviceId 입력 UI

3. **통신 레이어**
   - ApiClient → playlist fetch
   - ControlChannel (B에 따라 Socket.io 또는 TCP)
   - HeartbeatService

4. **재생 엔진**
   - MediaCacheRepo (D에 따라 prefetch 전략)
   - PlaylistEngine + NtpClient (C에 따라)
   - TransitionLayer

5. **현장 검증** — 5대 동기 재생, 네트워크 단절 복구 시나리오

---

## 8. 참고: 본 분석에서 읽은 파일 목록

작업 재개 시 같은 컨텍스트를 빠르게 재구축하려면 아래만 다시 읽으면 됨:

- `server/index.js` (REST + TCP + Socket.io 전체)
- `server/prisma/schema.prisma` (DB 모델)
- `dashboard/src/App.jsx` (DevicePreview 재생 로직 = player 참조 모델)
- `clauderoadmap.md` (프로젝트 배경)
- `implementation_plan.md`, `development_roadmap.md` (Phase 계획)
- `2026-05-07_progress.md` (이전 진척)
- `ruleforai.md` (개발 원칙)
- `reference/y-board-premium/net/ybroad/dispy/lib/{PlayData,ClientData,MySocket}.java` (레퍼런스 프로토콜)

---

*본 문서는 §6의 4가지 결정을 받기 전까지 코드 변경 없이 분석 단계로만 유지.*

---

## 9. 현재 코드 전반의 발견된 문제점 (2026-05-17 추가 분석)

### 9.1 Critical — 운영 시작하면 바로 터질 것들

**#1 TCP 메시지 프레이밍 부재** (`server/index.js:391`)
```js
socket.on('data', async (data) => {
  const msg = data.toString('utf-8').trim();
```
TCP는 바이트 스트림. `data` 이벤트가 메시지 단위가 아님:
- 두 하트비트 합쳐서 1패킷 → 파싱 실패
- 한 하트비트가 2패킷에 쪼개짐 → 절반은 startsWith 통과 안 함, 드롭
지금 LAN 환경에서 우연히 작동. **줄바꿈(\n) 구분자 + 클라이언트별 버퍼 누적** 필요.

**#2 디바이스 오프라인 감지 미작동** (`server/index.js:442`)
```js
socket.on('close', () => {
  console.log(...);
  // 여기서 해당 소켓에 매핑된 기기의 status를 offline으로 변경하는 로직이 들어갈 수 있습니다.
});
```
주석에 자백되어 있음. **셋톱박스 전원 빠져도 대시보드는 영원히 "온라인"** 표시.
→ `Map<socket, deviceId>` 유지 + close 시 `status='offline'` 업데이트.

**#3 인증/인가가 어디에도 없음**
- REST API 전체 공개: `DELETE /api/stores/:id`도 토큰 없이 가능 (`server/index.js:84`)
- TCP 10080도 공개: 임의 클라이언트가 `status:device-A/cpu:99`로 deviceId 점유 가능
- Socket.io CORS = `'*'` (`server/index.js:33`)

와이파이 게스트망 노출 시 즉시 위험. **REST 토큰 헤더 + TCP 핸드셰이크에 deviceSecret** 최소 추가.

### 9.2 Important — 기능 신뢰성에 영향

**#4 Playlist 스키마는 1:N인데 API는 1:1로 취급**
- 스키마: `Group.playlists Playlist[]` (`schema.prisma:28`)
- API: `findFirst({where:{groupId}})` (`index.js:327`) → 그룹당 1개만 가정
스케줄링·시간대별 재생목록 진입 막힘. **groupId에 unique 걸거나 명시적 시간 anchor 추가**.

**#5 하트비트 파싱이 NaN을 그대로 DB 저장** (`server/index.js:404`)
`parseFloat(p.substring(4))`만 호출. 깨진 입력 시 `cpuUsage = NaN`. Postgres 마이그레이션 시 폭발.
→ `Number.isFinite` 가드.

**#6 업로드 크기·타입 검증 없음 — DoS 가능** (`server/index.js:30`)
- `multer({ storage })` — `limits.fileSize` 미설정
- mimetype은 브라우저 자체 신고를 그대로 신뢰 (`index.js:237`) → `.exe`에 `video/mp4` 헤더 붙이면 통과

**#7 DevicePreview가 디바이스당 별도 socket + 별도 fetch** (`App.jsx:43`)
5대 보드 = Socket.io 연결 5개 + `/playlist` 호출 5번. App 부모에도 별도 소켓.
→ 소켓 1개 공유 + playlist 캐시 단일화.

**#8 `Device.macAddress` 필드는 죽은 코드** (`schema.prisma:36`)
`@unique` 잡혀 있지만 upsert는 `id`로만. 의도와 사용처 불일치.

**#9 `PlaylistMedia.targetDeviceId`는 FK가 아닌 문자열** (`schema.prisma:80`)
디바이스 삭제 시 잔존 참조 → 영원히 매칭 안 되는 슬라이드가 playlist에 남음.

### 9.3 Operational — 프로덕션 가기 전 보강

**#10 비디오 미리보기가 실제 영상이 아님** (`App.jsx:117`) — `<div>VIDEO: filename</div>`로 끝.

**#11 SQLite 인덱스 0개** — `Device.groupId`, `Media.storeId`, `PlaylistMedia.playlistId` 풀스캔. Track B(클라우드/PG) 전 보강 필수.

**#12 Graceful shutdown 없음** — SIGTERM/Electron 종료 시 Prisma disconnect·TCP close 미구현. A-2 패키징 시 DB 락 잔존 위험.

**#13 백업 전략 없음** — `server/prisma/dev.db` 단일 파일. HDD 죽으면 모든 매장/기기/미디어 메타 증발.

**#14 구조화 로깅·로테이션 없음** — `console.log`만. 현장 장애 추적 불가.

### 9.4 권장 우선순위

| 순위 | 작업 | 이유 |
|---|---|---|
| 1 | #1 TCP 프레이밍 + #2 offline 감지 | 시스템 정확성의 기반 |
| 2 | #3 최소 인증 | 와이파이 노출 위험 |
| 3 | #4 Playlist 정리 + #9 targetDeviceId FK화 | A-1 player 진입 전 스키마 안정화 |
| 4 | #5~#7 입력 검증·연결 최적화 | 안정성 |
| 5 | #11~#13 운영 보강 | A-2 Electron 패키징 직전 |

**#1~#3은 A-1 player 작업 시작 전 반드시 같이 처리** — player가 의존하는 contract가 바로 이 부분이라, 깨진 상태에서 player를 구현하면 두 번 일하게 됨.

---

## 10. 재작성(rewrite) vs 점진 개선 — 결정 기록

§9의 문제 목록을 보고 "처음부터 다시 짜는 게 낫지 않냐"는 자연스러운 의문에 대한 답.

### 결론: **재작성 불필요. 점진 개선 권장.**

### 근거

| 측면 | 분석 |
|---|---|
| **문제의 성격** | 모두 **국소적 수정**으로 해결 가능. TCP 프레이밍 ~20줄, offline 감지 ~30줄, auth 미들웨어 ~50줄. 아키텍처 결함이 아닌 구현 누락. |
| **아키텍처 적합성** | Express + Socket.io + TCP + Prisma + Vite/React = 5대 보드 규모와 Track A/B(온프레→클라우드) 진로에 정확히 맞음. 재작성해도 같은 스택이 다시 나올 가능성 매우 높음. |
| **이미 동작하는 부분** | 대시보드 Swimlane UI, 그룹/사업장 관리, 미디어 업로드, 전환 효과 → 잘 만들어졌고 버릴 이유 없음. |
| **시간 비용** | 재작성 시 최소 2~3주 회귀. 그동안 player 작업 진입 못 함. 같은 결과물에 도달. |
| **리스크** | 이미 검증된 코드를 버리고 새로 만들면 새로운 버그가 더 많이 생김. "재작성하면 깨끗해진다"는 흔한 함정. |

### 재작성이 정당화될 시나리오 (지금은 해당 없음)

- 스택 자체를 바꾸고 싶을 때 (예: Go로 단일 바이너리화, TypeScript로 통일)
- 멀티 테넌시·SaaS 전환처럼 데이터 모델 자체가 근본적으로 달라질 때
- 디버깅이 신규 작성보다 오래 걸리는 수준의 누적 부채 (현재 아님)

### 권장 액션

1. §6의 4가지 결정 받기
2. §9.1 (#1~#3) 수정 — A-1 진입 전 함께 처리
3. §7 진입점 순서대로 player 구현
4. §9.2 항목은 player 결합하면서 자연스럽게 수정
5. §9.3은 A-2(Electron 패키징) 직전에 보강

> **요약**: "재작성"이 아니라 **A-1 진입과 동시에 §9.1 3개 항목을 같이 끝내는 통합 작업**으로 진행하면 됨. 같은 시간 동안 player 진척 + 기반 안정화를 동시에 얻음.

