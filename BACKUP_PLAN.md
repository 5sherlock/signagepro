# SignagePro 백업 계획 (현장 서버)

> 작성: 2026-06-13. 대상: 현장/운영 서버 **jeju-osulloc** (Windows, `C:\signagepro`, C:/D: 드라이브 분리).
> 원칙: **3-2-1** — 데이터 3벌 · 매체 2종 · **오프사이트 1벌**. (C:만/D:만으론 부족 — 매장 화재·도난·랜섬웨어 대비 오프사이트 필수)

---

## 1. 무엇을 백업하나 (우선순위)

| 대상 | 경로(현장 서버) | 크기 | 중요도 | 비고 |
|---|---|---|---|---|
| **DB** | `C:\signagepro\server\prisma\dev.db` | 작음 | ★★★ | 기기·플레이리스트·스토어·미디어 메타. 최우선 |
| **`.env`** | `C:\signagepro\server\.env` | 작음 | ★★★ | 시크릿(DEVICE_SECRET·adminPassword·R2키). **암호화 보관** |
| **서명키** | `signagepro.keystore` (android/app) | 작음 | ★★★ | **대체 불가** — 분실 시 기존 앱 위 업데이트 서명 불일치로 설치 불가 |
| **미디어** | `C:\signagepro\server\uploads\` | 큼 | ★★ | 실제 미디어 파일 |
| (OTA APK) | `server\update\app.apk` | 중간 | ★ | 재빌드 가능 → 선택 |

> 코드는 git에 있으니 백업 제외 OK. **위 4종(DB·.env·keystore·uploads)은 git에 없음 → 반드시 별도 백업.**

---

## 2. Layer 1 — 로컬 C: → D: 자동 백업 (드라이브 분리 활용)

목적: **단일 드라이브 고장** 방어 + 빠른 복구.
- 데이터 `C:\signagepro` → 백업 `D:\backups`
- **Windows Task Scheduler**로 매일 새벽 1회 실행.
- 핵심 규칙:
  - **DB는 단순 복사 금지 → SQLite 온라인 백업** 사용(WAL 미반영/손상 방지):
    `sqlite3 "C:\signagepro\server\prisma\dev.db" ".backup D:\backups\db\dev_yyyymmdd.db"`
    (sqlite3.exe 없으면 설치, 또는 서버 잠깐 멈추고 dev.db + dev.db-wal + dev.db-shm 함께 복사)
  - uploads: `robocopy "C:\signagepro\server\uploads" "D:\backups\uploads" /MIR /Z /R:2 /W:5`
  - `.env`, `keystore` 복사
  - **회전**: 일일 14개 유지, 오래된 것 삭제
  - 로그 파일 남기기

---

## 3. Layer 2 — 오프사이트 (가장 중요)

매장 화재·도난·랜섬웨어면 **C:·D: 둘 다 사라짐.** 작고 치명적인 것(DB·.env·keystore)은 **반드시 매장 밖에.**

선택지 (택1 또는 병행):
- **A. Tailscale로 NAS/원격 서버 미러** — `D:\backups`를 tailnet의 **항상 켜진 NAS**(권장)에 `robocopy` 동기화. (dev PC는 항상 켜진 게 아니니 NAS가 이상적)
- **B. 클라우드(restic / Duplicati → Backblaze B2 등)** — **암호화·버전·증분** 자동. 시크릿 백업엔 이게 안전(기본 암호화).

권장 빈도:
- DB·.env·keystore (작음): **매일** 오프사이트
- uploads (큼): **주간** 오프사이트

---

## 4. 주의사항 (실수 방지)

1. **같은 드라이브에만 백업 금지** — C: 데이터 + C: 백업 = C: 고장 시 무용. 반드시 D: + 오프사이트.
2. **SQLite는 `.backup`(또는 WAL 체크포인트) 사용** — 라이브 파일 단순 복사는 손상 위험.
3. **시크릿 백업 암호화** — `.env`·`keystore`를 클라우드/외부에 **평문 금지**. restic/Duplicati 암호화 사용.
4. **keystore는 다중·오프사이트·암호화** 보관 — 잃으면 복구 불가.
5. **복구 테스트 주기적** — 복원 안 해본 백업은 백업이 아님. 분기 1회라도 테스트 복원.
6. 백업 대상 서버는 **현장 서버(jeju-osulloc)** — AnyDesk 또는 Tailscale ADB/원격으로 접근해 설정.

---

## 5. 추천 조합 (이 환경)

- **로컬**: C:→D: **일일 자동** (sqlite `.backup` + robocopy `/MIR` + 14일 회전 + 로그) — Task Scheduler.
- **오프사이트**: DB·.env·keystore **매일**, uploads **주간** → **restic → Backblaze B2**(암호화) 또는 **Tailscale로 NAS 미러**.
- 보존: 일일 14 + 주간 8 회전.

---

## 6. 내일 할 일 (체크리스트)

- [ ] 현장 서버에 `sqlite3.exe` 있는지 확인(없으면 설치 or 대체 복사 방식)
- [ ] `D:\backups\{db,uploads}` 폴더 생성
- [ ] 백업 PowerShell 스크립트 작성 (sqlite `.backup` + robocopy + 회전 + 로그) — **dev에서 작성 후 AnyDesk로 현장 서버에 배치**
- [ ] Task Scheduler 등록 (매일 새벽, 최고 권한)
- [ ] 1회 수동 실행 → `D:\backups` 결과 확인
- [ ] 오프사이트 선택: NAS(Tailscale) vs restic/Duplicati→B2 → 설치·설정
- [ ] **복구 테스트** 1회 (백업 DB로 서버 기동되는지)
- [ ] keystore 오프사이트 암호화 보관 확인

> 스크립트(현장 서버용 backup.ps1)는 내일 dev에서 작성 예정. 작성되면 `signagepro/` 또는 `server/scripts/`에 두고 AnyDesk로 현장 서버에 복사 → Task Scheduler 등록.

---

## 참고 — 관련 문서
- 인프라/접속: Tailscale(`jeju-osulloc:3300`, MagicDNS), Cloudflare 차단됨 → [SECURITY.md](SECURITY.md)
- 배포 토폴로지 → [DEPLOYMENT.md](DEPLOYMENT.md)
- 미디어 저장 방식(로컬 `/uploads` vs R2): 현재 R2 사용 여부는 현장 서버 `.env`로 확인. private 지향이면 로컬+Tailscale 권장.
