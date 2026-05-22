# 원격 접속 VPN 설정 가이드

기기, 서버, 관리자 PC가 각각 다른 장소/네트워크일 때 가상 네트워크로 연결.

---

## 구성 완료 현황

| 장치 | 역할 | Tailscale IP | 상태 |
|------|------|-------------|------|
| 서버 PC | SignagePro 서버 | 100.72.54.115 | ✅ 완료 |
| 관리자 PC | 대시보드 원격 접속 | - | ⏳ 미설치 |
| 현장 기기 (Android 5.1.1) | 플레이어 | - | ⚠️ 별도 검토 필요 |

---

## 1. 서버 PC (완료)

```powershell
winget install tailscale.tailscale
& "C:\Program Files\Tailscale\tailscale.exe" up   # 브라우저 로그인
& "C:\Program Files\Tailscale\tailscale.exe" ip   # → 100.72.54.115
```

---

## 2. 관리자 PC 설정 (미완료)

1. https://tailscale.com/download 에서 Windows 설치파일 다운로드
2. 설치 후 **같은 Tailscale 계정**으로 로그인
3. 브라우저에서 대시보드 접속: `http://100.72.54.115:3000`

> ⚠️ 서버 URL을 Tailscale IP로 바꿀 필요 없음 — 관리자 PC만 Tailscale에 연결되면 됨

---

## 3. 현장 기기 (Android 5.1.1) — VPN 옵션 비교

> **문제**: Tailscale 공식 앱은 Android 6.0 (API 23) 이상만 지원.
> 현장 기기(U4X+, Android 5.1.1 / API 22)에 설치 불가.

### 옵션 A: ZeroTier (권장)

- Android 4.1(API 16) 이상 지원 → 현장 기기에 설치 가능
- Tailscale과 동일한 개념의 P2P 메시 VPN
- 무료 플랜: 기기 25대까지

**설치 순서:**
1. https://my.zerotier.com 에서 계정 생성
2. "Create A Network" → Network ID 복사 (예: `a09acf0233b30dba`)
3. 서버 PC: https://www.zerotier.com/download/ 에서 Windows 설치
4. 현장 기기: `zerotier-one.apk` ADB 사이드로드
5. 각 기기에서 Network ID로 네트워크 참가
6. 관리자 페이지에서 각 기기 승인(Authorize)

**현장 기기 서버 URL 변경:**
```
기존: http://192.168.0.25:3000
변경: http://<서버PC ZeroTier IP>:3000
```

---

### 옵션 B: 현재 구조 유지 (기기·서버 동일 LAN)

현장 기기가 서버와 **같은 매장/같은 LAN**에 있다면 VPN 불필요.
관리자 PC만 Tailscale로 원격 접속.

```
[현장 기기] ── LAN ── [서버 PC] ── Tailscale ── [관리자 PC 어디서든]
              (동일 장소)     100.72.54.115           접속
```

---

## 참고: ADB 원격 제어

Tailscale/ZeroTier 연결 후 VPN IP로 ADB 접속:
```powershell
adb connect 100.72.54.115:5555   # Tailscale IP 예시
adb connect 172.x.x.x:5555      # ZeroTier IP 예시
```
