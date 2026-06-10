# 화면 스케줄 ON/OFF 불량 원인 분석 및 수정 내역
**버전:** v0.4.25  
**작성일:** 2026-06-11  
**대상 기기:** dev-101 ~ dev-105 (현장 RK3229 STB)

---

## 1. 현상

| 항목 | 상태 |
|------|------|
| STB 전원 | 5대 모두 켜짐 |
| 서버 연결 (Socket.io / TCP 하트비트) | 5대 모두 정상 연결 |
| 대시보드 온라인 표시 | 5대 모두 온라인 |
| 스케줄 OFF 동작 | 5대 모두 정상 (TV 꺼짐) |
| 스케줄 ON 동작 | **101만 정상 (102~105 TV 꺼진 채 고착)** |

---

## 2. 시스템 구성

```
현장 STB (Android, RK3229)
  └─ HDMI ──→ TV 디스플레이
  └─ iptime(KT모뎀) ──→ Cloudflare 터널 ──→ 서버 (port 3300)

서버: Express.js + Socket.io + node-cron
  └─ 스케줄 onTime/offTime → runScreenCommand() → 기기별 room에 screen_control 이벤트 전송
```

---

## 3. 코드 흐름 (수정 전)

### 3-1. 화면 끄기 (OFF) — 5대 모두 정상 작동

```
서버 cron → runScreenCommand(on=false)
 → io.to('device:dev-10X').emit('screen_control', { deviceId, on:false })
 → [기기] ControlChannel.kt: screen_control 수신
 → ScreenScheduleManager.applyScreenState(false)
 → turnScreenOff()
   → dpm.lockNow()          ← Device Admin 권한 (5대 모두 활성화)
   → TV 화면 꺼짐 + 잠금화면 활성 ✓
```

### 3-2. 화면 켜기 (ON) — 101만 성공, 102~105 실패

```
서버 cron → runScreenCommand(on=true)
 → io.to('device:dev-10X').emit('screen_control', { deviceId, on:true })
 → [기기] ControlChannel.kt: screen_control 수신
 → ScreenScheduleManager.applyScreenState(true)
 → turnScreenOn()
   → WakeLock.acquire(3초)   ← 화면 3초 깨어남
   → [101] 잠금화면 없음 → 앱 즉시 표시 → FLAG_KEEP_SCREEN_ON → 유지 ✓
   → [102~105] 잠금화면 표시 → 앱이 잠금화면 뒤에 갇힘
              → 3초 후 WakeLock 해제 → 화면 다시 꺼짐 ✗
```

---

## 4. 원인 분석

### 원인 A — `dpm.lockNow()` 가 잠금화면을 생성함 (주요 원인)

`dpm.lockNow()`는 화면을 끄는 동시에 **Android 잠금화면을 활성화**한다.  
이후 `WakeLock`으로 화면을 깨워도 잠금화면이 앞에 표시될 뿐이며,  
앱(`MainActivity`)은 잠금화면 뒤에 갇혀 `FLAG_KEEP_SCREEN_ON`이 작동하지 않는다.

```
dpm.lockNow() 후:
  [화면] 잠금화면 (foreground)
  [앱]  MainActivity (background) ← FLAG_KEEP_SCREEN_ON 무효
```

**dev-101이 유일하게 작동하는 이유:**  
101은 Android 보안 잠금(PIN/패턴)이 설정되지 않아 `lockNow()`가 화면만 끄고  
잠금화면을 생성하지 않음 → WakeLock으로 화면을 깨우면 앱이 바로 표시됨.  
102~105는 PIN 또는 패턴 잠금이 설정되어 있어 `lockNow()` 후 보안 잠금화면이 생성됨.

> **참고:** Device Admin 활성화 여부는 5대 모두 동일 (전부 활성화).  
> 차이는 Android 보안 잠금(PIN/패턴) 설정 여부.

### 원인 B — `lastScreenOn` 조기 갱신으로 재시도 불가

```kotlin
// ScreenScheduleManager.kt (수정 전)
private fun applyScreenState(on: Boolean) {
    if (lastScreenOn == on) return
    lastScreenOn = on          // ← 실행 성공 전에 미리 갱신
    if (on) turnScreenOn()     // 실패해도 이후 명령에서 "이미 켜짐"으로 무시
}
```

`turnScreenOn()`이 잠금화면 때문에 실제로 효과가 없어도 `lastScreenOn = true`가  
저장되어, 이후 모든 ON 명령이 차단됨.

---

## 5. 수정 내역 (v0.4.25)

### 5-1. `MainActivity.kt` — 잠금화면 위에 앱 표시

```kotlin
// onCreate() 에 추가
window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

// 추가된 코드
if (Build.VERSION.SDK_INT >= 27) {
    setShowWhenLocked(true)   // 잠금화면 위에 Activity 표시
    setTurnScreenOn(true)     // Activity 표시 시 화면 자동 켜기
} else {
    window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
    )
}
```

### 5-2. `ScreenScheduleManager.kt` — 켜기 시 앱 포그라운드 복귀

```kotlin
private fun turnScreenOn() {
    // 1. WakeLock으로 화면 깨우기
    val wl = powerManager.newWakeLock(
        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
        "SignagePro:ScreenScheduleOn"
    )
    wl.acquire(3_000L)

    // 2. MainActivity를 포그라운드로 복귀 (잠금화면 해제) ← 핵심 추가
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP or FLAG_ACTIVITY_REORDER_TO_FRONT)
    }
    context.startActivity(intent)

    // 3. su 폴백 (rooted 기기)
    Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent 224"))

    onScreenStateChange?.invoke(true)
}
```

### 5-3. `build.gradle.kts` — 버전 업

```
versionCode : 18 → 19
versionName : "0.4.17" → "0.4.25"
```

> Android는 동일 서명 + 높은 versionCode 일 때만 OTA 업데이트를 수락함.

---

## 6. 수정 후 동작 흐름

```
스케줄 OFF 시각:
  runScreenCommand(on=false)
  → screen_control 수신
  → dpm.lockNow() → TV 꺼짐 + 잠금화면 활성

스케줄 ON 시각:
  runScreenCommand(on=true)
  → screen_control 수신
  → WakeLock.acquire(3초) → 화면 깨어남
  → startActivity(MainActivity)
  → setShowWhenLocked → 잠금화면 위에 앱 표시  ← 핵심
  → FLAG_KEEP_SCREEN_ON → TV 화면 유지 ✓
```

---

## 7. 즉시 조치 (APK 배포 전)

STB가 전원 켜진 상태이므로 Socket.io 연결이 살아있음.  
대시보드에서 102~105 각 기기의 **"앱 재시작"** 또는 **"기기 재부팅"** 명령을 전송하면  
앱이 재시작되면서 `FLAG_KEEP_SCREEN_ON`에 의해 TV 화면이 복귀됨.

---

## 8. 배포 정보

| 항목 | 내용 |
|------|------|
| 브랜치 | `release/0.4.25` |
| 커밋 | `7b6c732` |
| APK 파일 | `server/update/signagepro-0.4.25.apk` |
| 빌드 결과 | BUILD SUCCESSFUL (47초) |
| 배포 방법 | 대시보드 → 환경설정 → OTA 배포 |
