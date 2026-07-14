# STB 부트애니메이션 (부팅 로고)

우리 브랜드 부트애니. 새 STB 들어오면 아래 방법대로 넣으면 됨.

## 파일
- `bootanimation_mgm_cat_1080p.zip` — **완성본**. MGM 스타일 금색 필름릴 + 상단 "SignagePro" +
  중앙 우리집 고양이가 하품. 1920x1080 / 24fps / 256색 / 110프레임(핑퐁 무한루프). 38MB.
- `source_mgm_cat.mp4` — 원본 영상(1280x720, 64프레임). 재편집 시 사용.
- `build_bootanim.py` — 재빌드 스크립트(프레임 추출은 ffmpeg 선행).
- `OEM_backup_*.zip` — 공장 기본 부트애니 백업(되돌릴 때).

## ⚠️ 기종별 부트애니 경로가 다름 (핵심 함정)
| 기종 | 안드로이드 | 읽는 경로(우선순위) | 쓰기 방법 |
|---|---|---|---|
| **큐버 QR5G-M110S** (RK3566, osl-1~5) | 11 | **`/cache/media/animation/bootanimation.zip`** + **`/cache/media/logo/custom_logo.bmp`** | **remount 불필요** — `/cache`가 rw. 그냥 push |
| **rk356x** (Ultracube U4X V2, dev-104) | 11 | **`/odm/media/bootanimation.zip`** 가 `/system/media`보다 우선 | overlayfs → `adb root && adb remount` |
| **rk322x** (U4X+CM, dev-101~103) | 5.1.1 | `/system/media/bootanimation.zip` (/odm 없음) | ext4 ro → `mount -o rw,remount /system` |

rk356x에서 `/system/media`만 바꾸면 **안 먹힘** — OEM이 바이너리를 패치해 `/odm/media`를
먼저 읽음. 반드시 `/odm/media` 를 덮을 것. (진단: `find / -iname 'bootanimation*.zip'`)

**큐버는 위 둘과 완전히 다름.** 같은 rk356x인데도 `/odm/media`·`/system/media` 디렉터리 자체가
없고, 부트애니 파일이 공장 출하 시 **하나도 없다**. 큐버가 `libbootanimation.so`를 패치해
`/cache` 아래를 읽도록 바꿨고, `init.rc`가 부팅 때 폴더를 만들어 둔다:
```
# /system/etc/init/hw/init.rc:486
# 240221_JYW : Change custom boot animation path under /cache
mkdir /cache/media        0777 root root
mkdir /cache/media/animation 0777 root root
mkdir /cache/media/logo      0777 root root
```
사용자 가이드의 **[디스플레이 설정 → 부트 로고/애니메이션]** 화면에 있는 "로고 경로 / 애니메이션
경로 [선택]" 버튼이 하는 일이 정확히 이 두 파일 복사다. (근거: `QuberUpdate.apk` 안에
`/cache/media/logo/custom_logo.bmp`, `/cache/media/animation/bootanimation.zip` 문자열)
→ **리모컨으로 한 대씩 할 필요 없이 ADB로 5대 일괄 배포 가능.**

> 참고: `com.android.settings/.Settings$QuberDashboardActivity` 는 매니페스트에만 있고
> 클래스가 APK에 없어 `am start` 하면 **ClassNotFoundException 으로 크래시**한다(큐버 빌드 결함).
> 설정 UI를 인텐트로 직접 띄우려 하지 말 것.

## 배포 방법

### 큐버 QR5G-M110S (Android 11, /cache) — 현재 오설록 5대
```bash
adb connect <tailscale-ip>:5555     # osl-1~5 는 tailnet IP 로 바로 붙는다
adb root                            # /cache push 하려면 필수 (재부팅하면 풀림 → 다시 root)
adb push bootanimation_mgm_cat_1080p.zip /cache/media/animation/bootanimation.zip
adb push custom_logo.bmp                 /cache/media/logo/custom_logo.bmp
adb shell "su 0 sh -c 'chmod 644 /cache/media/animation/bootanimation.zip /cache/media/logo/custom_logo.bmp'"
# 재부팅하면 적용됨. /cache 는 별도 파티션(771M)이라 초기화/OTA 때 날아갈 수 있음 → 재적용 필요할 수 있음.
```
- **로고는 파일명이 `custom_logo.bmp` 로 고정**. BMP(1920x1080, 24bit)로 저작.
  안 넣으면 공장 기본 **"quber" 로고**가 애니메이션 앞에 뜬다.
