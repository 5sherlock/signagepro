# SignagePro 보안 취약점 분석 보고서

작성일: 2026-06-08

---

## 1. SignagePro 서버 취약점

### 🔴 HIGH — 즉시 수정 필요

#### 1-1. 인증 완전 우회 (Dev-mode 토큰)
- **위치**: `server/index.js:87`
- **설명**: `ADMIN_PASSWORD` 환경변수 미설정 시 `'dev-mode'` 토큰을 반환하여 누구나 관리자 권한 획득 가능
- **코드**: `if (!adminPassword) return res.json({ token: 'dev-mode' })`
- **조치**: `.env`에 강력한 `ADMIN_PASSWORD` 설정 필수, dev-mode 로직 제거

#### 1-2. 20개 이상 API 엔드포인트 인증 없음
- **위치**: `index.js` 라인 275, 294, 313, 391, 450, 520, 536, 554, 572, 719, 754, 795, 858, 945, 1037, 1052, 1116, 1234, 1244, 1319, 1336, 1377, 1423, 1451 등
- **설명**: 스토어/그룹/기기/미디어 삭제, APK 업로드·배포 등 중요 관리 API에 `requireAuth` 미들웨어 없음
- **조치**: 모든 관리 엔드포인트에 `requireAuth` 추가

#### 1-3. 원격 쉘 명령 실행 (RCE)
- **위치**: `server/index.js:1377`
- **설명**: `/api/debug/run-cmd` 엔드포인트가 인증 없이 모든 연결된 Android 기기에 임의 쉘 명령 전송 가능
- **영향**: 전체 기기 원격 제어, 악성코드 설치 등
- **조치**: 엔드포인트 비활성화 또는 `requireAuth` + IP 화이트리스트 적용

#### 1-4. 시크릿 키 로그 평문 노출
- **위치**: `server/index.js:1496`
- **설명**: 기기 인증 실패 시 `DEVICE_SECRET` 값이 로그에 평문으로 출력됨
- **코드**: `console.log(\`[TCP] 인증 실패: deviceId="${deviceId}" secret="${secret}" expected="${DEVICE_SECRET}"\`)`
- **조치**: 로그에서 secret 값 제거

---

### 🟡 MEDIUM

#### 2-1. APK 업로드/배포 인증 없음
- **위치**: `index.js:945` (업로드), `index.js:1052` (배포)
- **설명**: 누구나 악성 APK를 서버에 업로드하고 전체 기기에 배포 가능
- **조치**: 두 엔드포인트 모두 `requireAuth` 추가

#### 2-2. 진단 API 공개 노출
- **위치**: `index.js:623`, `638`, `688`
- **설명**: `/api/diagnostics/sockets`, `/api/diagnostics/ota`, `/api/diagnostics/logs` 인증 없이 기기 IP, 버전, 서버 로그 조회 가능
- **조치**: `requireAuth` 추가

#### 2-3. CORS Wildcard
- **위치**: `index.js:73`
- **설명**: `origin: '*'` 설정으로 모든 도메인에서 API 호출 가능 — CSRF 공격 위험
- **조치**: 신뢰할 수 있는 도메인만 허용하도록 제한

#### 2-4. 로그인 브루트포스 무방비
- **위치**: `index.js:86`
- **설명**: 로그인 시도 횟수 제한 없음, 최소 비밀번호 4자리로 너무 짧음
- **조치**: `express-rate-limit` 적용, 최소 비밀번호 12자리 이상으로 강화

#### 2-5. 세션 메모리 저장
- **위치**: `index.js:66`
- **설명**: 세션을 메모리(Map)에만 저장하여 서버 재시작 시 모든 세션 소멸
- **조치**: Redis 또는 DB 기반 세션 저장소 사용

#### 2-6. 기기 인증 약함
- **위치**: `index.js:1492`
- **설명**: 단순 공유 비밀키(`DEVICE_SECRET`) 방식 — 키 노출 시 모든 기기 위장 가능
- **조치**: 기기별 인증서 기반 인증으로 전환

---

### 🟢 LOW

#### 3-1. 기본 DEVICE_SECRET 값 사용
- **위치**: `server/.env`
- **설명**: `DEVICE_SECRET="signagepro-secret-2026"` — 예측 가능한 기본값
- **조치**: 랜덤 생성된 강력한 값으로 교체

