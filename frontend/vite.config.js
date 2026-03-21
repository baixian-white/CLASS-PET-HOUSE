import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // 我顺手帮你把这里兜底的端口改回了3000，这样前端就不会一直报 ECONNREFUSED 了！
  const backendUrl = env.VITE_BACKEND_URL || 'http://localhost:3000'

  return {
    plugins: [vue(), tailwindcss()],
    server: {
      host: true,
      proxy: {
        '/api': backendUrl,
        '/pet-images': backendUrl,
        '/pet-backgrounds': backendUrl,  // 你的本地新增内容
        '/动物图片': backendUrl
      }
    }
  }
})
