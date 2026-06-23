import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { readFileSync } from 'fs'

const pkg = JSON.parse(readFileSync('./package.json', 'utf-8'))
const _now = new Date();
const _pad = n => String(n).padStart(2, '0');
const buildDate = `${_now.getFullYear()}-${_pad(_now.getMonth()+1)}-${_pad(_now.getDate())} ${_pad(_now.getHours())}:${_pad(_now.getMinutes())}`;
// 6자리 빌드번호 = major*10000 + minor*100 + patch (STB versionCode와 동일 산식). 예: 1.0.0 → "010000"
const _vp = String(pkg.version).split('.').map(n => parseInt(n) || 0);
const buildCode = String((_vp[0] || 0) * 10000 + (_vp[1] || 0) * 100 + (_vp[2] || 0)).padStart(6, '0');

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: './',
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version),
    __BUILD_CODE__: JSON.stringify(buildCode),
    __BUILD_DATE__: JSON.stringify(buildDate),
  },
  server: {
    host: true,
    port: 3000,
    // dev 서버를 Tailscale 머신명(dev-home 등)·LAN IP로 접속 허용.
    // dev 전용(운영은 node가 dist 서빙)이고 tailnet/LAN 내부라 호스트 체크 완화.
    allowedHosts: ['dev-home', 'dev-office', 'localhost', '.ts.net'],
    proxy: {
      '/api': { target: 'http://localhost:3001', changeOrigin: true },
      '/socket.io': { target: 'http://localhost:3001', changeOrigin: true, ws: true },
      '/uploads': { target: 'http://localhost:3001', changeOrigin: true },
      '/update': { target: 'http://localhost:3001', changeOrigin: true },
    },
    watch: {
      usePolling: true,
      interval: 500,
    },
  },
})
