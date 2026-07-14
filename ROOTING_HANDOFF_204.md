# 셋톱박스(dev-204) 루팅 작업 핸드오프

> 작성: 2026-06-13. **갱신: 2026-07-02 — ✅ 루팅 완료(작업 종료). 아래는 완료 기록 + 향후 신규 RK356x 보드 루팅 참고용.**
> 작업 환경: Windows + PowerShell/Git Bash.

---

## 0. 한 줄 요약 (2026-07-02 실측)
**dev-204 루팅 완료.** 목표였던 "신규 기기와 동일한 루팅 개발 환경"이 갖춰졌습니다.

- **기기 확정**: Ultracube **U4X CM V2**, SoC **RK356x(RK3566)**, Android 11.
- **root 작동 확인**: `/system/bin/su` 존재, `su -c id` → `uid=0(root)`. (`ro.debuggable=1`)
- **영구 ADB TCP 5555 활성** (재부팅 유지). SignagePro **1.0.7**(versionCode 10007) 설치.
- **Tailscale 노드**: `100.75.80.108` (`com.tailscale.ipn`). 접속: `adb connect 100.75.80.108:5555`.
- 2026-06-13의 "USB 미인식으로 막힘"은 **해소됨**(현재 네트워크 ADB로 접속). 원래 LAN IP `172.30.1.19` → 지금은 Tailscale로 접속.

> 이후 이 문서는 **완료 기록**이자, **새 RK356x 보드가 비루팅으로 입고될 경우의 루팅 절차 참고서**로 사용하세요. (신규 기기는 가급적 루팅 상태로 구매)

---

## 1. 루팅 목적 (왜 하는가)
1. **개발 환경 일치**: 앞으로 구매할 사이니지 셋톱박스를 **루팅된 상태로 구매**할 예정. 그래서 현재 dev 기기(204)도 루팅해서 **동일 환경에서 개발/테스트**하려는 것. (가장 중요한 이유)
2. **영구 원격 ADB**: 현장 기기는 OTA가 막히면 ADB로 복구해야 하는데, 원격 ADB가 되려면 **재부팅 후에도 ADB TCP(5555)가 유지**돼야 함. 이건 `setprop service.adb.tcp.port 5555` 를 부팅마다 적용해야 하고 → **root(su) 필요**.
   - 앱이 이미 부팅 시 자동으로 이걸 시도함:
     - `android/app/src/main/java/com/signagepro/player/BootReceiver.kt:55` → `setprop service.adb.tcp.port 5555 && stop adbd && start adbd`
     - `android/app/src/main/java/com/signagepro/player/PlayerApp.kt:65` 에도 동일
   - 즉 **기기가 root면 앱이 알아서 영구 ADB를 켜줌.** 204가 비루팅이라 이게 실패 중.

> 참고(비루팅 대안): Android 11 "무선 디버깅"으로도 영구 네트워크 ADB가 가능하지만, 사용자는 **신규 fleet이 루팅 상태라 일부러 루팅 환경을 원함** → 무선 디버깅은 채택 안 함. 루팅으로 진행.

---

## 2. 대상 기기 현황
| 기기 | OS | SoC | 접속 | 상태 | 결과 |
|------|----|----|------|------|------|
| dev-201 | Android 5.1.1 | RK3229 | — | — | 5.1.1 구형(비루팅 대안: 무선디버깅/ZeroTier) |
| dev-202 | Android 5.1.1 | RK3229 | — | — | 동일 |
| dev-203 | Android 5.1.1 | RK3229 | — | — | 동일 |
| **dev-204** | **Android 11** | **RK356x (Ultracube U4X CM V2)** | **Tailscale `100.75.80.108:5555`** | 온라인 | **✅ 루팅 완료 · ADB TCP 상시 · 1.0.7 · Tailscale** |

- 2026-07-02 실측: `adb connect 100.75.80.108:5555` → `device product:rk356x_box model:Ultracube_U4X_CM_V2`.
- root 확인: `su -c id` → `uid=0(root)`. `service.adb.tcp.port=5555`, `ro.debuggable=1`, `release-keys`.

---

## 3. 진행 경과 (해결됨)
- **2026-06-13 막힘**: USB 미인식(TV박스 USB-A가 호스트 포트) → `adb devices` 에 안 잡힘. → **해소됨**(네트워크/Tailscale ADB로 접속 확립).
- **루팅 달성**: `/system/bin/su` 존재 + `su -c id` uid=0. 4번 판정 기준의 "이미 root"에 해당 → 5번 플래싱 불필요.
- **6번(영구 ADB·앱·Tailscale) 전부 완료** 상태(0.5절 요약 참고).

