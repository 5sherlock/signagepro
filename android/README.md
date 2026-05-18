# SignagePro Player (Android)

오설록 매장 디지털 사이니지용 Android 플레이어.

## 타겟 환경

- **하드웨어**: ULTRACUBE U4X+ CM (Rockchip RK3229, 2GB DDR3, 8GB Flash)
- **OS**: Android 5.1.1 (API 22) ~ 최신
- **runtime**: Kotlin + Media3(ExoPlayer)

## 빌드

1. Android Studio에서 `android/` 폴더 열기
2. Gradle Sync (의존성 다운로드)
3. 실기기/에뮬레이터 연결 후 Run

## 첫 실행

`SetupActivity`에서 입력:
- **디바이스 ID** — 서버 대시보드에서 등록한 ID와 일치해야 함
- **서버 주소** — 예: `http://192.168.0.10:3000`
- **디바이스 시크릿** — 서버 `.env`의 `DEVICE_SECRET`과 동일

저장 후 자동으로 `KioskActivity`(풀스크린)로 진입.

## 현재 상태 (Phase 0 — 골조)

✅ 프로젝트 구조, 매니페스트, 권한
✅ ConfigStore (SharedPreferences)
✅ BootActivity / SetupActivity / KioskActivity 골격
✅ BootReceiver (단말 부팅 시 자동 실행)
⏳ ApiClient (Retrofit + Moshi)
⏳ ControlChannel (Socket.io)
⏳ HeartbeatService (TCP 10080)
⏳ NtpClient
⏳ MediaCacheRepo
⏳ PlaylistEngine + ExoPlayer 연결
⏳ TransitionLayer
