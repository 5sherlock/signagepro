import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { readFileSync } from 'fs'

const pkg = JSON.parse(readFileSync('./package.json', 'utf-8'))
const _now = new Date();
const _pad = n => String(n).padStart(2, '0');
const buildDate = `${_now.getFullYear()}-${_pad(_now.getMonth()+1)}-${_pad(_now.getDate())} ${_pad(_now.getHours())}:${_pad(_now.getMinutes())}`;

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: './',
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version),
    __BUILD_DATE__: JSON.stringify(buildDate),
  },
  server: {
    host: true,
    port: 3000,
    proxy: {
      '/api': { target: 'http://localhost:3001', changeOrigin: true },
      '/socket.io': { target: 'http://localhost:3001', changeOrigin: true, ws: true },
      '/uploads': { target: 'http://localhost:3001', changeOrigin: true },
    },
    watch: {
      usePolling: true,
      interval: 500,
    },
  },
})
