# SignagePro 운영 서버 (NAS/Docker) 멀티스테이지 빌드
# 단일 컨테이너가 API + 대시보드(/) + 모바일(/mobile) 정적 서빙을 전부 담당.
# 참고: server/index.js 는 ../dashboard/dist, ../mobile/dist 를 정적 서빙하므로
# 런타임에서 WORKDIR=/app/server, 프론트는 /app/dashboard/dist, /app/mobile/dist 에 위치.

# ---------- Stage 1: 대시보드 빌드 ----------
FROM node:22-bookworm AS dashboard
WORKDIR /build/dashboard
COPY dashboard/package.json dashboard/package-lock.json ./
RUN npm install
COPY dashboard/ ./
RUN npm run build

# ---------- Stage 2: 모바일 빌드 ----------
FROM node:22-bookworm AS mobile
WORKDIR /build/mobile
COPY mobile/package.json mobile/package-lock.json ./
RUN npm install
COPY mobile/ ./
RUN npm run build

# ---------- Stage 3: 서버 의존성(네이티브 빌드 + prisma generate) ----------
# full bookworm = node-gyp용 빌드툴/python 포함 → sqlite3 등 네이티브 모듈 컴파일 가능.
# prisma(CLI)는 devDependency 이고 런타임 CMD가 migrate deploy 를 쓰므로 full 설치 필요.
FROM node:22-bookworm AS serverdeps
WORKDIR /build/server
COPY server/package.json server/package-lock.json ./
RUN npm install
COPY server/prisma ./prisma
RUN npx prisma generate

# ---------- Stage 4: 런타임 ----------
FROM node:22-bookworm-slim AS runtime
ENV NODE_ENV=production
# ffmpeg/ffprobe 를 PATH 에 넣어 FFMPEG_PATH/FFPROBE_PATH env 불필요.
# openssl/ca-certificates 는 prisma 엔진(debian-openssl-3.0.x) 동작에 필요.
RUN apt-get update && apt-get install -y --no-install-recommends \
        ffmpeg openssl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app/server
# 1) 서버 소스 (node_modules/.env/*.db/uploads 는 .dockerignore 로 제외됨)
COPY server/ ./
# 2) 빌드 스테이지에서 만든 node_modules(네이티브 sqlite3 + @prisma/client + prisma CLI + .prisma 엔진)
COPY --from=serverdeps /build/server/node_modules ./node_modules
# 3) 빌드된 프론트엔드 정적 산출물
COPY --from=dashboard /build/dashboard/dist /app/dashboard/dist
COPY --from=mobile    /build/mobile/dist    /app/mobile/dist

# 영속 데이터(아래 볼륨으로 마운트):
#   /data                 → sqlite DB (DATABASE_URL=file:/data/prod.db)
#   /app/server/uploads   → 업로드 미디어 (정적 서빙 경로와 동일, UPLOADS_DIR 설정 금지)
EXPOSE 3300 10080

# 기동 시 스키마 동기화(db push) 후 서버 시작.
# ※ migrations 폴더가 init 하나뿐이고 이후 변경은 dev에서 db push로 반영돼 와서,
#   migrate deploy로는 ScreenSchedule 등 신규 테이블이 안 생김 → db push 사용.
CMD ["sh", "-c", "npx prisma db push --skip-generate && node index.js"]