- 로고를 애니메이션 **첫 프레임(part0/0000.png)** 으로 만들면 로고→애니가 끊김 없이 이어진다.
- ⚠️ **Git Bash 함정**: `adb push ... /cache/...` 하면 MSYS가 경로를 `D:/Git/cache/...` 로
  자동 변환해 **"1 file pushed" 라고 하면서 엉뚱한 데로 간다.** 반드시 `MSYS_NO_PATHCONV=1`
  (또는 PowerShell 사용).

### rk356x (Android 11, /odm)
```bash
adb connect <IP>:5555
adb root && adb remount                       # overlay rw
adb push bootanimation_mgm_cat_1080p.zip /sdcard/ba.zip
adb shell "cp /sdcard/ba.zip /odm/media/bootanimation.zip && \
  chmod 644 /odm/media/bootanimation.zip && \
  chcon u:object_r:oemfs:s0 /odm/media/bootanimation.zip"   # 컨텍스트는 oemfs!
# (원하면 /system/media 도 동일하게 복사, 컨텍스트는 system_file:s0)
```

### rk322x (Android 5.1.1, /system)
```bash
adb connect <IP>:5555
adb root
adb shell "mount -o rw,remount /system"
adb push bootanimation_mgm_cat_1080p.zip /sdcard/ba.zip
adb shell "cp /sdcard/ba.zip /system/media/bootanimation.zip && \
  chmod 644 /system/media/bootanimation.zip && \
  chcon u:object_r:system_file:s0 /system/media/bootanimation.zip"
```
저사양이라 1080p/110프레임이 버벅이면 `build_bootanim.py`에서 `COLORS=128`,
`FPS=12`(핑퐁 프레임 절반) 로 경량 버전 재빌드 권장.

## 재부팅 없이 라이브 확인
```bash
adb shell "setprop service.bootanim.exit 0; /system/bin/bootanimation &"
# 5.1.1은 pkill/awk 없음 → 끌 때: setprop service.bootanim.exit 1
# A11: sleep N; kill %1; pkill bootanimation
```

## zip 필수 규칙 (안 지키면 기본 로고로 폴백)
- **무압축(Stored)** zip. deflate면 안 됨.
- `desc.txt` = `WIDTH HEIGHT FPS` + `p <반복> <일시정지> part0`, **마지막 줄 개행 필수**.
- 프레임 PNG 실제 크기 = desc 해상도와 일치.
- 부트애니는 프레임을 화면 중앙에 **원본 크기로** 얹음(스케일 안 함) → 패널 해상도(1080p)로 저작해야 꽉 참.

## 히스토리
- 2026-07-14: **큐버 QR5G-M110S 5대(osl-1~5) 적용 완료.** 경로가 `/cache/media` 라는 걸 밝혀냄
  (`libbootanimation.so` strings + `QuberUpdate.apk` + `init.rc`). remount 불필요.
  로고는 애니 첫 프레임을 1920x1080 BMP로 뽑아 `custom_logo.bmp` 로 넣어 "quber" 로고 제거.
- 2026-07-01: MGM 고양이 컨셉 제작. dev-104(rk356x /odm) + dev-101(.87 rk322x /system) 적용.
  dev-102(.98)/dev-103(.1)은 미적용(신규 STB 대기). 원본 영상의 마지막 액자 전환(57~64f) 제외,
  Gemini 워터마크 제거함.
- 2번째 컨셉(잔디밭에 누워 자는 거대 고양이 꼬리 흔들기)은 영상 준비되면 제작 예정.
