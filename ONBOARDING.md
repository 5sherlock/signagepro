# 신규 루팅 STB 온보딩 — 버전·데이터 이전 체크리스트

> 새 기기를 받았을 때 순서대로 진행. dev/staging/운영 어느 환경에 붙일지 먼저 정할 것.
> 관련: DEPLOYMENT.md(환경·포트), SECURITY.md, 인수인계.md

## Phase 0 — 물리/전제 확인 (받자마자)
- [ ] **루팅 확인**: `adb shell su -c id` → `uid=0(root)` 나오는지
- [ ] **ADB 네트워크(5555)**: `adb connect <IP>:5555`. 안 되면 USB로 `adb tcpip 5555` 1회 + IP 확보
- [ ] **하드웨어 스펙**: `getprop ro.product.model`, `getprop ro.build.version.release`
  - 칩셋: RK3229(ARMv7) vs RK3566(ARMv8) → APK ABI(`armeabi-v7a`)·해상도(`MediaRenderer.maxDim`) 영향
  - Android 버전: `minSdk 22`+. **구형 5.1.1 ROM이면 APK 서명 V1 전용 필수**(V2/V3 off — 현 build.gradle 그대로면 OK)
  - RAM: 4K 영상 쓸 거면 캐시 쿼터(`MediaCacheRepo.DEFAULT_QUOTA` 2GB) 상향 검토
- [ ] **Device Owner 가능 여부**: 계정 없는 초기상태여야 `dpm set-device-owner` 가능. 계정 있으면 팩토리리셋
- [ ] **기존 런처/기본 플레이어**: 우리 앱과 포커스 다투는지 → 비활성화(`pm disable-user`)/제거 판단

## Phase 1 — APK 설치 (버전)
- [ ] **최신 APK 설치**(현재 `signagepro-0.4.31.apk`, V1 서명, armeabi-v7a): `adb install -r` 또는 `su pm install -r`
  - ⚠ 신규는 **직접 install 권장**(OTA 루프/다운그레이드 회피). OTA로 받게 하려면 그 서버 deploy-meta `apkVersion`이 목표 버전이어야 함
- [ ] **Device Owner 설정**: `adb shell dpm set-device-owner com.signagepro.player/.SignageDeviceAdmin` (자가재시작·원격제어·reboot 권한)
- [ ] 충돌 앱 정리, 우리 앱 홈/부팅 자동실행

## Phase 2 — 프로비저닝 (config 주입)
- [ ] **device_id** 고유 부여 (운영 네이밍 규칙)
- [ ] **server_url** = 소속 환경 서버 (dev `…:3001` / staging `…:3300` / 운영 `…:3300`)
  - ⚠ **하트비트 TCP포트는 server_url HTTP포트로 자동 결정**: 3000/3001→10081, 그 외→10080. 서버가 그 포트 listen하는지 확인
- [ ] **device_secret** = 서버 `DEVICE_SECRET`과 일치
- [ ] **네트워크 경로**:
  - 같은 LAN → server_url에 **LAN IP**(고정예약 권장), Tailscale 불필요
  - 원격 현장 → **Tailscale 설치+로그인**(tun0) 후 tailnet 호스트명/IP 사용
- [ ] 첫 부팅 → `register_device`/하트비트 → 서버 DB **online** 확인

> 💡 초기 프로비저닝은 앱 설정/프로비저닝 경로로. **이후 server_url 변경은 대시보드 `set_server_url` 원격 명령**(0.4.31+, root 편집 불필요).
> 단 **출발 서버에 엔드포인트가 있어야** 함 — 현재 dev/staging만, 운영 미반영(staging→main 승격 필요).

## Phase 3 — 데이터/콘텐츠 할당
- [ ] 대시보드에서 **사업장·그룹 배정** → 플레이리스트·자막 자동 동기
- [ ] 비디오월이면 자막 **deviceOrder** 배열에 추가(순서=물리 배치) → deviceIndex/totalDevices 자동
- [ ] 미디어는 그룹 배정 시 앱이 **자동 prefetch**(수동 이전 불필요, 아래 캐시 정책 참고)

## Phase 4 — 검증
- [ ] online·버전·화면 출력·HDMI
- [ ] (멀티스크린) 월 미리보기로 자막 이음새·하트비트 시각 skew 확인
- [ ] 스케줄(on/off)·볼륨·OTA 수신 점검

---

## 미디어 캐시 정책 (데이터 이전 시 이해 필요)
`MediaCacheRepo` — 앱 내부저장소 `filesDir/media_cache/`, 파일명 = **SHA-256 해시**(서버 무관, 콘텐츠 주소화).
- **서버 이전(repoint) 시 캐시 안 지움** — set_server_url 경로에 캐시 clear 없음.
- 새 서버가 **같은 미디어(같은 hash)** 서빙 → **재다운로드 없이 재사용**. 다른 미디어면 새로 받고, 옛 파일은 비활성으로 남음.
- **쿼터 2GB 초과 시 trim**: 현재 playlist에 없는(비활성) 파일을 **오래된 것부터** 삭제, 그래도 넘으면 활성 파일도 삭제(최후). → "이전 시 삭제"가 아니라 **"가득 차면 LRU 제거"**.
- 따라서 dev↔staging 왕복해도 **쿼터에 안 밀린 파일은 그대로 남아 복귀 시 재사용**. 밀려서 지워졌으면 복귀 시 재다운로드.
- 캐시는 앱 재시작/kill로 안 지워짐(filesDir). 삭제는 trim/언인스톨/수동뿐. (prefs clear는 캐시와 무관)

## 환경 이전 (기존 기기 dev↔staging↔운영)
- **버전**: OTA(deploy-meta target+push→Watchdog 5분 캐스케이드) 또는 ADB install. semver라 목표<현재면 안 내려감(루프 안전).
- **서버 소속 이전**: 대시보드 `set_server_url`(엔드포인트 있는 환경끼리). device_id 동일하면 도착 서버 DB가 알아보고 그룹배정 유지.
- ⚠ 출발 서버 deploy-meta `apkVersion`이 fleet보다 구버전이면 다운그레이드 위험 → 운영/staging은 `apkVersion=null` 유지 권장.
