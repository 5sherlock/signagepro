# 셋톱박스(dev-204) 루팅 작업 핸드오프 (→ Gemini)

> 작성: 2026-06-13. Claude가 진행하던 작업을 Gemini가 이어받기 위한 자체 완결 문서.
> 이 문서만 읽고 바로 이어서 진행 가능하도록 작성함. 작업 환경은 Windows + PowerShell/Git Bash.

---

## 0. 한 줄 요약
**dev-204 (Android 11, LAN IP `172.30.1.19`) 를 루팅**해서, 앞으로 구매할 **루팅된 신규 기기들과 동일한 개발 환경**을 만드는 것이 목표. 현재 **USB로 기기가 PC에 인식되지 않아** 막힌 상태(포트 변경 시도 중).

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
| 기기 | OS | SoC | LAN IP | 상태 | 처리 방침 |
|------|----|----|--------|------|-----------|
| dev-201 | Android 5.1.1 | RK3229 | (미상) | 오프라인 | 사용자가 USB로 0.4.29 설치 |
| dev-202 | Android 5.1.1 | RK3229 | (미상) | 오프라인 | 동일 |
| dev-203 | Android 5.1.1 | RK3229 | (미상) | 오프라인 | 동일 |
| **dev-204** | **Android 11** | **미상(확인 필요)** | **172.30.1.19** | 오프라인 | **루팅 대상 ← 이 작업** |

- 204는 LAN ping 응답 OK. 단 **ADB TCP(5555) 꺼져 있음**(`adb connect 172.30.1.19:5555` → 연결 거부 10061).
- 204의 정확한 SoC/모델은 **아직 확인 못 함**(ADB 접속이 안 돼서).

---

## 3. 지금까지 진행 / 막힌 지점
- `adb devices` 에 204가 **안 잡힘**. `adb kill-server && adb start-server` 후에도 동일.
- Windows USB 장치 스캔 결과: **안드로이드 기기 VID 없음.** 보이는 건 Logitech 키보드/마우스(VID `0x046D`)와 ASMedia USB 허브(VID `0x174C`)뿐.
  - 안드로이드 제조사 VID 참고: Rockchip `0x2207`, Amlogic `0x1b8e`, Google `0x18d1`, Allwinner `0x1f3a`.
- **추정 원인**: TV박스의 USB-A 포트는 보통 **호스트 포트**(USB 메모리용)라 PC에 기기로 안 붙음. **OTG/device 포트**(별도 micro-USB/USB-C, 또는 "OTG" 표시 포트)로 연결해야 함. 또는 데이터 케이블이 아닐 수 있음.
- **사용자가 현재 "셋톱 USB 포트를 바꿔보는 중".** → 다시 연결되면 아래 4번 진단부터 진행.

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

## 6. 루팅 성공(또는 이미 root) 후 할 일
### 6-1. 영구 ADB TCP 활성화 (재부팅 유지)
```powershell
& $adb shell "su -c 'setprop service.adb.tcp.port 5555; stop adbd; start adbd'"
# 확인
& $adb shell "getprop service.adb.tcp.port"   # 5555 나와야 함
```
- 앱의 `BootReceiver`/`PlayerApp`이 부팅마다 이걸 자동 수행하므로, root만 되면 **재부팅 후에도 자동 유지**됨.
- 이후 네트워크 ADB: `adb connect 172.30.1.19:5555`

### 6-2. SignagePro 앱 최신(0.4.29) 설치
- APK: `D:\WorkSpace\signagepro\android\app\build\outputs\apk\debug\app-debug.apk` (versionCode 29, versionName 0.4.29, 이미 빌드됨)
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

## 9. Gemini가 바로 할 일 (요약)
1. 사용자가 USB 포트 변경/재연결 → `adb devices` 로 204 인식 확인.
2. 인식되면 **4번 진단** 실행 → SoC/모델/root 여부 판정.
3. 이미 root거나 `adb root` 되면 → **6번**(영구 ADB + 0.4.29 + Tailscale)으로 바로 진행.
4. 비루팅 확정이면 → **4번에서 얻은 SoC 기준으로 5번 루팅 경로**를 구체화(그 기기 펌웨어/도구 안내). 플래싱은 사용자 물리 작업 + 신중히.
5. USB가 끝내 안 잡히면(호스트 포트뿐) → OTG/device 포트 탐색 또는 박스 모델 확인 후 벤더 플래싱 도구(RKDevTool/Amlogic) 경로로 전환.

**핵심: SoC 확인 전에는 플래싱 명령 실행 금지(벽돌 위험).**
