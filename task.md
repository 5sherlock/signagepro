# SignagePro 개발 작업 목록

> 최종 업데이트: 2026-05-10
> 개발 전략: 2트랙 순차 진행 (Track A → Track B)

---

## ✅ 완료된 공통 기반 작업

- [x] **0단계: 개발 환경 구성** *(2026-05-10 완료)*
  - [x] clauderoadmap.md 분석 및 프로젝트 구조 파악
  - [x] Node.js v24.12.0 / npm v11.6.2 확인
  - [x] `server` 의존성 설치 확인 (187 packages)
  - [x] `dashboard` 의존성 설치 확인 (417 packages)
  - [x] Prisma Client 재생성 (`npx prisma generate`)
  - [x] SQLite DB 스키마 동기화 확인 (`npx prisma db push`)
  - [x] 루트 `package.json` 생성 — `npm run dev` 한 번에 서버+대시보드 동시 실행
  - [x] 백엔드 서버 실행 확인 (Port 3000 HTTP, Port 10080 TCP)
  - [x] 대시보드 Dev Server 실행 확인 (http://localhost:5173)

- [x] **1단계: 백엔드 서버 구축 (Node.js + Express + SQLite)**
  - [x] `server` 폴더에 Node.js 프로젝트 초기화
  - [x] 필수 패키지 설치 (Express, CORS, Prisma 등)
  - [x] Prisma SQLite DB 모델링 (Store, Group, Device, Playlist 모델 생성)
  - [x] 10080 포트 TCP 소켓 서버 구축 (셋톱박스 하트비트 수신)
  - [x] 그룹 단위 미디어 배포 및 제어 REST API 구현
  - [x] Socket.io WebSocket 실시간 통신 구현

- [x] **2단계: 관리자 대시보드 구축 (Vite + React)**
  - [x] `dashboard` 폴더에 Vite React 프로젝트 생성
  - [x] 기기 실시간 관제 대시보드 (온/오프라인, CPU/메모리 모니터링)
  - [x] 드래그 앤 드롭 기반 기기 그룹(Group) 관리 기능
  - [x] 그룹 단위 콘텐츠 스케줄링 및 배포 UI 구현

- [x] **2.5단계: 미디어 스케줄링 UI 고도화 (Swimlane & 기기 타겟팅)**
  - [x] 단일 리스트에서 개별 기기별 수평 타임라인(Swimlane) UI로 전면 개편
  - [x] 타임라인 내 아이템 전환 효과(디졸브, 밀기 등) 및 개별 재생 시간 설정 기능 추가
  - [x] 기기 간 재생 시간 동기화(Sync) 토글 기능 추가
  - [x] 스마트 상태 비교를 통한 '배포 대기' 상태 자동 해제 로직 구현

---

## 🅐 Track A — 설치파일(Electron) + 온프레미스 서버

> **목표**: 현장 관리자 PC에 `.exe` 설치 → 로컬 서버와 연동
> **사용자**: 오설록 현장 담당자 (인터넷 불필요, 사내 LAN 운영)

### A-1. 백엔드 고도화 및 Android 클라이언트 연동
- [ ] 보드용 클라이언트 아키텍처 확립 (Android → TCP/WebSocket 통신 프로토콜 정의)
- [ ] 소켓 기반 하트비트 전송 및 그룹화/동기화 명령 수신 로직
- [ ] 로컬 캐싱 기능이 적용된 영상/이미지 미디어 플레이어 구현
  - [ ] 기기별 필터링 재생 및 트랜지션 적용
  - [ ] NTP 기반 재생 타이밍 동기화 (5개 보드 싱크)
- [ ] 와이파이 재접속 자동화 로직 (네트워크 불안정 대응)

### A-2. Electron 데스크탑 앱 패키징
- [ ] `electron/main.cjs` 온프레미스 모드 설정 (서버 URL = localhost)
- [ ] Electron에서 Node.js 서버 자동 내장 실행 (앱 시작 시 서버 동시 기동)
- [ ] Windows `.exe` 인스톨러 빌드 (`npm run electron:build`)
- [ ] 설치 후 자동 시작(시작프로그램 등록) 옵션 추가
- [ ] 오프라인 환경 테스트 (인터넷 없이 LAN만으로 완전 동작 확인)

### A-3. 현장 배포 및 테스트
- [ ] 오설록 현장 PC에 `.exe` 설치 테스트
- [ ] 셋톱박스 5대 연결 및 멀티 싱크 검증
- [ ] 유선 LAN (고정 IP) 환경에서 안정성 테스트
- [ ] 운영 매뉴얼 작성

---

## 🅑 Track B — 웹 대시보드 + 클라우드 서버

> **목표**: 어디서나 브라우저로 접속 → 클라우드 서버 연동
> **사용자**: 본사/원격지 관리자 (인터넷 필요, 다중 매장 확장 가능)
> **선행 조건**: Track A 안정화 완료 후 진행

### B-1. 클라우드 서버 구축
- [ ] 클라우드 플랫폼 선택 (AWS / GCP / Railway 등)
- [ ] SQLite → PostgreSQL 마이그레이션 (Prisma schema 수정)
- [ ] 환경변수 및 `.env` 클라우드 배포용 분리
- [ ] HTTPS 설정 및 도메인 연결
- [ ] 파일 업로드 스토리지 → S3 또는 클라우드 스토리지 연동

### B-2. 웹 대시보드 배포
- [ ] Electron 의존성 제거 → 순수 웹앱으로 분리 (`dashboard-web` 폴더)
- [ ] Vite 프로덕션 빌드 (`npm run build`)
- [ ] 정적 파일 호스팅 (Vercel / Netlify / S3 Static Hosting)
- [ ] 인증/로그인 기능 추가 (관리자 계정 보안)
- [ ] 반응형 UI 최적화 (태블릿/모바일 대응)

### B-3. 운영 안정화 및 AI 연동
- [ ] 다중 매장 관리 기능 (현재 오설록 외 확장)
- [ ] antigravity Gemini 연동 — 콘텐츠 자동 추천
- [ ] 스케줄 최적화 자동화
- [ ] 데이터 분석 및 리포트 기능
- [ ] 장애 알림 (이메일 / Slack / 카카오 알림)
