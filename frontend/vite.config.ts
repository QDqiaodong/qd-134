import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 8195,
    proxy: {
      '/api': {
        target: 'http://localhost:8184',
        changeOrigin: true
      }
    }
  }
})
