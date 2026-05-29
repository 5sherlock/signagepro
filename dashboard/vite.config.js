import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { readFileSync } from 'fs'

const pkg = JSON.parse(readFileSync('./package.json', 'utf-8'))
const buildDate = new Date().toISOString().slice(0, 16).replace('T', ' ')  // "2026-05-27 14:30"

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: './',
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version),
    __BUILD_DATE__: JSON.stringify(buildDate),
  },
  server: {
    host: true, // 0.0.0.0 바인딩 → 외부 IP로 접근 가능
    watch: {
      // Windows에서 외부 편집기로 수정 시 파일 변경 감지 안 되는 문제 해결
      usePolling: true,
      interval: 500,
    },
  },
})
