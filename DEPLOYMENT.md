# SignagePro 배포 토폴로지 & 파이프라인

> 최종 확인: 2026-06-13 (PM2 + netstat + git 브랜치 실측 기준)
> 이 문서는 "어디서 무엇이 어떤 포트로 도는지"와 dev → staging → 운영 흐름의 **정본(source of truth)** 입니다.
> 포트/이름이 헷갈리면 추측하지 말고 이 표를 기준으로 하세요.

---

## 1. 환경 구성 (실측)

이 PC는 **로컬 개발 PC**이며, 그 위에 dev와 staging 두 백엔드가 별도 폴더/별도 git 체크아웃으로 돕니다. 운영(production)은 **별도 원격 PC**(AnyDesk 접속)이며 이 PC에 없습니다.

| 환경 | 폴더 | git 브랜치 | PM2 이름 | HTTP 포트 | TCP 하트비트 | 비고 |
|------|------|-----------|----------|-----------|-------------|------|
| **샌드박스/dev** | `D:\WorkSpace\signagepro` | `dev` | `signagepro` | **3001** | 10081 | 개발 작업 트리 (이 워크스페이스) |
| **스테이징** | `D:\signagepro` | `staging` | `signagepro-staging` | **3300** | 10082 | 원격 운영과 동일 코드 최종 테스트 |
| **운영(원격)** | (원격 PC) | `main` 받음 | — | **3300** (Cloudflare 터널) | (원격) | AnyDesk 접속해 수동 빌드·배포. `원격지IP:3300`을 signage-pro.com으로 터널링 |

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
5174  모바일 Vite (프론트 dev)
10081 dev    Android TCP 하트비트
10082 staging Android TCP 하트비트
10080 (코드 기본값) — 운영/미설정 시 사용
```

---

## 1.5 환경 패리티 — staging = 운영 "리허설"

**staging은 원격 운영과 거의 동일한 환경에서 모의 테스트하는 곳**이다. 따라서 staging은 dev가 아니라 **운영의 구성을 그대로 따라가야** 한다.

| 항목 | dev (3001) | staging (3300) | 운영 (원격 3300) |
|------|-----------|----------------|------------------|
| 대시보드 서빙 | 별도 Vite(3000) 핫리로드 | **빌드본을 노드가 단일 서빙** | 빌드본을 노드가 단일 서빙 |
| 서빙 코드 근거 | — | [index.js:1923-1926](server/index.js#L1923-L1926) `../dashboard/dist` | 동일 |
| 미디어 스토리지 | R2 (`.env`에 키 있음) | ⚠️ **로컬 디스크 (R2 키 없음)** | R2 |
| 외부 노출 | 로컬만 | 로컬만 | Cloudflare 터널 공개 |

- ✅ **서빙 방식은 일치** — staging/운영 모두 노드 서버 하나가 `dashboard/dist`(Vite build)와 API를 함께 서빙. dev의 3000/5174 Vite는 개발 편의용이라 운영엔 없음.
- ⚠️ **미디어 경로가 불일치** — staging `.env`에 R2 키가 없어 업로드가 로컬 디스크로 감. "운영에선 R2, staging에선 로컬"이라 모의 테스트 충실도가 떨어짐. → staging `.env`에 운영과 동일한(또는 staging 전용 버킷) R2 설정을 추가해야 진짜 리허설이 됨.
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
 ⑤ 원격 운영 배포      (원격 PC, AnyDesk 접속)
           git pull origin/main → npm install → 빌드 → PM2 재시작
           운영 API 3300 → Cloudflare 터널(원격지IP:3300) → https://signage-pro.com
```

### 핵심 원칙

- **dev와 staging은 폴더가 분리되어야 한다.** git 워킹트리는 한 번에 한 브랜치만 보므로, 두 환경을 같은 폴더에 두면 `dev`와 `staging`을 동시에 띄울 수 없다. → 현재처럼 `D:\WorkSpace\signagepro`(dev) / `D:\signagepro`(staging) 분리가 정답.
- **운영 서버는 절대 자동 배포하지 않는다.** 항상 AnyDesk로 사람이 직접 `git pull origin/main` 후 빌드·재시작. (메모리 규칙: 운영 자율 배포 금지)
- **승격은 브랜치 머지로 한다.** `dev` → `staging` → `main`. 원격 운영은 항상 **`main`** 을 pull (staging 아님).

---

## 3. 알려진 불일치 / TODO (배포 측)

> 보안 항목은 [SECURITY.md](SECURITY.md) 참조. 아래는 배포·운영 흐름의 정합성 문제.

1. **`deploy_server.ps1`가 현재 파이프라인과 불일치** — [deploy_server.ps1](deploy_server.ps1)
   - `staging`이 아니라 `release/0.4.17` 브랜치를 pull한다.
   - 대상 경로가 원격이 아니라 **dev 폴더(`D:\WorkSpace\signagepro`)** 다 → 이 PC에서 돌리면 운영이 아니라 dev를 덮어쓴다.
   - `prisma db push --accept-data-loss` 사용 → **운영에서 실행 시 스키마 드리프트로 DB 파손 위험.** (백업은 선행하지만 위험)
   - PM2 등록명을 `signagepro-server`로 하는데 실제 도는 이름은 `signagepro` / `signagepro-staging`.
   - → 스크립트를 staging 기준 + 안전한 마이그레이션(`prisma migrate deploy`)으로 재작성 필요.

2. **`dist/` 빌드 산출물이 git에 추적됨** — 두 체크아웃 모두 `dashboard/dist/index.html`이 항상 dirty. 빌드할 때마다 더러워져 `git reset --hard` / `git pull`에서 충돌·덮어쓰기 발생. → `dashboard/dist/`를 `.gitignore`에 추가하고 배포 시 빌드 산출하도록.

3. **환경별 설정 분리 미흡** — dev와 staging의 `.env`가 비밀값(adminPassword, DEVICE_SECRET)을 공유하고, R2 키는 dev에만 있어 staging은 운영을 충실히 흉내내지 못함. (상세·조치는 [SECURITY.md](SECURITY.md))

4. **운영 배포 스크립트 부재** — 4단계(원격 운영 배포)에 맞는 검증된 스크립트가 없다. 현재는 수동. → staging 전용 안전 배포 스크립트 정비 필요.

---

## 4. 자주 쓰는 확인 명령

```powershell
pm2 list                         # 도는 프로세스/상태
pm2 logs signagepro --lines 50   # dev 로그
pm2 logs signagepro-staging      # staging 로그
netstat -ano -p tcp | findstr "3001 3300 10081 10082"   # 실제 리스닝 포트
git -C D:\WorkSpace\signagepro rev-parse --abbrev-ref HEAD   # dev 체크아웃 브랜치
git -C D:\signagepro          rev-parse --abbrev-ref HEAD   # staging 체크아웃 브랜치
```