**사용 ADB 경로**: `D:\WorkSpace\signagepro\server\bin\adb.exe` (버전 1.0.41). 또는 시스템 `adb`.

---

## 4. 루팅 전 진단 절차 (USB 인식되면 가장 먼저)
PowerShell 또는 Git Bash에서:
```powershell
$adb = "D:\WorkSpace\signagepro\server\bin\adb.exe"
& $adb kill-server; & $adb start-server
& $adb devices -l            # 204가 device/unauthorized 로 뜨는지
```
- `unauthorized` 로 뜨면 → 204 화면에서 **USB 디버깅 허용 RSA 프롬프트 수락** 필요(UI 접근 필요).
- `device` 로 뜨면 진단 진행:
```powershell
# 기기 식별
& $adb shell "getprop ro.product.model; getprop ro.product.manufacturer; getprop ro.board.platform; getprop ro.build.version.release; getprop ro.build.fingerprint; getprop ro.build.tags; getprop ro.secure; getprop ro.debuggable; getprop ro.boot.flash.locked; getprop service.adb.tcp.port"
# 현재 셸 권한
& $adb shell id
# su(루팅) 존재 여부 — 비차단
& $adb shell "command -v su; ls -l /system/xbin/su /system/bin/su /sbin/su 2>/dev/null"
# Magisk 등 설치 여부
& $adb shell "pm list packages | Select-String -Pattern 'magisk|supersu|kingroot|superuser'"
# adb 자체를 root로 재시작 가능한지(엔지니어링 빌드면 됨)
& $adb root
& $adb shell id     # uid=0(root) 나오면 성공
```

### 판정 기준
- **이미 root**(su 존재) 또는 **`adb root` 성공** → **플래싱(루팅) 불필요!** 바로 6번(영구 ADB 설정)으로.
- `ro.build.tags=test-keys` / `ro.debuggable=1` / `ro.secure=0` → 루팅/권한 획득이 쉬운 엔지니어링 빌드 가능성 높음.
- 그 외(release-keys, su 없음, adb root 실패) → **5번 정식 루팅 절차** 필요.

---

## 5. 루팅 방법 (정식 — 비루팅 확정 시)
> ⚠️ 기기(SoC)마다 완전히 다름. **반드시 4번에서 SoC를 먼저 확인.** 잘못된 이미지 = 벽돌(brick). 원격 불가, 물리 USB 필수.

### 5-1. 공통 흐름 (Magisk 방식)
1. **SoC/모델 확정** (`ro.board.platform`, `ro.product.model`).
2. 그 기기의 **stock 펌웨어에서 `boot.img` 확보** (제조사/포럼/벤더 자료).
3. 기기에 **Magisk 앱 설치** → `boot.img`를 Magisk로 패치 → `magisk_patched.img` 생성.
4. **부트로더 해제 + 플래싱**:
   - fastboot 지원 기기: `fastboot oem unlock` → `fastboot flash boot magisk_patched.img`
   - **Rockchip**: 부트로더/fastboot 대신 **RKDevTool** + 로더로 boot 파티션 굽기.
   - **Amlogic**: **Amlogic USB Burning Tool**로 패치된 이미지 굽기.
5. 재부팅 → Magisk 앱에서 su 정상 동작 확인.

### 5-2. 더 쉬운 대안
- 그 보드용 **pre-rooted 펌웨어(이미 su 포함)** 가 있으면 그걸 굽는 게 가장 안전/간단.
- ❌ **원클릭 루팅 앱(KingoRoot 등) 금지** — Android 11에서 거의 안 되고 멀웨어 위험.

### 5-3. 주의
- 부트로더 해제 시 **데이터 초기화**될 수 있음 → 설치 후 SignagePro 앱 재설치 + 서버주소 재설정 필요.
- 루팅 후 보안 약화 → 반드시 **VPN(아래) 안에서만** ADB 노출, 인터넷 직접 노출 금지.

---

## 6. 루팅 성공(또는 이미 root) 후 할 일  — ✅ dev-204는 전부 완료
> dev-204 기준 아래 6-1~6-3 모두 적용 완료. 절차는 **신규 RK356x 보드** 세팅 시 참고용으로 남겨둠. (신규 보드는 버전을 최신으로 대체 — 현재 플릿 기준 **1.0.7**)