#### 3-2. 취약한 비밀번호 정책
- **위치**: `index.js:99`
- **설명**: 최소 4자리 비밀번호 허용
- **조치**: 최소 12자리, 영문+숫자+특수문자 조합 요구

---

## 2. 외부 DDoS 공격 대응

### 현재 위험
서버가 포트 3000으로 직접 노출되어 있을 경우 DDoS 공격에 무방비 상태.

### 권장 아키텍처
```
인터넷 → Cloudflare → iptime 공유기 → nginx (reverse proxy) → Node.js :3000
```

### 방어 조치

#### Cloudflare 무료 플랜
- DNS를 Cloudflare로 변경하면 기본 DDoS 차단, IP 숨김 효과
- "Under Attack Mode" 활성화 시 자동 봇 차단

#### nginx Rate Limiting 설정
```nginx
http {
    limit_req_zone $binary_remote_addr zone=api:10m rate=30r/m;
    limit_conn_zone $binary_remote_addr zone=conn:10m;

    server {
        listen 80;

        location /api/ {
            limit_req zone=api burst=10 nodelay;
            limit_conn conn 10;
            proxy_pass http://localhost:3000;
        }
    }
}
```

#### 방화벽 설정
- 포트 3000을 외부에서 직접 접근 불가하도록 방화벽 차단
- nginx(80/443)만 외부에 노출

#### Node.js 레벨 Rate Limiting
```javascript
const rateLimit = require('express-rate-limit');

const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15분
  max: 5,                    // 최대 5회 시도
  message: { error: '너무 많은 시도입니다. 15분 후 재시도하세요.' }
});

app.post('/api/login', loginLimiter, ...);
```

---

## 3. iptime 공유기 보안

### 주요 공격 벡터 및 방어

| 공격 방법 | 위험도 | 방어 조치 |
|---|---|---|
| 기본 admin/admin 비밀번호 | HIGH | 즉시 강력한 비밀번호로 변경 (12자 이상) |
| 원격 관리 페이지 외부 노출 | HIGH | 관리자 페이지 외부 IP 접근 비활성화 |
| 펌웨어 취약점 | HIGH | iptime 공식 사이트에서 최신 펌웨어 업데이트 |
| DNS Hijacking | HIGH | DNS를 1.1.1.1, 8.8.8.8로 고정 |
| UPnP 자동 포트 개방 | MEDIUM | UPnP 비활성화 |
| WPS 취약점 | MEDIUM | WPS 비활성화 |
| 포트포워딩 과다 개방 | MEDIUM | 필요한 포트만 최소한으로 개방 |

### iptime 보안 설정 체크리스트

- [ ] 관리자 비밀번호 변경 (기본값 admin/admin 사용 금지)
- [ ] 외부 관리 접속 비활성화 (`고급 설정 > 관리자 설정 > 원격 관리 포트` 비활성화)
- [ ] 최신 펌웨어 업데이트
- [ ] UPnP 비활성화
- [ ] WPS 비활성화
- [ ] DNS 서버 고정 (`1.1.1.1` / `8.8.8.8`)
- [ ] 불필요한 포트포워딩 규칙 제거
- [ ] DHCP 범위 최소화 (실제 기기 수에 맞게)
- [ ] 와이파이 비밀번호 WPA3 또는 WPA2 + 강력한 비밀번호

---

## 4. 즉시 조치 우선순위

| 순위 | 항목 | 예상 소요 시간 |
|---|---|---|
| 1 | `/api/debug/run-cmd` 비활성화 | 5분 |
| 2 | `.env` ADMIN_PASSWORD 강력한 값 설정 확인 | 5분 |
| 3 | APK 업로드/배포 API에 `requireAuth` 추가 | 30분 |
| 4 | 모든 관리 API에 `requireAuth` 추가 | 2시간 |
| 5 | 로그인 Rate Limiting 적용 | 30분 |
| 6 | CORS origin 제한 | 10분 |
| 7 | 로그에서 secret 값 제거 | 10분 |
| 8 | iptime 보안 설정 체크리스트 적용 | 30분 |
| 9 | Cloudflare 또는 nginx 앞단 배치 | 1시간 |

---

*이 보고서는 자체 인프라 보안 점검 목적으로 작성되었습니다.*
