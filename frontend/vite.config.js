import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 子路径部署时通过 VITE_BASE 指定，如 /mem/；本地开发默认根路径
  base: process.env.VITE_BASE || '/',
  plugins: [vue()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      // 开发环境：后端 Spring Boot 跑在 8082
      '/api': 'http://127.0.0.1:8082',
      '/uploads': 'http://127.0.0.1:8082',
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          three: ['three'],
        },
      },
    },
  },
})