### 6-1. 영구 ADB TCP 활성화 (재부팅 유지)
```powershell
& $adb shell "su -c 'setprop service.adb.tcp.port 5555; stop adbd; start adbd'"
# 확인
& $adb shell "getprop service.adb.tcp.port"   # 5555 나와야 함
```
- 앱의 `BootReceiver`/`PlayerApp`이 부팅마다 이걸 자동 수행하므로, root만 되면 **재부팅 후에도 자동 유지**됨.
- 이후 네트워크 ADB: `adb connect 172.30.1.19:5555`

### 6-2. SignagePro 앱 설치 (현재 dev-204 = 1.0.7 / versionCode 10007)
> ⚠️ 아래 0.4.29 경로·버전은 2026-06-13 당시 값. 현재 플릿은 **1.0.7**. 신규 설치 시 최신 릴리스 APK 사용.
- (구) APK: `D:\WorkSpace\signagepro\android\app\build\outputs\apk\debug\app-debug.apk` (versionCode 29, versionName 0.4.29)
```powershell
& $adb connect 172.30.1.19:5555
& $adb -s 172.30.1.19:5555 install -r "D:\WorkSpace\signagepro\android\app\build\outputs\apk\debug\app-debug.apk"
```
- `-r`(재설치)로 서버주소 설정 유지. 204의 서버주소는 **`http://172.30.1.44:3001`** 권장(공인 IP 쓰면 하어핀 NAT로 IP가 게이트웨이로 잡히는 문제 있음 — 아래 8번).

### 6-3. Tailscale 설치 (원격 ADB의 핵심)
- 204가 Android 11이라 **Tailscale 공식 앱 사용 가능**(구형 5.1.1은 ZeroTier).
- Tailscale APK 설치 → 서버 PC와 **동일 Tailscale 계정** 로그인 → 204가 `100.x.x.x` IP 획득.
- 그러면 어디서든: `adb connect <204-Tailscale-IP>:5555` → 원격 복구 가능.
- 서버 PC Tailscale IP: `100.72.54.115` (기존). 자세한 건 `TAILSCALE_SETUP.md` 참고.

---

## 7. 프로젝트 규칙 (반드시 준수)
- **dev에서만 작업**: `D:\WorkSpace\signagepro` (dev 브랜치, API 포트 3001). staging(`D:\signagepro`, 3300)·원격 운영(main) 건드리지 말 것.
- **APK 자율 배포 금지**: 기기 설치/OTA 푸시는 **사용자가 직접**. AI가 자동 배포 X.
- **APK 버전**: 변경 시 versionCode/versionName **+1** ([build.gradle.kts](android/app/build.gradle.kts), 현재 29 / 0.4.29).
- 운영 서버 pm2 restart / git push 등 자율 실행 금지.

---

## 8. 알아두면 좋은 배경(이미 해결/발견된 것)
- **앱 TCP 포트 로직 수정 완료**: `PlayerCoordinator.kt:581` 에서 `httpPort==3000||3001 → TCP 10081, 그 외 → 10080`. (dev 서버 HTTP 3001 / TCP 10081 매칭). 이게 0.4.29의 핵심 변경.
- **하어핀 NAT 함정**: 기기가 공인 IP(`211.184.50.200`)로 접속하면 같은 LAN 내 하어핀 때문에 서버가 기기 IP를 **게이트웨이(172.30.1.254)** 로 기록 → 대시보드 ADB 설치가 엉뚱한 IP를 때려 실패함. **해결: 기기 서버주소를 LAN IP `172.30.1.44:3001` 로 설정.**
- **KT 공유기 포트포워딩**(이미 설정됨): dev-api `3001→172.30.1.44:3001`, dev-tcp `10081→…:10081`, dev-mobile `5175→…:5175`. (관리주소 `172.30.1.254:8899`)
- 204 정보: Android 11, LAN `172.30.1.19`, ADB TCP 현재 OFF.

---

## 9. 상태 (완료)
dev-204 루팅 작업은 **종료**되었습니다. 남은 To-Do 없음.
- ✅ 기기 인식(Tailscale ADB) · SoC/모델 확정(RK356x / Ultracube U4X CM V2)
- ✅ root 확인(su, uid=0) — 플래싱 불필요였음
- ✅ 영구 ADB TCP 5555 · SignagePro 1.0.7 · Tailscale(`100.75.80.108`)

### 향후 신규 RK356x 보드 입고 시
1. `adb connect <IP>:5555` (또는 USB) → **4번 진단**으로 root 여부 판정.
2. 이미 root면 → **6번**(영구 ADB + 최신 APK + Tailscale)만 적용.
3. 비루팅이면 → **5번** 절차(RK356x는 RKDevTool + 패치 boot.img). **SoC 확인 전 플래싱 금지(벽돌 위험).**
4. 가능하면 **루팅 상태로 구매**해 5번 자체를 생략.
