# SignagePro 배포 토폴로지 & 파이프라인

> 최종 갱신: 2026-06-16 (인프라 결정 2026-06-14 반영 — Cloudflare·R2 폐기, Tailscale 단일망, 현장 NAS 자가호스팅)
> 이 문서는 "어디서 무엇이 어떤 포트로 도는지"와 dev → staging → 운영 흐름의 **정본(source of truth)** 입니다.
> 포트/이름이 헷갈리면 추측하지 말고 이 표를 기준으로 하세요. 토폴로지 그림: [topology.svg](topology.svg)

---

## 0. 인프라 방향 (2026-06-14 결정)

기존 **Cloudflare 터널(공개 도메인) + R2(클라우드 스토리지)** 구성을 **보안상 폐기**하고, **Tailscale 단일 가상망 + 현장 NAS 자가호스팅**으로 전환한다.

- ❌ **Cloudflare 폐기** — 터널은 이미 차단(Tailscale로 전환). 공개 도메인(signage-pro.com) 노출 제거. 회원탈퇴는 도메인 정리 후 (무료라 급하지 않음).
- ❌ **R2 폐기** — 전 환경(dev/staging/현장 22개) 모두 R2 미사용 확정. 미디어/DB는 전부 **로컬(NAS) 자가호스팅**. R2 키 폐기 + 버킷(signagepro-media) 삭제 가능.
- ✅ **Tailscale 단일망** — 서버·관리자/개발자 PC·STB가 하나의 tailnet에 묶임. 외부 공개 없이 tailnet 내부에서만 접속.
- ✅ **현장 NAS = 운영 서버 + 저장** — Synology **DS225+**(Docker)에서 Node 서버 + 미디어(uploads) + DB(SQLite) 자가호스팅. 멀티사이트 확장 시 현장별 최소형 NAS(로컬 캐시) 배치.

> ⚠️ **과도기 단서**: 현재 현장 STB가 Android 5.1.1(API 22)이라 Tailscale 앱 미지원 → 임시로 **ZeroTier** 사용. **4K STB(큐버) 전환 후 Tailscale로 일원화**가 목표. (상세 [TAILSCALE_SETUP.md](TAILSCALE_SETUP.md))

---

## 1. 환경 구성 (실측)

이 PC는 **로컬 개발 PC**이며, 그 위에 dev와 staging 두 백엔드가 별도 폴더/별도 git 체크아웃으로 돕니다. 운영(production)은 **현장 NAS**(Tailscale 접속)이며 이 PC에 없습니다.

| 환경 | 위치 | git 브랜치 | 런타임 | HTTP 포트 | TCP 하트비트 | 비고 |
|------|------|-----------|--------|-----------|-------------|------|
| **샌드박스/dev** | `D:\WorkSpace\signagepro` | `dev` | PM2 `signagepro` | **3001** | 10081 | 개발 작업 트리 (이 워크스페이스) |
| **스테이징** | `D:\signagepro` | `staging` | PM2 `signagepro-staging` | **3300** | 10082 | 운영과 동일 코드 최종 테스트 |
| **운영(현장 NAS)** | 현장 NAS (`jeju-osulloc`) | `main` 받음 | **Docker** | **3300** | (현장) | Tailscale 접속해 수동 빌드·배포. tailnet 내부 공개(`jeju-osulloc:3300`), 외부 공개 없음 |

### 프론트엔드 dev 서버 (Vite — API 아님)

| 대상 | PM2 이름 | 포트 |
|------|----------|------|
| 대시보드 | `dashboard` | **3000** |
| 모바일 대시보드 | `signagepro-mobile` | 5174 |

> ⚠️ **3000번은 대시보드 Vite가 쓰는 포트입니다.** dev API(`signagepro`)는 3000이 아니라 **3001**입니다. (과거 문서/메모리의 "dev=3000"은 오류였음 — 3000은 프론트 dev 서버)

### 포트 한눈에

