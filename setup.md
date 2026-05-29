# SignagePro 서버 설치 가이드

## 준비물

- USB (이 파일이 들어있는 USB)
- 서버로 사용할 Windows PC (Windows 10/11)
- 인터넷 연결

---

## 설치 순서

### 1단계 — USB를 서버 PC에 연결

### 2단계 — 설치 스크립트 실행

`setup_from_usb.bat` 파일을 **우클릭 → 관리자 권한으로 실행**

자동으로 처리되는 항목:
- Node.js 설치 (없는 경우)
- 서버 파일 복사 (`C:\signagepro\`)
- 대시보드 빌드 파일 복사
- 데이터베이스 복사 및 초기화
- PM2 설치 및 서버 시작
- 부팅 시 자동 시작 등록 (작업 스케줄러)
- Windows 방화벽 포트 개방 (3300, 10080)

설치 완료 후 이 PC의 로컬 IP가 화면에 표시됩니다.

### 3단계 — 공유기 포트포워딩 설정

공유기 관리 페이지 접속 (보통 `192.168.0.1` 또는 `192.168.1.1`)

아래 두 개의 포트포워딩 규칙 추가:

| 이름 | 내부 IP | 외부 포트 | 내부 포트 | 프로토콜 |
|------|---------|----------|----------|---------|
| signagepro-api | 이 PC의 IP | 3300 | 3300 | TCP |
| signagepro-tcp | 이 PC의 IP | 10080 | 10080 | TCP |

> 이 PC의 IP는 설치 완료 화면에서 확인하거나 `ipconfig` 명령어로 확인

### 4단계 — 대시보드 접속 확인

브라우저에서 아래 주소 접속:

- 로컬 접속: `http://이PC의IP:3300`
- 외부 접속: `http://공인IP:3300`

정상 접속 시 SignagePro 대시보드가 표시됩니다.

### 5단계 — 현장 기기 서버 주소 설정

Android 사이니지 기기에서:
1. 화면 **우상단 2번 클릭** → 초기 설정 화면
2. 서버 주소 입력: `http://공인IP:3300`
3. 저장 후 앱 재시작

---

## 설치 후 관리

### PM2 명령어 (서버 PC에서 실행)

```powershell
pm2 status                    # 실행 상태 확인
pm2 logs signagepro-server    # 실시간 로그
pm2 restart signagepro-server # 서버 재시작
pm2 stop signagepro-server    # 서버 중지
```

### 서버 업데이트 시

1. 개발 PC에서 `npm run build` (대시보드 빌드)
2. USB에 최신 파일 담기
3. 서버 PC에서 `setup_from_usb.bat` 재실행 (기존 DB는 보호됨)

---

## 포트 정리

| 포트 | 용도 |
|------|------|
| 3300 | HTTP API + 대시보드 웹 |
| 10080 | Android 기기 TCP 연결 |

---

## 문제 해결

**서버가 실행되지 않을 때**
```powershell
cd C:\signagepro\server
pm2 logs signagepro-server
```

**포트 충돌 시**
```powershell
netstat -ano | findstr "3300"
taskkill /PID [PID번호] /F
pm2 restart signagepro-server
```

**DB 초기화 필요 시** (데이터 삭제 주의)
```powershell
cd C:\signagepro\server
npx prisma db push --accept-data-loss
```
