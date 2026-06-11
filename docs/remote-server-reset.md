# 원격 서버 Git 히스토리 재작성 후 처리 절차

## 배경

로컬에서 `git-filter-repo`로 `dashboard/dist_electron/` 폴더를 히스토리에서 완전 삭제했습니다.
모든 커밋 해시가 변경됐기 때문에 원격 서버의 로컬 클론이 GitHub과 맞지 않는 상태가 됩니다.

## 원격 서버에서 해야 할 작업

### 1. 현재 작업 중인 변경사항 백업 (있는 경우)

```bash
git stash
# 또는 중요한 파일이라면
cp 파일명 파일명.bak
```

### 2. GitHub에서 최신 히스토리 가져오기

```bash
git fetch --all
```

### 3. main 브랜치로 강제 리셋

```bash
git checkout main
git reset --hard origin/main
```

### 4. 서버 재시작

```bash
pm2 restart all
```

### 5. 정상 동작 확인

```bash
pm2 status
git log --oneline -5
```

---

## 주의사항

- `git pull` 만으로는 안 됩니다 — 히스토리가 달라서 conflict 발생
- 반드시 `git reset --hard origin/main` 으로 강제 맞춰야 합니다
- 원격 서버에서 직접 커밋한 내용이 있다면 미리 백업 필수

## 이후 정상 배포 흐름

```
로컬 dev 작업 완료
  → git push origin dev
  → D:\signagepro 에서 git pull (staging 테스트)
  → git push origin main
  → 원격 서버: git pull  (이제 일반 pull로 됨)
  → pm2 restart all
```