```
3000  대시보드 Vite (프론트 dev)
3001  dev 백엔드 API  (signagepro,         D:\WorkSpace\signagepro, dev 브랜치)
3300  staging 백엔드 API (signagepro-staging, D:\signagepro,           staging 브랜치)
3300  운영 API (현장 NAS Docker, main 브랜치, tailnet jeju-osulloc:3300)
5174  모바일 Vite (프론트 dev)
10081 dev    Android TCP 하트비트
10082 staging Android TCP 하트비트
10080 (코드 기본값) — 운영/미설정 시 사용
```

---

## 1.5 환경 패리티 — staging = 운영 "리허설"

**staging은 현장 NAS 운영과 거의 동일한 환경에서 모의 테스트하는 곳**이다. 따라서 staging은 dev가 아니라 **운영의 구성을 그대로 따라가야** 한다.

| 항목 | dev (3001) | staging (3300) | 운영 (현장 NAS 3300) |
|------|-----------|----------------|----------------------|
| 대시보드 서빙 | 별도 Vite(3000) 핫리로드 | **빌드본을 노드가 단일 서빙** | 빌드본을 노드가 단일 서빙 (Docker) |
| 서빙 코드 근거 | — | [index.js:1923-1926](server/index.js#L1923-L1926) `../dashboard/dist` | 동일 |
| 미디어 스토리지 | 로컬 `uploads` | 로컬 `uploads` | 로컬 `uploads` (NAS) |
| 외부 노출 | 로컬만 | 로컬만 | Tailscale tailnet 내부만 (공개 X) |

- ✅ **서빙 방식 일치** — staging/운영 모두 노드 서버 하나가 `dashboard/dist`(Vite build)와 API를 함께 서빙. dev의 3000/5174 Vite는 개발 편의용이라 운영엔 없음.
- ✅ **미디어 경로 일치(R2 폐기 후)** — 전 환경이 로컬 `uploads`를 쓰므로 과거의 "운영 R2 vs staging 로컬" 패리티 갭은 해소됨.
- 패리티를 맞출 때 staging이 빌드본을 서빙하므로 **테스트 전 반드시 `dashboard` 재빌드**가 선행되어야 최신 코드가 반영된다.

---

## 2. 배포 파이프라인 (dev → staging → 운영)

```
 ① 개발/코딩          D:\WorkSpace\signagepro (dev 브랜치, API 3001)
        │  git commit/push → origin/dev
        ▼
 ② dev → staging 승격  git merge dev → staging, push origin/staging
        │
        ▼
 ③ 스테이징 최종 테스트 D:\signagepro (staging 브랜치, API 3300)
        │  git pull origin/staging → 서버·대시보드 빌드 → 실기기 테스트
        ▼
 ④ staging → main 승격 git merge staging → main, push origin/main
        │
        ▼
 ⑤ 현장 NAS 운영 배포   (현장 NAS, Tailscale 접속)
           git pull origin/main → 빌드 → Docker 컨테이너 재시작
           운영 API 3300 → tailnet 내부 공개 (jeju-osulloc:3300), 외부 공개 없음
```

### 핵심 원칙

- **dev와 staging은 폴더가 분리되어야 한다.** git 워킹트리는 한 번에 한 브랜치만 보므로, 두 환경을 같은 폴더에 두면 `dev`와 `staging`을 동시에 띄울 수 없다. → 현재처럼 `D:\WorkSpace\signagepro`(dev) / `D:\signagepro`(staging) 분리가 정답.
- **운영(현장 NAS)은 절대 자동 배포하지 않는다.** 항상 Tailscale로 사람이 직접 `git pull origin/main` 후 빌드·컨테이너 재시작. (메모리 규칙: 운영 자율 배포 금지)
- **승격은 브랜치 머지로 한다.** `dev` → `staging` → `main`. 현장 NAS 운영은 항상 **`main`** 을 pull (staging 아님).

---

## 3. 백업 (3-2-1)

> 상세는 [BACKUP_PLAN.md](BACKUP_PLAN.md). 운영 데이터(코드 제외)는 **3벌 · 매체 2종 · 오프사이트 1벌**.

백업 대상(git에 없음 → 반드시 별도 백업): **DB(`dev.db`) · `.env`(시크릿) · keystore(대체불가) · `uploads`(미디어)**

```
 ① 원본 — 현장 NAS (살아있는 운영 데이터)
        │  일일 자동 (SQLite .backup + robocopy /MIR + 회전)
        ▼
 ② 현장 PC (로컬 백업)  ── 단일 드라이브/기기 고장 방어
        │  tailnet 미러 (DB/.env/keystore 매일, uploads 주간)
        ▼
 ③ 개발자 PC (오프사이트) ── 화재·도난·랜섬웨어 대비 (가장 중요)
        +
 ④ USB (콜드) ── keystore 등 대체불가 항목 다중·암호화 보관
```

주의: 같은 드라이브에만 백업 금지 · 시크릿/keystore 외부 보관 시 암호화 필수 · SQLite는 `.backup`(라이브 파일 단순복사 금지) · 복구 테스트 분기 1회.

---

## 4. 알려진 불일치 / TODO (배포 측)

> 보안 항목은 [SECURITY.md](SECURITY.md) 참조 (⚠️ SECURITY.md는 아직 일부 Cloudflare 기준 — 갱신 필요). 아래는 배포·운영 흐름의 정합성 문제.

1. **현장 NAS Docker 운영 배포 스크립트 부재** — ⑤단계(NAS 배포)에 맞는 검증된 스크립트가 없다. 현재는 수동. → NAS Docker 기준 안전 배포 절차/스크립트 정비 필요.

2. **`deploy_server.ps1`가 현재 파이프라인과 불일치** — [deploy_server.ps1](deploy_server.ps1)
   - `staging`이 아니라 `release/0.4.17` 브랜치를 pull한다.
   - 대상 경로가 NAS가 아니라 **dev 폴더(`D:\WorkSpace\signagepro`)** 다 → 이 PC에서 돌리면 운영이 아니라 dev를 덮어쓴다.
   - `prisma db push --accept-data-loss` 사용 → **운영에서 실행 시 스키마 드리프트로 DB 파손 위험.**
   - PM2 등록명을 `signagepro-server`로 하는데 실제 도는 이름은 `signagepro` / `signagepro-staging`(운영은 Docker).
   - → 스크립트를 staging/NAS 기준 + 안전한 마이그레이션(`prisma migrate deploy`)으로 재작성 필요.

3. **`dist/` 빌드 산출물이 git에 추적됨** — 두 체크아웃 모두 `dashboard/dist/index.html`이 항상 dirty. 빌드할 때마다 더러워져 `git reset --hard` / `git pull`에서 충돌·덮어쓰기 발생. → `dashboard/dist/`를 `.gitignore`에 추가하고 배포 시 빌드 산출하도록.

4. **환경별 설정 분리 미흡** — dev와 staging의 `.env`가 비밀값(adminPassword, DEVICE_SECRET)을 공유. (상세·조치는 [SECURITY.md](SECURITY.md))

5. **종속 리스크** — 소스코드·Tailscale 계정이 개인 소유 → 회사 이전 필요. (상세 [인수인계.md](인수인계.md))

---

## 5. 자주 쓰는 확인 명령

```powershell
pm2 list                         # dev/staging 프로세스/상태 (로컬 PC)
pm2 logs signagepro --lines 50   # dev 로그
pm2 logs signagepro-staging      # staging 로그
netstat -ano -p tcp | findstr "3001 3300 10081 10082"   # 실제 리스닝 포트
git -C D:\WorkSpace\signagepro rev-parse --abbrev-ref HEAD   # dev 체크아웃 브랜치
git -C D:\signagepro          rev-parse --abbrev-ref HEAD   # staging 체크아웃 브랜치
tailscale status                 # tailnet 노드/IP 확인 (NAS jeju-osulloc 포함)
```

---

## 6. 담당자 퇴사/교체 시 인수인계 절차

> 자산 위치·계정·인벤토리 상세는 [인수인계.md](인수인계.md). 여기서는 **무엇을 어떤 순서로** 넘기는지(절차)를 정의한다.
> 이 시스템의 최대 리스크는 **소스코드·서명키·Tailscale·도메인이 담당자 개인 자산에 종속**된 점이다. 인계의 본질 = **개인 종속 해소 + 후임 단독 운영 검증.**

```
 ① 동결/실측  →  ② 회사 자산화  →  ③ 비밀값 안전 전달  →  ④ 접근권 인계
                                                              │
 ⑧ 문서 최종화 ← ⑦ 퇴사자 권한 회수 ← ⑥ 인수 검증(단독 실행) ← ⑤ 빌드환경 재현
```

### ① 현황 동결 & 실측
- 운영을 멈추는 변경(배포·스키마 변경)을 일시 동결. [인수인계.md](인수인계.md) §3 인벤토리의 빈칸(IP·시리얼·계정주·매장별 STB)을 **실측값으로 전부 기입**.
- `tailscale status` / `pm2 list` / `netstat` 로 현재 도는 노드·포트·브랜치 스냅샷을 문서에 남긴다.

### ② 개인 종속 해소 (회사 자산화) — 가장 중요
- [ ] **소스코드**: GitHub `5sherlock/signagepro`(개인) → **회사 org로 이전** 또는 후임/회사 계정에 owner 권한 부여.
- [ ] **Tailscale tailnet**: 소유권을 **개인 이메일 → 회사/공용 계정**으로 이전. (이전 전엔 퇴사 시 전 노드 접속 끊김 위험)
- [ ] **서명키(keystore) + 키 암호**: 회사 보관소 + USB(암호화) 다중 보관. **대체 불가 — 분실 시 기존 앱 OTA 업데이트 영구 불가.**
- [ ] **도메인**(signage-pro.com) / **Cloudflare 잔여 계정**: 유지 시 회사 명의 이전, 폐기 시 정리(현재 Tailscale 전환으로 공개 도메인 미사용).

### ③ 비밀값 안전 전달 (평문 금지)
- DEVICE_SECRET · adminPassword · keystore 암호 · NAS(DSM)/AnyDesk/Tailscale 계정 → **비밀번호 관리자(1Password/Bitwarden) 공유 또는 봉인 문서**로 전달. 문서/깃에 평문 금지.
- 환경 간 공유·약한 시크릿(`signagepro-secret-2026` 등)은 **인계 전 환경별 강한 값으로 교체** ([SECURITY.md](SECURITY.md)).

### ④ 접근권 인계
- AnyDesk(현장) · Tailscale 초대 · NAS DSM 관리자 · 대시보드 admin · STB 원격(ADB `<tailscale-ip>:5555`)을 후임 계정으로 부여하고 **후임이 직접 접속 성공**까지 확인.

### ⑤ 빌드환경 재현
- 후임 PC에 저장소 클론 + Android SDK/Gradle(버전 명시) + keystore 배치 + `.env` 세팅 → **테스트 APK 1회 빌드 성공**.

### ⑥ 인수 검증 (Acceptance — 후임이 단독으로 1회씩 수행)
- [ ] **배포 파이프라인**: dev → staging 머지 → 빌드·실기기 테스트 → main 승격 → **현장 NAS(Docker) 배포** 1회 완주.
- [ ] **OTA**: APK 빌드 → 대시보드 업로드 → STB 1대 OTA 푸시 성공.
- [ ] **백업 복구**: 백업본 DB로 서버 기동되는지 복구 테스트 1회 ([BACKUP_PLAN.md](BACKUP_PLAN.md)).
- → 세 가지를 후임이 **혼자** 성공해야 인계 완료로 본다.

### ⑦ 퇴사자 권한 회수
- 퇴사자의 GitHub 접근·Tailscale 노드/계정·AnyDesk·NAS·서버 계정 제거, ②③에서 옮긴 후 **공유했던 비밀값 재교체**(이전 담당자가 알던 값 무효화).

### ⑧ 문서 최종화
- [인수인계.md](인수인계.md) §3 인벤토리·§11 연락처·§12 TODO를 최신 상태로 갱신, 본 절차의 체크박스 결과를 기록.
