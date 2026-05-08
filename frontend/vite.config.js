import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue2'

const devApiTarget = process.env.VITE_DEV_API_TARGET || 'http://127.0.0.1:18080'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    headers: {
      'Cache-Control': 'no-store, no-cache, must-revalidate, proxy-revalidate',
      Pragma: 'no-cache',
      Expires: '0'
    },
    proxy: {
      '/api': {
        target: devApiTarget,
        changeOrigin: true
      }
    }
  }
})
