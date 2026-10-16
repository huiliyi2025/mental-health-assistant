import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 读取 .env / .env.[mode] 中的环境变量
  const env = loadEnv(mode, process.cwd(), '')
  // 后端地址，默认本机；可在 .env 里用 VITE_API_TARGET 覆盖
  const API_TARGET = env.VITE_API_TARGET || 'http://localhost:1236'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      proxy: {
        '/api': {
          target: API_TARGET,
          changeOrigin: true
        }
      }
    }
  }
})
