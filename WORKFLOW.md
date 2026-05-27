# SignagePro 작업 흐름 가이드

> 이 문서는 일상 운영, USB 백업, 새 PC 구축의 실행 순서를 설명합니다.

---

## 1. 서버 일상 운영 (서버 PC)

### 서버 시작
```powershell
cd C:\WorkSpace\signagepro\server
node index.js
```
또는 PM2로 백그라운드 실행:
```powershell
pm2 start C:\WorkSpace\signagepro\server\index.js --name signagepro --cwd C:\WorkSpace\signagepro\server
pm2 start node --name dashboard -- C:\WorkSpace\signagepro\dashboard\node_modules\vite\bin\vite.js --cwd C:\WorkSpace\signagepro\dashboard
pm2 save
```

### 대시보드 접속
- 로컬: http://localhost:5173
- API: http://localhost:3000

---

## 2. 개발 작업 흐름

```
코드 수정
  ↓
git add / commit (dev 브랜치)
  ↓
테스트 확인
  ↓
USB 백업 (export_usb.bat)
```

### 브랜치 규칙
| 브랜치 | 용도 |
|--------|------|
| `dev`  | 일상 작업, 기능 추가 |
| `main` | 안정 버전 보존 (직접 커밋 금지) |

---

## 3. USB 백업 (서버 PC → USB)

> **언제**: 작업 종료 시, 중요 변경 후, 현장 방문 전

### 실행 순서

1. USB를 서버 PC에 연결
2. USB 루트의 **`export_usb.bat`** 더블클릭

```
F:\export_usb.bat
```

### 스크립트가 자동으로 하는 일

**[1/3] Git 커밋 & Push**
- `git add -A` — 수정·추가·삭제된 파일 전체 스테이징
- 변경사항이 있으면 **터미널에서 커밋 메시지 입력 대기**
- `git commit -m "입력한 메시지"`
- `git push origin dev` — GitHub dev 브랜치에 업로드
- 변경사항 없으면 push 건너뜀

**[2/3] USB 자동 감지**
- 이동식 드라이브(DriveType=2) 자동 탐색
- USB가 여러 개면 선택 프롬프트 표시

**[3/3] 파일 복사**
- USB `signagepro_backup\` 폴더에 아래 파일 복사:

**복사되는 파일:**
- `server/prisma/dev.db` — 데이터베이스
- `server/.env` — 환경변수 (DEVICE_SECRET 포함)
- `server/uploads/` — 업로드된 미디어 파일
- `server/update/app.apk` — OTA 배포용 APK
- `android/app/signagepro.keystore` — APK 서명 키
- `android/app/build/.../app-debug.apk` — 최신 빌드 APK
- `setup_from_usb.ps1` / `.bat` — 새 PC 구축 스크립트
- `SETUP.md` — 자동 생성 가이드

---

## 4. 새 PC 구축 (USB → 새 PC)

> **언제**: 새 PC에 서버 환경을 처음 구성할 때

### 사전 설치 (새 PC에 미리 설치 필요)

| 소프트웨어 | 버전 | 다운로드 |
|-----------|------|---------|
| Node.js | 20+ | https://nodejs.org |
| Java JDK | 21 | https://www.oracle.com/java |
| Android Studio | 최신 | https://developer.android.com/studio |
| Git | 최신 | https://git-scm.com |

### 실행 순서

**① 저장소 클론** (PowerShell)
```powershell
git clone https://github.com/5sherlock/signagepro.git C:\WorkSpace\signagepro
```

**② USB 연결 후 더블클릭**
```
USB:\signagepro_backup\setup_from_usb.bat
```

### 스크립트가 자동으로 하는 일

| 단계 | 내용 |
|------|------|
| Step 1 | 저장소 폴더 확인 |
| Step 2 | DB, .env, 미디어, APK, 서명 키 복사 |
| Step 3 | `local.properties` (Android SDK 경로) 자동 생성 |
| Step 3 | `gradle.properties` (Java 21 경로) 자동 추가 |
| Step 4 | `npm install` + `prisma generate` + `prisma db push` |
| Step 5 | 대시보드 `npm install` |
| Step 6 | PM2 설치 → 서버 + 대시보드 시작 |

---

## 5. Android APK 빌드 및 배포

### APK 빌드
```powershell
cd C:\WorkSpace\signagepro\android
.\gradlew.bat assembleDebug
```
빌드 결과: `android\app\build\outputs\apk\debug\app-debug.apk`

### OTA 배포 (Wi-Fi)
1. 대시보드 → **환경설정** 탭
2. APK 파일 업로드
3. 배포할 기기 선택 (체크박스)
4. **🚀 OTA 푸시** 클릭

### ADB 유선 설치 (초기 설치 또는 키 변경 시)
```powershell
# USB 연결 후
adb devices                          # 기기 확인
adb uninstall com.signagepro.player  # 기존 앱 제거 (필요 시)
adb install app-debug.apk            # 설치
```

> ⚠️ **uninstall 시 기기 설정이 초기화됩니다.**  
> 서버 주소와 Device ID를 앱에서 재입력하거나 ADB로 복원하세요.

---

## 6. 포트 정보

| 포트 | 용도 |
|------|------|
| 3000 | API 서버 + Socket.io |
| 5173 | 대시보드 (개발) |
| 10080 | Android TCP 하트비트 |
| 5555 | ADB over TCP (무선) |

---

## 7. 파일 위치 요약

| 파일 | 위치 | git |
|------|------|-----|
| 데이터베이스 | `server/prisma/dev.db` | ❌ 제외 |
| 환경변수 | `server/.env` | ❌ 제외 |
| 미디어 파일 | `server/uploads/` | ❌ 제외 |
| APK 서명 키 | `android/app/signagepro.keystore` | ❌ 제외 |
| OTA APK | `server/update/app.apk` | ❌ 제외 |
| 백업 스크립트 | `export_usb.ps1` / `.bat` | ✅ 포함 |
| 구축 스크립트 | `setup_from_usb.ps1` / `.bat` | ✅ 포함 |
