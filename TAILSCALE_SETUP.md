# 원격 접속 VPN 설정 가이드

기기, 서버, 관리자 PC가 각각 다른 장소/다른 네트워크일 때  
가상 네트워크(VPN)로 하나의 네트워크처럼 연결하는 설정 가이드.

---

## 전체 구성도 (완성 후)

```
[현장 기기 - 매장A]  ──┐
[현장 기기 - 매장B]  ──┤── ZeroTier 가상 네트워크 ──── [서버 PC]
[현장 기기 - 매장C]  ──┘                                    │
                                                    Tailscale
                                                        │
                                              [관리자 PC - 어디서든]
                                          http://100.72.54.115:3000
```

---

## 현재 완료 상태

| 장치 | VPN | IP | 상태 |
|------|-----|----|------|
| 서버 PC | Tailscale | 100.72.54.115 | ✅ 완료 |
| 관리자 PC | Tailscale | - | ⏳ 설치 예정 |
| 현장 기기 (Android 5.1.1) | ZeroTier | - | ⏳ 설정 예정 |

---

## STEP 1 — 관리자 PC Tailscale 설치 (예정)

1. https://tailscale.com/download → Windows 설치파일 다운로드 및 설치
2. 서버 PC와 **동일한 Tailscale 계정**으로 로그인
3. 브라우저에서 대시보드 접속: `http://100.72.54.115:3000`

> 서버 PC에 Tailscale이 이미 설치되어 있으므로 관리자 PC 설치만 하면 됨.

---

## STEP 2 — 현장 기기 ZeroTier 설치 (예정)

> **Tailscale 사용 불가 이유**: 공식 앱이 Android 6.0(API 23) 이상만 지원.  
> 현장 기기(U4X+)는 Android 5.1.1(API 22) → **ZeroTier** 사용 (Android 4.1 이상 지원, 무료).

### 2-1. ZeroTier 계정 및 네트워크 생성

1. https://my.zerotier.com 접속 → 계정 생성 (Google/GitHub 로그인 가능)
2. **Create A Network** 클릭
3. 생성된 **Network ID** 메모 (16자리, 예: `a09acf0233b30dba`)
4. Network 설정에서 **Access Control → Private** 유지 (보안)

### 2-2. 서버 PC에 ZeroTier 설치

```powershell
winget install zerotier.zerotier
zerotier-cli join <Network ID>   # 예: zerotier-cli join a09acf0233b30dba
```

ZeroTier 관리 페이지에서 서버 PC **Authorize** 체크 → IP 부여됨 (예: `172.28.x.x`)

### 2-3. 현장 기기에 ZeroTier APK 설치

ZeroTier Android APK (Android 5.x 지원):
- https://github.com/zerotier/ZeroTierOne/releases 에서 `.apk` 다운로드
- 또는 Google Play: `com.zerotier.one`

```powershell
# 기기별 ADB로 설치
adb connect 192.168.0.76:5555
adb -s 192.168.0.76:5555 install -r zerotier.apk

adb connect 192.168.0.73:5555
adb -s 192.168.0.73:5555 install -r zerotier.apk

adb connect 192.168.0.75:5555
adb -s 192.168.0.75:5555 install -r zerotier.apk
```

앱 실행 → Network ID 입력 → Join → ZeroTier 관리 페이지에서 각 기기 **Authorize**

### 2-4. SignagePro 서버 URL 변경

각 현장 기기 앱 설정 (우상단 2회 탭 → 설정 변경):
```
기존: http://192.168.0.25:3000   ← 로컬 LAN IP (다른 장소에서 안됨)
변경: http://172.28.x.x:3000    ← 서버 PC의 ZeroTier IP
```

### 2-5. 서버 PM2 URL 확인

서버 PC의 ZeroTier IP 확인:
```powershell
zerotier-cli listnetworks
# 또는
ipconfig | findstr "172."
```

---

## 참고사항

### ADB 원격 접속 (ZeroTier 연결 후)

현장 기기가 다른 장소에 있어도 ZeroTier IP로 ADB 접속:
```powershell
adb connect 172.28.x.x:5555   # 기기의 ZeroTier IP
```

### Tailscale vs ZeroTier 역할 분리 이유

| | Tailscale | ZeroTier |
|--|-----------|---------|
| 서버 PC | ✅ 설치 | ✅ 설치 |
| 관리자 PC | ✅ 설치 | 불필요 |
| 현장 기기 | ❌ Android 5.1.1 미지원 | ✅ 설치 |

- 관리자 PC → 서버 접속: **Tailscale** (`100.72.54.115:3000`)
- 현장 기기 → 서버 접속: **ZeroTier** (`172.28.x.x:3000`)
- 서버 PC는 두 VPN 모두 설치하여 양쪽 다 수용
