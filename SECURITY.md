# SignagePro 보안 점검 보고서

**점검일:** 2026-06-10  
**대상:** https://signage-pro.com (Cloudflare 터널 외부 노출 서버)  
**점검 방법:** 블랙박스 모의 침투 테스트 (외부 공격자 관점)

---

## 모의 공격 결과

### 확인된 취약점 1 — adminPassword 미설정으로 인증 전면 비활성화

**심각도:** Critical  
**상태:** 미패치 (운영 서버 기준)

`server/.env`에 `adminPassword`가 설정되지 않아 서버 코드의 다음 조건이 모든 인증을 우회:

```js
// server/index.js:112
const requireAuth = (req, res, next) => {
  if (!adminPassword) return next(); // adminPassword 없으면 무조건 통과
```

**실제 공격 결과 (토큰 없이 호출):**

```
GET  /api/devices              → HTTP 200  전체 기기 목록 + 실시간 상태 노출
GET  /api/diagnostics/sockets  → HTTP 200  Socket.io 연결 ID 17개 전체 노출
GET  /api/diagnostics/ota      → HTTP 200  OTA 배포 현황 전체 노출
```

**노출된 정보:**
- 기기 8대 ID, 이름, 실IP (222.116.38.23, 211.43.136.235, 211.184.50.200)
- 사업장명 (오설록, 이니스프리)
- 각 기기의 현재 재생 파일명, CPU/메모리/디스크 실시간 상태
- Socket.io 내부 연결 ID 전체

**조치:**  
운영 서버 `server/.env`에 추가 후 `pm2 restart signagepro --update-env`

```
adminPassword="강력한비밀번호"
```

---

### 확인된 취약점 2 — dev-mode 토큰 하드코딩

**심각도:** Critical  
**상태:** 미패치

```js
// server/index.js:114
if (token === 'dev-mode') return next(); // 고정 문자열로 인증 우회
```

`adminPassword`가 설정된 이후에도 `Authorization: Bearer dev-mode` 헤더만 붙이면 모든 API 우회 가능. 소스코드가 노출되거나 추측될 경우 즉시 악용 가능.

**조치:** 해당 라인 삭제.

---

### 확인된 취약점 3 — 비밀번호 미설정 시 패스워드 변경 무인증 허용

**심각도:** Critical  
**상태:** adminPassword 설정 후 해소됨

`adminPassword`가 없는 상태에서 누구나 새 비밀번호를 설정 가능 → 관리자 잠김 공격 가능:

```bash
curl -X POST https://signage-pro.com/api/auth/change-password \
  -H "Content-Type: application/json" \
  -d '{"current":"","newPassword":"hacked123"}'
# → 성공. 관리자 접근 차단됨
```

**조치:** adminPassword 설정으로 해소.

---

### 취약점 4 — 로그인 Rate Limiting 없음

**심각도:** High  
**상태:** 미패치

`/api/auth/login`에 요청 횟수 제한 없음 → 브루트포스 공격 무방비.

**조치:** `express-rate-limit` 패키지 적용.

```js
const rateLimit = require('express-rate-limit');
app.use('/api/auth/login', rateLimit({ windowMs: 15 * 60 * 1000, max: 10 }));
```

---

### 취약점 5 — CORS origin 전체 허용

**심각도:** High  
**상태:** 미패치

```js
// server/index.js:73, 183
cors({ origin: '*' })  // HTTP 및 Socket.io 모두 동일
```

**조치:** 실제 사용 도메인으로 제한.

```js
cors({ origin: ['https://signage-pro.com'] })
```

---

### 취약점 6 — /uploads 인증 없이 공개 접근

**심각도:** High  
**상태:** 미패치

```js
// server/index.js:137
app.use('/uploads', express.static(...)) // 미디어 파일 무인증 공개
```

외부에서 `/uploads/` 경로로 모든 미디어 파일 직접 다운로드 가능.

**조치:** `requireAuth` 미들웨어 적용 또는 Cloudflare Access 규칙 추가.

---

### 취약점 7 — 비밀번호 최소 길이 4자

**심각도:** Medium  
**상태:** 미패치

```js
// server/index.js:99, LoginScreen.jsx:36
if (newPw.length < 4) ...
```

**조치:** 최소 8자로 변경.

---

### 취약점 8 — 파일 업로드 경로 Traversal 위험

**심각도:** Medium  
**상태:** 미패치

```js
// server/index.js:149
filename: (req, file, cb) => cb(null, Date.now() + '-' + file.originalname)
```

`originalname`에 `../../` 등이 포함될 경우 업로드 디렉토리 외부에 파일 생성 가능성.

**조치:** `path.basename()` + 영숫자만 허용하는 sanitize 적용.

```js
const safe = path.basename(file.originalname).replace(/[^a-zA-Z0-9._-]/g, '_');
filename: (req, file, cb) => cb(null, Date.now() + '-' + safe)
```

---

### 취약점 9 — 에러 메시지 내부 정보 노출

**심각도:** Low  
**상태:** 미패치

여러 엔드포인트에서 `details: err.message`를 클라이언트에 직접 반환 → DB 구조, 파일 경로 등 내부 정보 노출 가능.

**조치:** 프로덕션 환경에서 `details` 필드 제거.

---

## 패치 우선순위

| 순위 | 취약점 | 난이도 | 작업 |
|------|--------|--------|------|
| 1 | adminPassword 미설정 | 즉시 | 운영 서버 `.env` 수정 + PM2 재시작 |
| 2 | dev-mode 토큰 | 1줄 삭제 | `index.js:114` 삭제 |
| 3 | Rate Limiting | 패키지 설치 | `express-rate-limit` 적용 |
| 4 | CORS 제한 | 설정 수정 | origin 도메인 지정 |
| 5 | /uploads 인증 | 미들웨어 추가 | `requireAuth` 적용 |
| 6 | 파일명 Sanitize | 코드 수정 | `path.basename` + 정규식 |
| 7 | 비밀번호 최소 길이 | 상수 변경 | 4 → 8 |
| 8 | 에러 메시지 | 조건부 제거 | `NODE_ENV` 분기 |

---

## Cloudflare 활용 추가 보안

- **Cloudflare Access (Zero Trust):** 대시보드 URL에 Google 계정 등 추가 인증 레이어 적용
- **WAF Rate Limiting Rule:** `/api/auth/login` 엔드포인트 요청 수 제한 (서버 코드 수정 없이 가능)
- **Bot Fight Mode:** 자동화된 스캔/크롤링 차단
